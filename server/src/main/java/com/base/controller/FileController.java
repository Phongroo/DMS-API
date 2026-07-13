package com.base.controller;

import com.base.model.DmsFile;
import com.base.repo.DmsFileRepository;
import com.base.service.DmsDocService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;

@RestController
@RequestMapping("/file")
public class FileController {
    @Autowired
    private DmsFileRepository dmsFileRepository;
    @Autowired
    private MinioClient minioClient;
    @Autowired
    private DmsDocService dmsDocService;

    @Value("${minio.bucket-name}")
    private String bucket;

    @GetMapping("/view/{id}")
    public ResponseEntity<byte[]> viewFile(
            @PathVariable Long id
    ) throws Exception {
        DmsFile file = dmsFileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found"));

        // Check fine-grained access control on document level
        if (file.getDoc() != null && !dmsDocService.hasAccess(file.getDoc())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }

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