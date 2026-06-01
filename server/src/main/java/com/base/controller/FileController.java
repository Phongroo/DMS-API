package com.base.controller;

import com.base.model.DmsFile;
import com.base.repo.DmsFileRepository;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/file")
public class FileController {
    @Autowired
    private DmsFileRepository dmsFileRepository;
    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucket;

    @GetMapping("/view/{id}")
    public ResponseEntity<byte[]> viewFile(
            @PathVariable Long id
    ) throws Exception {
        DmsFile file = dmsFileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found"));

        InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(file.getObjectKey())
                        .build()
        );

        byte[] data = stream.readAllBytes();

        MediaType mediaType;

        if (file.getContentType() != null) {
            mediaType = MediaType.parseMediaType(file.getContentType());
        } else {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(data);
    }
}