package com.base.service;

import com.base.model.Req.ProcessTaskReq;
import com.base.model.Req.StartJobReq;
import com.base.model.Res.BaseResponse;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

public interface CamundaService {
    BaseResponse startProcess(MultipartFile file, String docType, String email);
    BaseResponse startProcessFromTemplate(String docType, Map<String, Object> templateData, String email);
    BaseResponse processTask(ProcessTaskReq req);
    BaseResponse viewTask(String processInstanceId);
    void notifyVerificationCallback(String processInstanceId, String decision);
    void completeExternalTask(String externalTaskId, String decision);
    BaseResponse deployBpmn(String xml, String filename);
}

