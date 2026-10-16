package com.base.model;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "audit_sample")
public class AuditSample {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long projectId;
    private String sampleCode; // e.g. "SMP-2026-001"
    private String sampleName; // Tên mẫu (ví dụ: Gạch gốm ốp lát, Ống nhựa PVC, Sữa tiệt trùng...)
    private String samplingLocation; // Vị trí lấy mẫu (Kho thành phẩm, Dây chuyền sản xuất...)
    private Date samplingDate;
    private String samplerName; // Người lấy mẫu (Auditor)

    private String testingLab; // Phòng thử nghiệm (VILAS xxx / PTN chỉ định)
    private String testReportNumber; // Số phiếu kết quả thử nghiệm (e.g. "KQ-892/PTN")
    private Date testReportDate;
    // Status: "PENDING" (Chờ kết quả), "PASS" (Đạt tiêu chuẩn), "FAIL" (Không đạt)
    private String testResult;
    private String testReportFileUrl;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private Date createdAt;

    public AuditSample() {}

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

    public String getSampleCode() {
        return sampleCode;
    }

    public void setSampleCode(String sampleCode) {
        this.sampleCode = sampleCode;
    }

    public String getSampleName() {
        return sampleName;
    }

    public void setSampleName(String sampleName) {
        this.sampleName = sampleName;
    }

    public String getSamplingLocation() {
        return samplingLocation;
    }

    public void setSamplingLocation(String samplingLocation) {
        this.samplingLocation = samplingLocation;
    }

    public Date getSamplingDate() {
        return samplingDate;
    }

    public void setSamplingDate(Date samplingDate) {
        this.samplingDate = samplingDate;
    }

    public String getSamplerName() {
        return samplerName;
    }

    public void setSamplerName(String samplerName) {
        this.samplerName = samplerName;
    }

    public String getTestingLab() {
        return testingLab;
    }

    public void setTestingLab(String testingLab) {
        this.testingLab = testingLab;
    }

    public String getTestReportNumber() {
        return testReportNumber;
    }

    public void setTestReportNumber(String testReportNumber) {
        this.testReportNumber = testReportNumber;
    }

    public Date getTestReportDate() {
        return testReportDate;
    }

    public void setTestReportDate(Date testReportDate) {
        this.testReportDate = testReportDate;
    }

    public String getTestResult() {
        return testResult;
    }

    public void setTestResult(String testResult) {
        this.testResult = testResult;
    }

    public String getTestReportFileUrl() {
        return testReportFileUrl;
    }

    public void setTestReportFileUrl(String testReportFileUrl) {
        this.testReportFileUrl = testReportFileUrl;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
