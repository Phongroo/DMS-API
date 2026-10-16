package com.base.model;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "audit_project")
public class AuditProject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String projectCode;

    private String companyName;
    private String taxCode;
    private String address;
    private String contactPerson;
    private String contactPhone;
    private String contactEmail;

    private String standard; // ISO 9001:2015, ISO 22000, QCVN 16:2023/BXD, TCVN...
    private String auditType; // INITIAL, SURVEILLANCE_1, SURVEILLANCE_2, RECERTIFICATION

    @Column(columnDefinition = "TEXT")
    private String certificationScope;

    private String leadAuditor;
    private String auditorMembers;
    private String technicalReviewer;

    private Date plannedStartDate;
    private Date plannedEndDate;
    private Date actualAuditDate;

    // Workflow status:
    // DRAFT -> STAGE_1_DESK_REVIEW -> STAGE_2_ON_SITE -> TESTING -> CAPA_PENDING -> TECHNICAL_REVIEW -> APPROVED -> CERTIFIED
    private String status;

    @Column(columnDefinition = "TEXT")
    private String deskReviewNotes;

    @Column(columnDefinition = "TEXT")
    private String onSiteAuditSummary;

    @Column(columnDefinition = "TEXT")
    private String technicalReviewNotes;

    private String processInstanceId;
    private String createdBy;
    private Date createdAt;
    private Date updatedAt;

    public AuditProject() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public void setProjectCode(String projectCode) {
        this.projectCode = projectCode;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getTaxCode() {
        return taxCode;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getStandard() {
        return standard;
    }

    public void setStandard(String standard) {
        this.standard = standard;
    }

    public String getAuditType() {
        return auditType;
    }

    public void setAuditType(String auditType) {
        this.auditType = auditType;
    }

    public String getCertificationScope() {
        return certificationScope;
    }

    public void setCertificationScope(String certificationScope) {
        this.certificationScope = certificationScope;
    }

    public String getLeadAuditor() {
        return leadAuditor;
    }

    public void setLeadAuditor(String leadAuditor) {
        this.leadAuditor = leadAuditor;
    }

    public String getAuditorMembers() {
        return auditorMembers;
    }

    public void setAuditorMembers(String auditorMembers) {
        this.auditorMembers = auditorMembers;
    }

    public String getTechnicalReviewer() {
        return technicalReviewer;
    }

    public void setTechnicalReviewer(String technicalReviewer) {
        this.technicalReviewer = technicalReviewer;
    }

    public Date getPlannedStartDate() {
        return plannedStartDate;
    }

    public void setPlannedStartDate(Date plannedStartDate) {
        this.plannedStartDate = plannedStartDate;
    }

    public Date getPlannedEndDate() {
        return plannedEndDate;
    }

    public void setPlannedEndDate(Date plannedEndDate) {
        this.plannedEndDate = plannedEndDate;
    }

    public Date getActualAuditDate() {
        return actualAuditDate;
    }

    public void setActualAuditDate(Date actualAuditDate) {
        this.actualAuditDate = actualAuditDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDeskReviewNotes() {
        return deskReviewNotes;
    }

    public void setDeskReviewNotes(String deskReviewNotes) {
        this.deskReviewNotes = deskReviewNotes;
    }

    public String getOnSiteAuditSummary() {
        return onSiteAuditSummary;
    }

    public void setOnSiteAuditSummary(String onSiteAuditSummary) {
        this.onSiteAuditSummary = onSiteAuditSummary;
    }

    public String getTechnicalReviewNotes() {
        return technicalReviewNotes;
    }

    public void setTechnicalReviewNotes(String technicalReviewNotes) {
        this.technicalReviewNotes = technicalReviewNotes;
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

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    @Transient
    private List<ActionButton> actionButtons;

    public List<ActionButton> getActionButtons() {
        return actionButtons;
    }

    public void setActionButtons(List<ActionButton> actionButtons) {
        this.actionButtons = actionButtons;
    }
}
