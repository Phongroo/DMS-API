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
    @Override
    public BaseResponse save(DmsFileReq req, MultipartFile file) {
        try {


            String uploadDir = "server/uploads/";

            File dir = new File(uploadDir);

            if (!dir.exists()) {
                dir.mkdirs();
            }

            String fileName =
                    System.currentTimeMillis()
                    + "_"
                    + file.getOriginalFilename();

            String filePath =
                    uploadDir + fileName;

            Files.copy(
                    file.getInputStream(),
                    Paths.get(filePath),
                    StandardCopyOption.REPLACE_EXISTING
            );

            DmsFile dmsFile = new DmsFile();

            dmsFile.setFileName(
                    file.getOriginalFilename()
            );

            dmsFile.setFilePath(filePath);

            dmsFile.setContentType(
                    file.getContentType()
            );

            dmsFile.setFileSize(
                    file.getSize()
            );

            dmsFile.setUploadedDate(
                    new Date()
            );

            dmsFile.setDoc(req.getDmsDoc());

            repository.save(dmsFile);

            return new BaseResponse(
                    200,
                    dmsFile,
                    "Upload success"
            );

        } catch (Exception e) {

            return new BaseResponse(
                    500,
                    null,
                    e.getMessage()
            );
        }
    }
}
