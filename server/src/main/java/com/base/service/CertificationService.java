package com.base.service;

import com.base.model.AuditNcr;
import com.base.model.AuditProject;
import com.base.model.AuditSample;
import com.base.model.Certificate;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface CertificationService {
    List<AuditProject> getAllProjects();
    AuditProject getProjectById(Long id);
    AuditProject createProject(AuditProject project, String username);
    AuditProject updateProject(Long id, AuditProject project);
    AuditProject updateProjectStatus(Long id, String status, String note, String username);

    List<AuditNcr> getNcrsByProjectId(Long projectId);
    AuditNcr createNcr(Long projectId, AuditNcr ncr, String username);
    AuditNcr submitCapa(Long ncrId, String rootCause, String correctiveAction, String correctiveEvidenceUrl);
    AuditNcr reviewCapa(Long ncrId, boolean approve, String closedBy);

    List<AuditSample> getSamplesByProjectId(Long projectId);
    AuditSample createSample(Long projectId, AuditSample sample);
    AuditSample updateSampleResult(Long sampleId, String testResult, String testReportNumber, Date testReportDate, String testReportFileUrl, String notes);

    Certificate issueCertificate(Long projectId, String directorUsername);
    Certificate getCertificateByProjectId(Long projectId);
    Certificate verifyCertificate(String query);

    Map<String, Object> getDashboardStats();
    Map<String, Object> getProjectBpmnViewer(Long projectId);
    void seedSampleDataIfEmpty();
}
