package com.base.model;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "certificate")
public class Certificate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long projectId;

    @Column(unique = true, nullable = false)
    private String certificateNumber; // e.g. "VIC.HQ.16.2026.088"

    private String companyName;
    private String taxCode;
    private String address;
    private String standard;

    @Column(columnDefinition = "TEXT")
    private String scope;

    private Date issueDate;
    private Date expiryDate; // Usually 3 years from issue date
    private Date surveillance1Date; // 12 months from issue date
    private Date surveillance2Date; // 24 months from issue date

    @Column(unique = true)
    private String qrVerificationCode;

    // Status: "VALID" (Hiệu lực), "SURVEILLANCE_DUE" (Sắp đến hạn giám sát), "SUSPENDED" (Tạm đình chỉ), "EXPIRED" (Hết hiệu lực), "REVOKED" (Thu hồi)
    private String status;

    private String approvedBy; // Cert Director username
    private Date approvedAt;

    private String certificateFileUrl;

    public Certificate() {}

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

    public String getCertificateNumber() {
        return certificateNumber;
    }

    public void setCertificateNumber(String certificateNumber) {
        this.certificateNumber = certificateNumber;
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

    public String getStandard() {
        return standard;
    }

    public void setStandard(String standard) {
        this.standard = standard;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public Date getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(Date issueDate) {
        this.issueDate = issueDate;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Date getSurveillance1Date() {
        return surveillance1Date;
    }

    public void setSurveillance1Date(Date surveillance1Date) {
        this.surveillance1Date = surveillance1Date;
    }

    public Date getSurveillance2Date() {
        return surveillance2Date;
    }

    public void setSurveillance2Date(Date surveillance2Date) {
        this.surveillance2Date = surveillance2Date;
    }

    public String getQrVerificationCode() {
        return qrVerificationCode;
    }

    public void setQrVerificationCode(String qrVerificationCode) {
        this.qrVerificationCode = qrVerificationCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Date getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Date approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getCertificateFileUrl() {
        return certificateFileUrl;
    }

    public void setCertificateFileUrl(String certificateFileUrl) {
        this.certificateFileUrl = certificateFileUrl;
    }
}
