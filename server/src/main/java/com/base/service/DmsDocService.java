package com.base.service;

import com.base.model.Req.WorkflowContext;
import com.base.model.Res.BaseResponse;

public interface DmsDocService {
    BaseResponse fetchDoc();
    void handleTask(WorkflowContext context);
    boolean hasAccess(com.base.model.DmsDoc doc);
    BaseResponse reUploadFile(java.util.UUID docId, org.springframework.web.multipart.MultipartFile file) throws Exception;
}
