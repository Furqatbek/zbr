package com.fooddelivery.common.service;

import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.common.exception.ResourceNotFoundException;
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

    /**
     * The drawers an image may be filed in.
     *
     * <p>This list is new. The bucket arrived from a {@code @PathVariable} and
     * was used as a directory name with nothing but a traversal check, so
     * <em>any</em> name was accepted and created a drawer — which meant a typo
     * ({@code catagories}) silently produced a second one that no audit would
     * ever look in, and "what is in the restaurants bucket?" had no stable
     * answer. Clients reasonably assumed an enumeration existed, because the
     * endpoint reads like it should have one.
     *
     * <p>{@code documents} was briefly on this list and is not, because it was
     * dead at both ends: nothing here writes it, and the admin panel confirmed
     * nothing there does either. Note {@code Courier.documentsSubmitted}, which
     * is the feature it was presumably meant for — that flag is never set by
     * any code path, so the bucket would have been a drawer for a feature that
     * does not exist yet. Re-adding the string is the whole of the work when it
     * does. Removal does not affect reads: anything already stored under it is
     * still served.
     *
     * <p>Only the first segment is checked. Callers compose deeper paths —
     * {@code restaurants/7/logo}, {@code profiles/42} — and those stay free,
     * because the bucket is what the policy hangs off.
     */
    private static final List<String> ALLOWED_BUCKETS = List.of(
            "restaurants", "menu-items", "categories", "profiles");

    /**
     * Shown verbatim to the vendor when the server, not their file, is at
     * fault. Deliberately says nothing about paths or permissions: it is not
     * their problem to act on, and they are not the audience for it.
     */
    private static final String UPLOAD_UNAVAILABLE =
            "Загрузка изображений временно недоступна. Попробуйте позже.";

    @Value("${app.storage.images.path:/app/images}")
    private String storagePath;

    @Value("${app.storage.images.base-url:http://localhost:8080/api/v1/images}")
    private String baseUrl;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        // toAbsolutePath() BEFORE anything else: a relative path would resolve
        // against the process working directory, which is not the same when
        // running from an IDE, from a service unit and from the container.
        this.rootLocation = Paths.get(storagePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            logUnwritable(e);
            return;
        }

        // createDirectories on a directory that ALREADY exists is a no-op that
        // needs no write permission — which is exactly the case when a volume
        // is mounted over the path. Startup therefore looked healthy for weeks
        // while every upload failed. Probe with a real write instead.
        //
        // Only a diagnostic: uploads are not gated on the result, so ownership
        // repaired without a restart takes effect immediately.
        if (probeWrite()) {
            log.info("Image storage initialized at: {}", rootLocation);
        }
    }

    private boolean probeWrite() {
        Path probe = rootLocation.resolve(".write-probe-" + UUID.randomUUID());
        try {
            Files.createFile(probe);
            return true;
        } catch (IOException e) {
            logUnwritable(e);
            return false;
        } finally {
            try {
                Files.deleteIfExists(probe);
            } catch (IOException ignored) {
                // A leftover probe file is harmless; it is not served, because
                // it has no image extension and lives outside every category.
            }
        }
    }

    private void logUnwritable(IOException cause) {
        log.error("IMAGE STORAGE IS NOT WRITABLE: {} ({}). Every image upload will fail until this "
                        + "is fixed. The mount at this path is most likely owned by root while the "
                        + "application runs as a non-root user — see scripts/deploy.sh "
                        + "(fix_image_volume_ownership).",
                rootLocation, cause.toString());
    }

    /**
     * Store an image file and return the stored file information.
     */
    public ImageInfo storeImage(MultipartFile file, String category) {
        // Before the file, because an upload aimed at a drawer that does not
        // exist is wrong whatever it carries, and that is the message the
        // caller can act on.
        category = validateBucket(category);
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
            // The clients show `message` verbatim to a restaurant owner, so the
            // filesystem detail goes to the log — it means nothing to them, and
            // it leaks server paths. The cause was previously dropped entirely,
            // which is why this took a vendor bug report to find.
            log.error("Could not create image category directory {} — check ownership of the "
                    + "storage root {}", categoryPath, rootLocation, e);
            throw new BusinessException(UPLOAD_UNAVAILABLE);
        }

        Path destinationFile = categoryPath.resolve(uniqueFilename).normalize().toAbsolutePath();

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored image: {} -> {}", originalFilename, destinationFile);
        } catch (IOException e) {
            log.error("Failed to write image to {}", destinationFile, e);
            throw new BusinessException(UPLOAD_UNAVAILABLE);
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
            }
            // 404, not 400: the URL is well formed, the file is simply not
            // there. It also makes a missing image greppable and tells it apart
            // from a rejected path — an <Image> tag renders nothing either way,
            // so the log is the only place this is ever visible.
            log.warn("Image not found on disk: {} (resolved to {})", relativePath, file);
            throw new ResourceNotFoundException("Image not found: " + relativePath);
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
     * Check the bucket and return the path with it canonicalised.
     *
     * <p>Matching ignores case, and the stored path uses the canonical spelling:
     * the filesystem is case-sensitive, so {@code Restaurants} would otherwise
     * be a twin of {@code restaurants} — the exact split this list exists to
     * prevent. Everything after the first segment is left exactly as the caller
     * composed it.
     */
    private String validateBucket(String category) {
        if (category == null || category.isBlank()) {
            throw new BusinessException("An image bucket is required. Accepted: "
                    + String.join(", ", ALLOWED_BUCKETS));
        }

        String[] segments = category.split("/", 2);
        String bucket = ALLOWED_BUCKETS.stream()
                .filter(allowed -> allowed.equalsIgnoreCase(segments[0]))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Unknown image bucket '" + segments[0]
                        + "'. Accepted: " + String.join(", ", ALLOWED_BUCKETS)));

        return segments.length == 1 ? bucket : bucket + "/" + segments[1];
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
