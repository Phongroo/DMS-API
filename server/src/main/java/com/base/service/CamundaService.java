package com.base.service;

import com.base.model.Req.ProcessTaskReq;
import com.base.model.Req.StartJobReq;
import com.base.model.Res.BaseResponse;
import org.springframework.web.multipart.MultipartFile;

public interface CamundaService {
    BaseResponse startProcess(MultipartFile file);
    BaseResponse processTask(ProcessTaskReq req);
    BaseResponse viewTask(String processInstanceId);
}
