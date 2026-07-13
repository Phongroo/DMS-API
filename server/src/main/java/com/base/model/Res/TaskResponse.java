package com.base.model.Res;

import com.base.model.ActionButton;
import com.base.model.DmsDoc;
import com.base.model.DmsFile;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public class TaskResponse {
    private UUID id;
    private String docId;
    private String processInstanceId;
    private String status;
    private String createdBy;
    private String creatorName;
    private Date createdDate;

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }
    private String securityLevel;
    private String allowedRole;
    private String allowedPositions;
    private Long branchId;

    public Long getBranchId() {
        return branchId;
    }

    public void setBranchId(Long branchId) {
        this.branchId = branchId;
    }

    public String getAllowedRole() {
        return allowedRole;
    }

    public void setAllowedRole(String allowedRole) {
        this.allowedRole = allowedRole;
    }

    public String getAllowedPositions() {
        return allowedPositions;
    }

    public void setAllowedPositions(String allowedPositions) {
        this.allowedPositions = allowedPositions;
    }

    private String docType;

    public String getSecurityLevel() {
        return securityLevel;
    }

    public void setSecurityLevel(String securityLevel) {
        this.securityLevel = securityLevel;
    }

    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }
    private List<ActionButton> actionButtons;
    private List<DmsFile> dmsFiles;
    public TaskResponse(){};

    public TaskResponse(UUID id, String docId, String processInstanceId, String status, String createdBy, Date createdDate, List<ActionButton> actionButtons) {
        this.id = id;
        this.docId = docId;
        this.processInstanceId = processInstanceId;
        this.status = status;
        this.createdBy = createdBy;
        this.createdDate = createdDate;
        this.actionButtons = actionButtons;
    }

    public List<DmsFile> getDmsFiles() {
        return dmsFiles;
    }

    public void setDmsFiles(List<DmsFile> dmsFiles) {
        this.dmsFiles = dmsFiles;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public List<ActionButton> getActionButtons() {
        return actionButtons;
    }

    public void setActionButtons(List<ActionButton> actionButtons) {
        this.actionButtons = actionButtons;
    }
}
