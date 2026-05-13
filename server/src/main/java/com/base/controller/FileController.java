package com.base.controller;

import com.base.model.DmsFile;
import com.base.repo.DmsFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    @GetMapping("/view/{id}")
    public ResponseEntity<byte[]> viewFile(
            @PathVariable Long id
    ) throws Exception {
            DmsFile file = dmsFileRepository.getById(id);


            Path path =
                    Paths.get(file.getFilePath());

            byte[] data =
                    Files.readAllBytes(path);

            return ResponseEntity.ok()
                    .contentType(
                            MediaType.APPLICATION_PDF
                    )
                    .body(data);

    }
}