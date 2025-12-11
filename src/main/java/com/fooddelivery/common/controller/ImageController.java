package com.fooddelivery.common.controller;

import com.fooddelivery.common.dto.ApiResponse;
import com.fooddelivery.common.service.ImageStorageService;
import com.fooddelivery.common.service.ImageStorageService.ImageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller for image upload and retrieval operations.
 */
@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Images", description = "Image upload and retrieval API")
public class ImageController {

    private final ImageStorageService imageStorageService;

    @PostMapping(value = "/upload/{category}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'PLATFORM', 'RESTAURANT_OWNER', 'RESTAURANT_STAFF')")
    @Operation(summary = "Upload an image", description = "Upload an image file to the specified category")
    public ResponseEntity<ApiResponse<ImageInfo>> uploadImage(
            @PathVariable String category,
            @RequestParam("file") MultipartFile file) {

        log.info("Uploading image to category: {}, filename: {}, size: {}",
                category, file.getOriginalFilename(), file.getSize());

        ImageInfo imageInfo = imageStorageService.storeImage(file, category);

        return ResponseEntity.ok(ApiResponse.success("Image uploaded successfully", imageInfo));
    }

    @PostMapping(value = "/menu-items/{menuItemId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'PLATFORM', 'RESTAURANT_OWNER', 'RESTAURANT_STAFF')")
    @Operation(summary = "Upload menu item image", description = "Upload an image for a specific menu item")
    public ResponseEntity<ApiResponse<ImageInfo>> uploadMenuItemImage(
            @PathVariable Long menuItemId,
            @RequestParam("file") MultipartFile file) {

        log.info("Uploading image for menu item: {}, filename: {}", menuItemId, file.getOriginalFilename());

        ImageInfo imageInfo = imageStorageService.storeImage(file, "menu-items");

        return ResponseEntity.ok(ApiResponse.success("Menu item image uploaded successfully", imageInfo));
    }

    @GetMapping("/{category}/{filename:.+}")
    @Operation(summary = "Get image", description = "Retrieve an image by category and filename")
    public ResponseEntity<Resource> getImage(
            @PathVariable String category,
            @PathVariable String filename) {

        String relativePath = category + "/" + filename;
        Resource resource = imageStorageService.loadImage(relativePath);

        String contentType = determineContentType(filename);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "max-age=31536000, public")
                .body(resource);
    }

    @DeleteMapping("/{category}/{filename:.+}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLATFORM', 'RESTAURANT_OWNER', 'RESTAURANT_STAFF')")
    @Operation(summary = "Delete image", description = "Delete an image by category and filename")
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @PathVariable String category,
            @PathVariable String filename) {

        String relativePath = category + "/" + filename;
        boolean deleted = imageStorageService.deleteImage(relativePath);

        if (deleted) {
            return ResponseEntity.ok(ApiResponse.<Void>success("Image deleted successfully"));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    private String determineContentType(String filename) {
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        return switch (extension) {
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            default -> "image/jpeg";
        };
    }
}
