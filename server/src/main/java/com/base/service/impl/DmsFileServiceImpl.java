package com.base.service.impl;

import com.base.model.DmsFile;
import com.base.model.Req.DmsFileReq;
import com.base.model.Res.BaseResponse;
import com.base.repo.DmsFileRepository;
import com.base.service.DmsFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;

@Service
public class DmsFileServiceImpl implements DmsFileService {
    @Autowired
    private DmsFileRepository repository;

    @Autowired
    private MinioService minioService;

    @Override
    public void save(DmsFileReq req, MultipartFile file) {
        try {

            // 1. Upload file lên MinIO
            String objectKey = minioService.upload(file);

            // 2. Lưu metadata DB
            DmsFile dmsFile = new DmsFile();

            dmsFile.setFileName(file.getOriginalFilename());
            dmsFile.setObjectKey(objectKey);
            dmsFile.setContentType(file.getContentType());
            dmsFile.setFileSize(file.getSize());
            dmsFile.setUploadedDate(new Date());
            dmsFile.setDoc(req.getDmsDoc());

            repository.save(dmsFile);
        } catch (Exception e) {
         throw new RuntimeException("Lỗi khi lưu file: " + e.getMessage(), e);
        }
    }
}