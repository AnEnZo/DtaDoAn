package com.example.DtaAssigement.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface CloudinaryService {

    String uploadImage(MultipartFile file);

    Map<String, Object> uploadImageWithDetails(MultipartFile file);

    boolean deleteImage(String publicId);

    Map<String, String> generateUploadSignature(String folder);
}
