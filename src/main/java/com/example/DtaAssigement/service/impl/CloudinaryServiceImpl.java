package com.example.DtaAssigement.service.impl;

import com.cloudinary.Cloudinary;
import com.example.DtaAssigement.config.CloudinaryProperties;
import com.example.DtaAssigement.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;
    private final CloudinaryProperties cloudinaryProperties;

    private static final String[] ALLOWED_FORMATS = {"jpg", "jpeg", "png", "gif", "webp"};
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    @Override
    public String uploadImage(MultipartFile file) {
        Map<String, Object> result = uploadImageWithDetails(file);
        return (String) result.get("url");
    }

    @Override
    public Map<String, Object> uploadImageWithDetails(MultipartFile file) {
        validateFile(file);

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("folder", "DtaAssigment"); // Folder name in Cloudinary
            params.put("resource_type", "image");
            params.put("allowed_formats", ALLOWED_FORMATS);

            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), params);

            Map<String, Object> response = new HashMap<>();
            response.put("url", uploadResult.get("secure_url"));
            response.put("publicId", uploadResult.get("public_id"));
            response.put("format", uploadResult.get("format"));
            response.put("width", uploadResult.get("width"));
            response.put("height", uploadResult.get("height"));
            response.put("bytes", uploadResult.get("bytes"));

            log.info("Image uploaded successfully to Cloudinary: {}", uploadResult.get("secure_url"));

            return response;
        } catch (IOException e) {
            log.error("Failed to upload image to Cloudinary", e);
            throw new RuntimeException("Failed to upload image: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, String> generateUploadSignature(String folder) {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String folderParam = (folder == null || folder.isBlank()) ? "DtaAssigment" : folder;

        String params = "folder=" + folderParam + "&timestamp=" + timestamp;

        String signature;
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    cloudinaryProperties.getApiSecret().getBytes("UTF-8"), "HmacSHA1");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(params.getBytes("UTF-8"));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            signature = hexString.toString();
        } catch (Exception e) {
            log.error("Failed to generate Cloudinary signature", e);
            throw new RuntimeException("Failed to generate signature: " + e.getMessage(), e);
        }

        Map<String, String> result = new HashMap<>();
        result.put("signature", signature);
        result.put("timestamp", timestamp);
        result.put("apiKey", cloudinaryProperties.getApiKey());
        result.put("cloudName", cloudinaryProperties.getCloudName());
        result.put("folder", folderParam);

        log.info("Generated upload signature for folder: {}", folderParam);
        return result;
    }

    @Override
    public boolean deleteImage(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            log.warn("Attempted to delete image with null or empty publicId");
            return false;
        }

        try {
            Map<String, Object> result = cloudinary.uploader().destroy(publicId, new HashMap<>());
            boolean success = "ok".equals(result.get("result"));
            if (success) {
                log.info("Image deleted successfully from Cloudinary: {}", publicId);
            } else {
                log.warn("Image deletion returned non-ok result: {}", result.get("result"));
            }
            return success;
        } catch (IOException e) {
            log.error("Failed to delete image from Cloudinary: {}", publicId, e);
            return false;
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty or null");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size of 10MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed");
        }
    }
}
