package com.base.service;

import com.base.model.Req.DmsFileReq;
import com.base.model.Req.WorkflowContext;
import com.base.model.Res.BaseResponse;
import org.springframework.web.multipart.MultipartFile;

public interface DmsFileService {
    void save(DmsFileReq req, MultipartFile file);
}
