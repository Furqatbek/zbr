package com.fooddelivery.common.service;

import com.fooddelivery.common.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Service for handling image file storage operations.
 */
@Service
@Slf4j
public class ImageStorageService {

    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    );

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp"
    );

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    @Value("${app.storage.images.path:/app/images}")
    private String storagePath;

    @Value("${app.storage.images.base-url:http://localhost:8080/api/v1/images}")
    private String baseUrl;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        this.rootLocation = Paths.get(storagePath);
        try {
            Files.createDirectories(rootLocation);
            log.info("Image storage initialized at: {}", rootLocation.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize image storage location", e);
        }
    }

    /**
     * Store an image file and return the stored file information.
     */
    public ImageInfo storeImage(MultipartFile file, String category) {
        validateFile(file);

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = getFileExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID().toString() + "." + extension;

        // Category reaches here straight from a @PathVariable on /images/upload
        // and is used as a directory name. Validate BEFORE creating anything:
        // the traversal check used to run after createDirectories, so a crafted
        // category could still mkdir outside the store even though the write
        // was then refused.
        Path categoryPath = resolveWithinRoot(category, "category");
        try {
            Files.createDirectories(categoryPath);
        } catch (IOException e) {
            throw new BusinessException("Could not create category directory: " + category);
        }

        Path destinationFile = categoryPath.resolve(uniqueFilename).normalize().toAbsolutePath();

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored image: {} -> {}", originalFilename, destinationFile);
        } catch (IOException e) {
            throw new BusinessException("Failed to store image: " + e.getMessage());
        }

        String relativePath = category + "/" + uniqueFilename;
        String imageUrl = baseUrl + "/" + relativePath;

        return ImageInfo.builder()
                .originalName(originalFilename)
                .storedName(uniqueFilename)
                .path(destinationFile.toString())
                .relativePath(relativePath)
                .url(imageUrl)
                .size(file.getSize())
                .contentType(file.getContentType())
                .build();
    }

    /**
     * Load an image as a Resource.
     */
    public Resource loadImage(String relativePath) {
        try {
            // Containment check, as in deleteImage. Without it this method will
            // read and serve ANY file the JVM can open — the path arrives from
            // a {filename:.+} path variable, which matches slashes.
            Path file = resolveWithinRoot(relativePath, "image path");
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new BusinessException("Image not found: " + relativePath);
            }
        } catch (MalformedURLException e) {
            throw new BusinessException("Invalid image path: " + relativePath);
        }
    }

    /**
     * Delete an image file.
     */
    public boolean deleteImage(String relativePath) {
        try {
            Path file = resolveWithinRoot(relativePath, "image path");

            boolean deleted = Files.deleteIfExists(file);
            if (deleted) {
                log.info("Deleted image: {}", relativePath);
            }
            return deleted;
        } catch (IOException e) {
            log.error("Failed to delete image: {}", relativePath, e);
            return false;
        }
    }

    /**
     * Resolve a caller-supplied relative path against the image root and refuse
     * anything that escapes it.
     *
     * <p>The single place path containment is enforced. Comparing the resolved,
     * normalised, absolute path against the equally absolute root is what makes
     * this reliable: {@code rootLocation} is built from a configured string and
     * is not necessarily absolute, and {@code startsWith} on paths of differing
     * absoluteness silently returns false — or, worse, compares the wrong
     * prefix.
     */
    private Path resolveWithinRoot(String relativePath, String what) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new BusinessException("Invalid " + what);
        }
        Path root = rootLocation.toAbsolutePath().normalize();
        Path resolved = root.resolve(relativePath).normalize().toAbsolutePath();
        if (!resolved.startsWith(root)) {
            log.warn("SECURITY: rejected {} escaping the image store: {}", what, relativePath);
            throw new BusinessException("Invalid " + what);
        }
        return resolved;
    }

    /**
     * Validate the uploaded file.
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("File size exceeds maximum allowed size of 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException("Invalid file type. Allowed types: JPEG, PNG, GIF, WebP");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.contains("..")) {
            throw new BusinessException("Invalid filename");
        }

        String extension = getFileExtension(filename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("Invalid file extension. Allowed: jpg, jpeg, png, gif, webp");
        }
    }

    /**
     * Get file extension from filename.
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    /**
     * Image information holder.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ImageInfo {
        private String originalName;
        private String storedName;
        private String path;
        private String relativePath;
        private String url;
        private Long size;
        private String contentType;
    }
}
