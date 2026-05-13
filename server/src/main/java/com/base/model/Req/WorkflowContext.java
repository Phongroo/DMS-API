package com.base.model.Req;

import com.base.model.DmsDoc;

public class WorkflowContext {
    private String processInstanceId;
    private String statusHandle;
    private DmsDoc dmsDoc;

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getStatusHandle() {
        return statusHandle;
    }

    public void setStatusHandle(String statusHandle) {
        this.statusHandle = statusHandle;
    }

    public DmsDoc getDmsDoc() {
        return dmsDoc;
    }

    public void setDmsDoc(DmsDoc dmsDoc) {
        this.dmsDoc = dmsDoc;
    }
}
