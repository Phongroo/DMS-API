package com.base.model.Req;

import com.base.model.DmsDoc;

public class ProcessTaskReq {
    private String processInstanceId;
    private String managerApproved;
    private String directorApproved;
    private DmsDoc dmsDoc;

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getManagerApproved() {
        return managerApproved;
    }

    public void setManagerApproved(String managerApproved) {
        this.managerApproved = managerApproved;
    }

    public String getDirectorApproved() {
        return directorApproved;
    }

    public void setDirectorApproved(String directorApproved) {
        this.directorApproved = directorApproved;
    }

    public DmsDoc getDmsDoc() {
        return dmsDoc;
    }

    public void setDmsDoc(DmsDoc dmsDoc) {
        this.dmsDoc = dmsDoc;
    }
}
