package com.example.DtaAssigement.controller;

import com.example.DtaAssigement.service.CloudinaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Image Upload", description = "Image upload and management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class ImageUploadController {

    private final CloudinaryService cloudinaryService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','USER','STAFF')")
    @Operation(summary = "Upload an image", description = "Upload an image file to Cloudinary and return the URL")
    public ResponseEntity<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file) {
        log.info("Received image upload request: {}", file.getOriginalFilename());

        try {
            Map<String, Object> result = cloudinaryService.uploadImageWithDetails(file);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Image uploaded successfully");
            response.put("data", result);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("Validation error during image upload: {}", e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            log.error("Error during image upload", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to upload image: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/signature")
    @PreAuthorize("hasAnyRole('ADMIN','USER','STAFF')")
    @Operation(summary = "Generate upload signature", description = "Generate a Cloudinary signature for direct browser upload")
    public ResponseEntity<Map<String, Object>> generateSignature(@RequestBody(required = false) Map<String, String> body) {
        String folder = (body != null && body.containsKey("folder")) ? body.get("folder") : "DtaAssigment";
        log.info("Generating upload signature for folder: {}", folder);

        try {
            Map<String, String> signatureData = cloudinaryService.generateUploadSignature(folder);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", signatureData);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error generating signature", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to generate signature: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @DeleteMapping("/{publicId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER','STAFF')")
    @Operation(summary = "Delete an image", description = "Delete an image from Cloudinary by its public ID")
    public ResponseEntity<Map<String, Object>> deleteImage(@PathVariable String publicId) {
        log.info("Received image deletion request for publicId: {}", publicId);

        try {
            boolean deleted = cloudinaryService.deleteImage(publicId);

            Map<String, Object> response = new HashMap<>();
            if (deleted) {
                response.put("success", true);
                response.put("message", "Image deleted successfully");
            } else {
                response.put("success", false);
                response.put("message", "Failed to delete image");
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error during image deletion", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to delete image: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
