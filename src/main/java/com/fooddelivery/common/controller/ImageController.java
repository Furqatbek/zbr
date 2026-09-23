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

    @PostMapping(value = "/upload/{bucket}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'PLATFORM', 'RESTAURANT_OWNER', 'RESTAURANT_STAFF')")
    @Operation(summary = "Upload an image",
            description = "Buckets: restaurants, menu-items, categories, profiles, documents. "
                    + "Anything else is a 400 naming the accepted list.")
    public ResponseEntity<ApiResponse<ImageInfo>> uploadImage(
            @PathVariable String bucket,
            @RequestParam("file") MultipartFile file) {

        log.info("Uploading image to bucket: {}, filename: {}, size: {}",
                bucket, file.getOriginalFilename(), file.getSize());

        ImageInfo imageInfo = imageStorageService.storeImage(file, bucket);

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

    // {*path} captures EVERY remaining segment. The old mapping was
    // /{category}/{filename:.+}, which is exactly two segments: menu items
    // ("menu-items/<uuid>.png") fitted and were served, while restaurant images
    // ("restaurants/7/logo/<uuid>.png") are four and simply did not match the
    // mapping — the upload succeeded, the URL was stored on the restaurant, and
    // every request for it 404'd. A regex path variable cannot span "/" under
    // Spring's PathPatternParser, so widening the regex would not have helped.
    //
    // Containment is unaffected: loadImage refuses anything that resolves
    // outside the storage root, which is where traversal has always been
    // stopped — the mapping was never what made this safe.
    @GetMapping("/{*path}")
    @Operation(summary = "Get image", description = "Retrieve an image by its stored relative path")
    public ResponseEntity<Resource> getImage(@PathVariable String path) {

        // {*path} yields the captured remainder WITH its leading slash, which
        // would resolve as an absolute path against the root.
        String relativePath = path.startsWith("/") ? path.substring(1) : path;

        Resource resource = imageStorageService.loadImage(relativePath);

        String contentType = determineContentType(relativePath);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "max-age=31536000, public")
                .body(resource);
    }

    // ADMIN/PLATFORM only. Images carry no ownership record, so any owner who
    // could reach this endpoint could delete ANY image — and image URLs are
    // public in menu responses, so a competitor's filenames are simply readable.
    // Vendors never need it: MenuService deletes the old file itself when a
    // menu-item image is replaced or the item removed, behind the ownership
    // check that endpoint already enforces.
    @DeleteMapping("/{*path}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLATFORM')")
    @Operation(summary = "Delete image", description = "Delete an image by its stored relative path")
    public ResponseEntity<ApiResponse<Void>> deleteImage(@PathVariable String path) {

        // Same two-segment limit as the GET above, so restaurant images could
        // not be deleted either.
        String relativePath = path.startsWith("/") ? path.substring(1) : path;
        boolean deleted = imageStorageService.deleteImage(relativePath);

        if (deleted) {
            return ResponseEntity.ok(ApiResponse.<Void>success("Image deleted successfully"));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    private String determineContentType(String relativePath) {
        // The extension has to come from the LAST segment: the path is now
        // nested, and a directory name containing a dot would otherwise decide
        // the content type.
        String filename = relativePath.substring(relativePath.lastIndexOf('/') + 1);
        int dot = filename.lastIndexOf('.');
        if (dot < 0) {
            return "image/jpeg";
        }
        String extension = filename.substring(dot + 1).toLowerCase();
        return switch (extension) {
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            default -> "image/jpeg";
        };
    }
}
