package com.base.service;

import com.base.model.Req.WorkflowContext;
import com.base.model.Res.BaseResponse;

public interface DmsDocService {
    BaseResponse fetchDoc();
    BaseResponse handleTask(WorkflowContext context);
}
