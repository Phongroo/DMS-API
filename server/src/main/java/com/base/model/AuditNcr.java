package com.base.model;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "audit_ncr")
public class AuditNcr {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long projectId;
    private String ncrCode; // e.g., "NCR-01"
    private String severity; // "MAJOR", "MINOR", "OFI"
    private String standardClause; // e.g., "8.5.1 Kiểm soát sản xuất và cung cấp dịch vụ"

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String evidence;

    private Date deadline;

    // CAPA (Corrective and Preventive Action) fields
    @Column(columnDefinition = "TEXT")
    private String rootCause;

    @Column(columnDefinition = "TEXT")
    private String correctiveAction;

    private String correctiveEvidenceUrl;

    // Status: "OPEN" (Mới phát hiện), "SUBMITTED" (DN đã nộp CAPA), "CLOSED" (Chuyên gia duyệt đạt), "REJECTED" (CAPA không đạt)
    private String status;

    private String createdBy;
    private Date createdAt;
    private String closedBy;
    private Date closedDate;

    public AuditNcr() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getNcrCode() {
        return ncrCode;
    }

    public void setNcrCode(String ncrCode) {
        this.ncrCode = ncrCode;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getStandardClause() {
        return standardClause;
    }

    public void setStandardClause(String standardClause) {
        this.standardClause = standardClause;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    public Date getDeadline() {
        return deadline;
    }

    public void setDeadline(Date deadline) {
        this.deadline = deadline;
    }

    public String getRootCause() {
        return rootCause;
    }

    public void setRootCause(String rootCause) {
        this.rootCause = rootCause;
    }

    public String getCorrectiveAction() {
        return correctiveAction;
    }

    public void setCorrectiveAction(String correctiveAction) {
        this.correctiveAction = correctiveAction;
    }

    public String getCorrectiveEvidenceUrl() {
        return correctiveEvidenceUrl;
    }

    public void setCorrectiveEvidenceUrl(String correctiveEvidenceUrl) {
        this.correctiveEvidenceUrl = correctiveEvidenceUrl;
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

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getClosedBy() {
        return closedBy;
    }

    public void setClosedBy(String closedBy) {
        this.closedBy = closedBy;
    }

    public Date getClosedDate() {
        return closedDate;
    }

    public void setClosedDate(Date closedDate) {
        this.closedDate = closedDate;
    }
}
