package com.base.service.impl;

import com.base.model.ActionButton;
import com.base.model.AuditNcr;
import com.base.model.AuditProject;
import com.base.model.AuditSample;
import com.base.model.Authority;
import com.base.model.Certificate;
import com.base.model.CertificationActionConfig;
import com.base.repo.AuditNcrRepository;
import com.base.repo.AuditProjectRepository;
import com.base.repo.AuditSampleRepository;
import com.base.repo.CertificateRepository;
import com.base.service.AuditLogService;
import com.base.service.CamundaService;
import com.base.service.CertificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class CertificationServiceImpl implements CertificationService {

    @Autowired
    private AuditProjectRepository auditProjectRepository;

    @Autowired
    private AuditNcrRepository auditNcrRepository;

    @Autowired
    private AuditSampleRepository auditSampleRepository;

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private CamundaService camundaService;

    @Autowired
    private com.base.service.SystemSettingsService systemSettingsService;

    @org.springframework.beans.factory.annotation.Value("${camunda.url}")
    private String defaultCamundaUrl;

    private String getCamundaUrl() {
        try {
            return systemSettingsService.getSettings().getCamundaUrl();
        } catch (Exception e) {
            return defaultCamundaUrl;
        }
    }

    private org.springframework.web.client.RestTemplate getRestTemplate() {
        org.springframework.web.client.RestTemplate rt = new org.springframework.web.client.RestTemplate();
        try {
            int timeout = systemSettingsService.getSettings().getApiTimeout();
            org.springframework.http.client.SimpleClientHttpRequestFactory factory = 
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(timeout);
            factory.setReadTimeout(timeout);
            rt.setRequestFactory(factory);
        } catch (Exception ignored) {}
        return rt;
    }

    private String getCurrentUserPosition() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        return auth.getAuthorities()
                .stream()
                .filter(a -> a instanceof Authority)
                .map(a -> ((Authority) a).getPosition())
                .findFirst()
                .orElse(null);
    }

    private boolean isCurrentUserAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> "ADMIN".equalsIgnoreCase(a.getAuthority()) || "ROLE_ADMIN".equalsIgnoreCase(a.getAuthority()))
                || "phongro".equalsIgnoreCase(auth.getName());
    }

    @PostConstruct
    public void init() {
        try {
            seedSampleDataIfEmpty();
        } catch (Exception e) {
            System.err.println("Seed sample certification data failed: " + e.getMessage());
        }
    }

    @Override
    public List<AuditProject> getAllProjects() {
        List<AuditProject> projects = auditProjectRepository.findAll();
        String position = getCurrentUserPosition();
        boolean isAdmin = isCurrentUserAdmin();
        for (AuditProject p : projects) {
            p.setActionButtons(CertificationActionConfig.getButtons(p.getStatus(), position, isAdmin));
        }
        return projects;
    }

    @Override
    public AuditProject getProjectById(Long id) {
        AuditProject p = auditProjectRepository.findById(id).orElse(null);
        if (p != null) {
            p.setActionButtons(CertificationActionConfig.getButtons(p.getStatus(), getCurrentUserPosition(), isCurrentUserAdmin()));
        }
        return p;
    }

    @Override
    public AuditProject createProject(AuditProject project, String username) {
        if (project.getProjectCode() == null || project.getProjectCode().trim().isEmpty()) {
            project.setProjectCode("PRJ-" + Calendar.getInstance().get(Calendar.YEAR) + "-" + String.format("%03d", (auditProjectRepository.count() + 1)));
        }
        if (project.getStatus() == null || project.getStatus().trim().isEmpty()) {
            project.setStatus("STAGE_1_DESK_REVIEW");
        }
        project.setCreatedBy(username != null ? username : "system");
        project.setCreatedAt(new Date());
        project.setUpdatedAt(new Date());

        // Start Camunda Process Instance
        try {
            String camundaUrl = getCamundaUrl();
            org.springframework.web.client.RestTemplate restTemplate = getRestTemplate();

            Map<String, Object> reqBody = new HashMap<>();
            reqBody.put("businessKey", project.getProjectCode());

            Map<String, Object> variables = new HashMap<>();
            variables.put("projectId", Map.of("value", project.getProjectCode(), "type", "String"));
            variables.put("companyName", Map.of("value", project.getCompanyName() != null ? project.getCompanyName() : "", "type", "String"));
            variables.put("standard", Map.of("value", project.getStandard() != null ? project.getStandard() : "", "type", "String"));
            variables.put("leadAuditor", Map.of("value", project.getLeadAuditor() != null ? project.getLeadAuditor() : (username != null ? username : "admin"), "type", "String"));
            reqBody.put("variables", variables);

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            org.springframework.http.HttpEntity<Map<String, Object>> entity = new org.springframework.http.HttpEntity<>(reqBody, headers);

            org.springframework.http.ResponseEntity<Map> response = restTemplate.postForEntity(
                    camundaUrl + "/engine-rest/process-definition/key/certification_assessment/start",
                    entity,
                    Map.class
            );

            if (response.getBody() != null && response.getBody().get("id") != null) {
                String processInstanceId = String.valueOf(response.getBody().get("id"));
                project.setProcessInstanceId(processInstanceId);
                auditLogService.log("Đã khởi tạo tiến trình Camunda (" + processInstanceId + ") cho dự án " + project.getProjectCode(), "INFO");
            }
        } catch (Exception e) {
            System.err.println("Could not start Camunda process instance: " + e.getMessage());
        }

        AuditProject saved = auditProjectRepository.save(project);
        saved.setActionButtons(CertificationActionConfig.getButtons(saved.getStatus(), getCurrentUserPosition(), isCurrentUserAdmin()));
        auditLogService.log("Khởi tạo dự án đánh giá chứng nhận: " + saved.getProjectCode() + " - " + saved.getCompanyName(), "INFO");
        return saved;
    }

    @Override
    public AuditProject updateProject(Long id, AuditProject updatedData) {
        AuditProject project = auditProjectRepository.findById(id).orElse(null);
        if (project == null) return null;

        if (updatedData.getCompanyName() != null) project.setCompanyName(updatedData.getCompanyName());
        if (updatedData.getTaxCode() != null) project.setTaxCode(updatedData.getTaxCode());
        if (updatedData.getAddress() != null) project.setAddress(updatedData.getAddress());
        if (updatedData.getContactPerson() != null) project.setContactPerson(updatedData.getContactPerson());
        if (updatedData.getContactPhone() != null) project.setContactPhone(updatedData.getContactPhone());
        if (updatedData.getContactEmail() != null) project.setContactEmail(updatedData.getContactEmail());
        if (updatedData.getStandard() != null) project.setStandard(updatedData.getStandard());
        if (updatedData.getAuditType() != null) project.setAuditType(updatedData.getAuditType());
        if (updatedData.getCertificationScope() != null) project.setCertificationScope(updatedData.getCertificationScope());
        if (updatedData.getLeadAuditor() != null) project.setLeadAuditor(updatedData.getLeadAuditor());
        if (updatedData.getAuditorMembers() != null) project.setAuditorMembers(updatedData.getAuditorMembers());
        if (updatedData.getTechnicalReviewer() != null) project.setTechnicalReviewer(updatedData.getTechnicalReviewer());
        if (updatedData.getPlannedStartDate() != null) project.setPlannedStartDate(updatedData.getPlannedStartDate());
        if (updatedData.getPlannedEndDate() != null) project.setPlannedEndDate(updatedData.getPlannedEndDate());
        if (updatedData.getActualAuditDate() != null) project.setActualAuditDate(updatedData.getActualAuditDate());
        if (updatedData.getDeskReviewNotes() != null) project.setDeskReviewNotes(updatedData.getDeskReviewNotes());
        if (updatedData.getOnSiteAuditSummary() != null) project.setOnSiteAuditSummary(updatedData.getOnSiteAuditSummary());
        if (updatedData.getTechnicalReviewNotes() != null) project.setTechnicalReviewNotes(updatedData.getTechnicalReviewNotes());

        project.setUpdatedAt(new Date());
        return auditProjectRepository.save(project);
    }

    @Override
    public AuditProject updateProjectStatus(Long id, String status, String note, String username) {
        AuditProject project = auditProjectRepository.findById(id).orElse(null);
        if (project == null) return null;

        String position = getCurrentUserPosition();
        boolean isAdmin = isCurrentUserAdmin();

        // Enforce role-based position rules like Document Workflow
        if (!isAdmin) {
            List<ActionButton> allowedButtons = CertificationActionConfig.getButtons(project.getStatus(), position, false);
            boolean isAllowed = allowedButtons.stream().anyMatch(b -> status.equalsIgnoreCase(b.getAction()));
            if (!isAllowed) {
                throw new RuntimeException("Quy tắc nghiệp vụ: Vị trí '" + (position != null ? position : "NORMAL") +
                        "' không có thẩm quyền chuyển trạng thái từ '" + project.getStatus() + "' sang '" + status + "'!");
            }
        }

        // ISO 17021/17065 Rule: To advance to TECHNICAL_REVIEW or APPROVED, all MAJOR NCRs must be closed!
        if ("TECHNICAL_REVIEW".equalsIgnoreCase(status) || "APPROVED".equalsIgnoreCase(status)) {
            List<AuditNcr> majorNcrs = auditNcrRepository.findByProjectIdAndSeverity(id, "MAJOR");
            for (AuditNcr ncr : majorNcrs) {
                if (!"CLOSED".equalsIgnoreCase(ncr.getStatus())) {
                    throw new RuntimeException("Vi phạm ISO 17021/17065: Không thể chuyển sang thẩm xét kỹ thuật khi còn điểm không phù hợp nặng (Major NCR: " + ncr.getNcrCode() + ") chưa được khắc phục và đóng lỗi!");
                }
            }
        }

        project.setStatus(status);
        project.setUpdatedAt(new Date());

        if (note != null && !note.trim().isEmpty()) {
            if ("STAGE_1_DESK_REVIEW".equalsIgnoreCase(status)) {
                project.setDeskReviewNotes(note);
            } else if ("STAGE_2_ON_SITE".equalsIgnoreCase(status) || "TESTING".equalsIgnoreCase(status)) {
                project.setOnSiteAuditSummary(note);
            } else if ("TECHNICAL_REVIEW".equalsIgnoreCase(status)) {
                project.setTechnicalReviewNotes(note);
            }
        }

        // Ensure Camunda process instance exists
        if (project.getProcessInstanceId() == null || project.getProcessInstanceId().trim().isEmpty()) {
            try {
                String camundaUrl = getCamundaUrl();
                org.springframework.web.client.RestTemplate restTemplate = getRestTemplate();
                Map<String, Object> reqBody = new HashMap<>();
                reqBody.put("businessKey", project.getProjectCode());
                org.springframework.http.ResponseEntity<Map> response = restTemplate.postForEntity(
                        camundaUrl + "/engine-rest/process-definition/key/certification_assessment/start",
                        new org.springframework.http.HttpEntity<>(reqBody, new org.springframework.http.HttpHeaders()),
                        Map.class
                );
                if (response.getBody() != null && response.getBody().get("id") != null) {
                    project.setProcessInstanceId(String.valueOf(response.getBody().get("id")));
                }
            } catch (Exception ignored) {}
        }

        // Advance / Complete active user task on Camunda Engine
        if (project.getProcessInstanceId() != null && !project.getProcessInstanceId().trim().isEmpty()) {
            try {
                String camundaUrl = getCamundaUrl();
                org.springframework.web.client.RestTemplate restTemplate = getRestTemplate();
                String taskQueryUrl = camundaUrl + "/engine-rest/task?processInstanceId=" + project.getProcessInstanceId();
                org.springframework.http.ResponseEntity<List> taskResp = restTemplate.getForEntity(taskQueryUrl, List.class);
                if (taskResp.getBody() != null && !taskResp.getBody().isEmpty()) {
                    Map firstTask = (Map) taskResp.getBody().get(0);
                    String taskId = (String) firstTask.get("id");

                    Map<String, Object> completeBody = new HashMap<>();
                    Map<String, Object> vars = new HashMap<>();

                    if ("CAPA_PENDING".equalsIgnoreCase(status)) {
                        vars.put("hasNcr", Map.of("value", true, "type", "Boolean"));
                    } else if ("TECHNICAL_REVIEW".equalsIgnoreCase(status)) {
                        vars.put("hasNcr", Map.of("value", false, "type", "Boolean"));
                    } else if ("APPROVED".equalsIgnoreCase(status)) {
                        vars.put("reviewPassed", Map.of("value", true, "type", "Boolean"));
                    } else if ("REJECTED".equalsIgnoreCase(status)) {
                        vars.put("reviewPassed", Map.of("value", false, "type", "Boolean"));
                    }
                    completeBody.put("variables", vars);

                    org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                    headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
                    org.springframework.http.HttpEntity<Map<String, Object>> entity = new org.springframework.http.HttpEntity<>(completeBody, headers);

                    restTemplate.postForEntity(camundaUrl + "/engine-rest/task/" + taskId + "/complete", entity, Object.class);
                }
            } catch (Exception ex) {
                System.err.println("Could not complete Camunda task: " + ex.getMessage());
            }
        }

        AuditProject saved = auditProjectRepository.save(project);
        saved.setActionButtons(CertificationActionConfig.getButtons(saved.getStatus(), position, isAdmin));
        auditLogService.log("Cập nhật trạng thái dự án " + project.getProjectCode() + " -> " + status + " bởi " + (username != null ? username : "user"), "INFO");
        return saved;
    }

    @Override
    public List<AuditNcr> getNcrsByProjectId(Long projectId) {
        return auditNcrRepository.findByProjectId(projectId);
    }

    @Override
    public AuditNcr createNcr(Long projectId, AuditNcr ncr, String username) {
        ncr.setProjectId(projectId);
        if (ncr.getNcrCode() == null || ncr.getNcrCode().trim().isEmpty()) {
            long count = auditNcrRepository.findByProjectId(projectId).size() + 1;
            ncr.setNcrCode("NCR-" + String.format("%02d", count));
        }
        if (ncr.getStatus() == null || ncr.getStatus().trim().isEmpty()) {
            ncr.setStatus("OPEN");
        }
        if (ncr.getSeverity() == null || ncr.getSeverity().trim().isEmpty()) {
            ncr.setSeverity("MINOR");
        }
        ncr.setCreatedBy(username != null ? username : "auditor");
        ncr.setCreatedAt(new Date());

        AuditNcr saved = auditNcrRepository.save(ncr);
        auditLogService.log("Lập biên bản điểm không phù hợp " + saved.getNcrCode() + " (" + saved.getSeverity() + ") cho dự án #" + projectId, "WARN");
        return saved;
    }

    @Override
    public AuditNcr submitCapa(Long ncrId, String rootCause, String correctiveAction, String correctiveEvidenceUrl) {
        AuditNcr ncr = auditNcrRepository.findById(ncrId).orElse(null);
        if (ncr == null) return null;

        ncr.setRootCause(rootCause);
        ncr.setCorrectiveAction(correctiveAction);
        if (correctiveEvidenceUrl != null) {
            ncr.setCorrectiveEvidenceUrl(correctiveEvidenceUrl);
        }
        ncr.setStatus("SUBMITTED");
        return auditNcrRepository.save(ncr);
    }

    @Override
    public AuditNcr reviewCapa(Long ncrId, boolean approve, String closedBy) {
        AuditNcr ncr = auditNcrRepository.findById(ncrId).orElse(null);
        if (ncr == null) return null;

        if (approve) {
            ncr.setStatus("CLOSED");
            ncr.setClosedBy(closedBy != null ? closedBy : "lead_auditor");
            ncr.setClosedDate(new Date());
            auditLogService.log("Trưởng đoàn đã duyệt đóng lỗi " + ncr.getNcrCode(), "INFO");
        } else {
            ncr.setStatus("REJECTED");
            auditLogService.log("Biện pháp khắc phục lỗi " + ncr.getNcrCode() + " không đạt, yêu cầu làm lại", "WARN");
        }
        return auditNcrRepository.save(ncr);
    }

    @Override
    public List<AuditSample> getSamplesByProjectId(Long projectId) {
        return auditSampleRepository.findByProjectId(projectId);
    }

    @Override
    public AuditSample createSample(Long projectId, AuditSample sample) {
        sample.setProjectId(projectId);
        if (sample.getSampleCode() == null || sample.getSampleCode().trim().isEmpty()) {
            long count = auditSampleRepository.findByProjectId(projectId).size() + 1;
            sample.setSampleCode("SMP-" + String.format("%02d", count));
        }
        if (sample.getTestResult() == null || sample.getTestResult().trim().isEmpty()) {
            sample.setTestResult("PENDING");
        }
        sample.setCreatedAt(new Date());
        return auditSampleRepository.save(sample);
    }

    @Override
    public AuditSample updateSampleResult(Long sampleId, String testResult, String testReportNumber, Date testReportDate, String testReportFileUrl, String notes) {
        AuditSample sample = auditSampleRepository.findById(sampleId).orElse(null);
        if (sample == null) return null;

        sample.setTestResult(testResult);
        if (testReportNumber != null) sample.setTestReportNumber(testReportNumber);
        if (testReportDate != null) sample.setTestReportDate(testReportDate);
        if (testReportFileUrl != null) sample.setTestReportFileUrl(testReportFileUrl);
        if (notes != null) sample.setNotes(notes);

        return auditSampleRepository.save(sample);
    }

    @Override
    public Certificate issueCertificate(Long projectId, String directorUsername) {
        AuditProject project = auditProjectRepository.findById(projectId).orElse(null);
        if (project == null) throw new RuntimeException("Dự án không tồn tại");

        String position = getCurrentUserPosition();
        boolean isAdmin = isCurrentUserAdmin();
        if (!isAdmin && !"CERT_DIRECTOR".equalsIgnoreCase(position) && !"DIRECTOR".equalsIgnoreCase(position)) {
            throw new RuntimeException("Quy tắc nghiệp vụ: Chỉ Giám đốc Chứng nhận (CERT_DIRECTOR) mới có quyền ký duyệt phát hành Giấy chứng nhận!");
        }

        // Verify major NCRs are closed
        List<AuditNcr> majorNcrs = auditNcrRepository.findByProjectIdAndSeverity(projectId, "MAJOR");
        for (AuditNcr ncr : majorNcrs) {
            if (!"CLOSED".equalsIgnoreCase(ncr.getStatus())) {
                throw new RuntimeException("Không thể cấp GCN: Còn tồn tại Major NCR chưa đóng: " + ncr.getNcrCode());
            }
        }

        Certificate cert = certificateRepository.findByProjectId(projectId).orElse(new Certificate());
        cert.setProjectId(projectId);

        Calendar cal = Calendar.getInstance();
        Date issueDate = new Date();
        cert.setIssueDate(issueDate);

        // Expiry = issue + 3 years
        cal.setTime(issueDate);
        cal.add(Calendar.YEAR, 3);
        cert.setExpiryDate(cal.getTime());

        // Surveillance 1 = issue + 12 months
        cal.setTime(issueDate);
        cal.add(Calendar.MONTH, 12);
        cert.setSurveillance1Date(cal.getTime());

        // Surveillance 2 = issue + 24 months
        cal.setTime(issueDate);
        cal.add(Calendar.MONTH, 24);
        cert.setSurveillance2Date(cal.getTime());

        if (cert.getCertificateNumber() == null || cert.getCertificateNumber().trim().isEmpty()) {
            String prefix = project.getStandard() != null && project.getStandard().contains("QCVN") ? "VIC.HQ." : "VIC.ISO.";
            cert.setCertificateNumber(prefix + Calendar.getInstance().get(Calendar.YEAR) + "." + String.format("%04d", projectId));
        }

        cert.setCompanyName(project.getCompanyName());
        cert.setTaxCode(project.getTaxCode());
        cert.setAddress(project.getAddress());
        cert.setStandard(project.getStandard());
        cert.setScope(project.getCertificationScope());
        cert.setQrVerificationCode(UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        cert.setStatus("VALID");
        cert.setApprovedBy(directorUsername != null ? directorUsername : "cert_director");
        cert.setApprovedAt(new Date());

        Certificate saved = certificateRepository.save(cert);

        // Update project status to CERTIFIED
        project.setStatus("CERTIFIED");
        auditProjectRepository.save(project);

        auditLogService.log("Giám đốc chứng nhận đã ký duyệt và cấp Giấy chứng nhận: " + saved.getCertificateNumber() + " cho " + saved.getCompanyName(), "INFO");
        return saved;
    }

    @Override
    public Certificate getCertificateByProjectId(Long projectId) {
        return certificateRepository.findByProjectId(projectId).orElse(null);
    }

    @Override
    public Certificate verifyCertificate(String query) {
        if (query == null || query.trim().isEmpty()) return null;
        String cleanQuery = query.trim();

        // Find by certificate number
        Optional<Certificate> byNumber = certificateRepository.findByCertificateNumber(cleanQuery);
        if (byNumber.isPresent()) return byNumber.get();

        // Find by QR code
        Optional<Certificate> byQr = certificateRepository.findByQrVerificationCode(cleanQuery);
        if (byQr.isPresent()) return byQr.get();

        // Find by Tax code / Company
        List<Certificate> byCompany = certificateRepository.findByCompanyNameContainingIgnoreCase(cleanQuery);
        if (!byCompany.isEmpty()) return byCompany.get(0);

        return null;
    }

    @Override
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        long totalProjects = auditProjectRepository.count();
        long certifiedCount = certificateRepository.count();
        long validCertCount = certificateRepository.countByStatus("VALID");
        long openNcrs = auditNcrRepository.countByStatus("OPEN");
        long pendingTests = auditSampleRepository.countByTestResult("PENDING");

        stats.put("totalProjects", totalProjects);
        stats.put("totalCertificates", certifiedCount);
        stats.put("validCertificates", validCertCount);
        stats.put("openNcrs", openNcrs);
        stats.put("pendingTests", pendingTests);
        return stats;
    }

    @Override
    public Map<String, Object> getProjectBpmnViewer(Long projectId) {
        AuditProject project = auditProjectRepository.findById(projectId).orElse(null);
        if (project == null) {
            throw new RuntimeException("Dự án không tồn tại");
        }

        String bpmnXml = "";
        try {
            ClassPathResource res = new ClassPathResource("processes/certification_assessment.bpmn");
            if (res.exists()) {
                try (InputStream is = res.getInputStream()) {
                    bpmnXml = StreamUtils.copyToString(is, StandardCharsets.UTF_8);
                }
            }
        } catch (Exception e) {
            System.err.println("Could not read BPMN resource: " + e.getMessage());
        }

        List<String> running = new ArrayList<>();
        List<String> completed = new ArrayList<>();
        List<String> error = new ArrayList<>();
        List<String> flows = new ArrayList<>();

        // If project has processInstanceId on Camunda, query live activities
        if (project.getProcessInstanceId() != null && !project.getProcessInstanceId().trim().isEmpty()) {
            try {
                com.base.model.Res.BaseResponse liveViewer = camundaService.viewTask(project.getProcessInstanceId());
                if (liveViewer != null && liveViewer.getData() instanceof Map) {
                    Map<String, Object> liveData = (Map<String, Object>) liveViewer.getData();
                    if (liveData.get("bpmn20Xml") != null && !String.valueOf(liveData.get("bpmn20Xml")).isEmpty()) {
                        bpmnXml = (String) liveData.get("bpmn20Xml");
                    }
                    if (liveData.get("runningActivities") instanceof List && !((List<?>) liveData.get("runningActivities")).isEmpty()) {
                        running = (List<String>) liveData.get("runningActivities");
                    }
                    if (liveData.get("completedActivities") instanceof List && !((List<?>) liveData.get("completedActivities")).isEmpty()) {
                        completed = (List<String>) liveData.get("completedActivities");
                    }
                    if (liveData.get("errorActivities") instanceof List) {
                        error = (List<String>) liveData.get("errorActivities");
                    }
                    if (liveData.get("flows") instanceof List) {
                        flows = (List<String>) liveData.get("flows");
                    }
                }
            } catch (Exception ex) {
                System.err.println("Could not load live Camunda viewer: " + ex.getMessage());
            }
        }

        String status = project.getStatus() != null ? project.getStatus() : "STAGE_1_DESK_REVIEW";
        if (completed.isEmpty()) {
            completed.add("StartEvent_Cert");
        }

        if (running.isEmpty()) {
            switch (status) {
                case "STAGE_1_DESK_REVIEW":
                    running.add("Task_DeskReview");
                    flows.add("Flow_1");
                    break;
                case "STAGE_2_ON_SITE":
                    completed.add("Task_DeskReview");
                    running.add("Task_OnSiteAudit");
                    flows.addAll(Arrays.asList("Flow_1", "Flow_2"));
                    break;
                case "TESTING":
                    completed.addAll(Arrays.asList("Task_DeskReview", "Task_OnSiteAudit"));
                    running.add("Task_LabTesting");
                    flows.addAll(Arrays.asList("Flow_1", "Flow_2", "Flow_3"));
                    break;
                case "CAPA_PENDING":
                    completed.addAll(Arrays.asList("Task_DeskReview", "Task_OnSiteAudit", "Task_LabTesting", "Gateway_NCR"));
                    running.add("Task_CAPA");
                    flows.addAll(Arrays.asList("Flow_1", "Flow_2", "Flow_3", "Flow_4", "Flow_HasNCR"));
                    break;
                case "TECHNICAL_REVIEW":
                    completed.addAll(Arrays.asList("Task_DeskReview", "Task_OnSiteAudit", "Task_LabTesting", "Gateway_NCR", "Task_CAPA"));
                    running.add("Task_TechnicalReview");
                    flows.addAll(Arrays.asList("Flow_1", "Flow_2", "Flow_3", "Flow_4", "Flow_HasNCR", "Flow_CAPADone", "Flow_5"));
                    break;
                case "APPROVED":
                    completed.addAll(Arrays.asList("Task_DeskReview", "Task_OnSiteAudit", "Task_LabTesting", "Gateway_NCR", "Task_CAPA", "Task_TechnicalReview", "Gateway_ReviewDecision"));
                    running.add("Task_IssueCert");
                    flows.addAll(Arrays.asList("Flow_1", "Flow_2", "Flow_3", "Flow_4", "Flow_HasNCR", "Flow_CAPADone", "Flow_5", "Flow_ReviewPass", "Flow_6"));
                    break;
                case "CERTIFIED":
                    completed.addAll(Arrays.asList("Task_DeskReview", "Task_OnSiteAudit", "Task_LabTesting", "Gateway_NCR", "Task_CAPA", "Task_TechnicalReview", "Gateway_ReviewDecision", "Task_IssueCert", "EndEvent_Certified"));
                    flows.addAll(Arrays.asList("Flow_1", "Flow_2", "Flow_3", "Flow_4", "Flow_HasNCR", "Flow_CAPADone", "Flow_5", "Flow_ReviewPass", "Flow_6"));
                    break;
                case "REJECTED":
                    completed.addAll(Arrays.asList("Task_DeskReview", "Task_OnSiteAudit", "Task_TechnicalReview", "Gateway_ReviewDecision"));
                    running.add("EndEvent_Rejected");
                    flows.addAll(Arrays.asList("Flow_1", "Flow_2", "Flow_3", "Flow_5", "Flow_ReviewFail"));
                    break;
                default:
                    running.add("Task_DeskReview");
                    flows.add("Flow_1");
                    break;
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("projectId", project.getId());
        data.put("processInstanceId", project.getProcessInstanceId());
        data.put("projectCode", project.getProjectCode());
        data.put("companyName", project.getCompanyName());
        data.put("standard", project.getStandard());
        data.put("status", status);
        data.put("bpmn20Xml", bpmnXml);
        data.put("runningActivities", running);
        data.put("completedActivities", completed);
        data.put("errorActivities", error);
        data.put("flows", flows);

        String position = getCurrentUserPosition();
        boolean isAdmin = isCurrentUserAdmin();
        data.put("actionButtons", CertificationActionConfig.getButtons(status, position, isAdmin));

        return data;
    }

    @Override
    public void seedSampleDataIfEmpty() {
        if (auditProjectRepository.count() > 0) return;

        Calendar cal = Calendar.getInstance();

        // Project 1: Hợp quy Vật liệu Xây dựng QCVN 16:2023/BXD
        AuditProject p1 = new AuditProject();
        p1.setProjectCode("PRJ-2026-001");
        p1.setCompanyName("Tập đoàn Gốm Sứ Viglacera");
        p1.setTaxCode("0100106263");
        p1.setAddress("KCN Tiên Sơn, Phường Đồng Nguyên, TP Từ Sơn, Bắc Ninh");
        p1.setContactPerson("Ông Nguyễn Văn Thắng (Giám đốc Kỹ thuật)");
        p1.setContactPhone("0912345678");
        p1.setContactEmail("thangnv@viglacera.vn");
        p1.setStandard("QCVN 16:2023/BXD");
        p1.setAuditType("INITIAL");
        p1.setCertificationScope("Sản xuất và chứng nhận hợp quy gạch ốp lát ceramic tráng men nhóm BIIa");
        p1.setLeadAuditor("hongphong");
        p1.setAuditorMembers("Chuyên gia Nguyễn Thanh Sơn, Kỹ thuật viên Lê Hải Đăng");
        p1.setTechnicalReviewer("emmadirector");
        p1.setPlannedStartDate(new Date(System.currentTimeMillis() - 86400000L * 10));
        p1.setPlannedEndDate(new Date(System.currentTimeMillis() - 86400000L * 8));
        p1.setActualAuditDate(new Date(System.currentTimeMillis() - 86400000L * 8));
        p1.setStatus("TESTING");
        p1.setDeskReviewNotes("Hồ sơ hệ thống kiểm soát chất lượng tại nhà máy (FPC) đầy đủ.");
        p1.setOnSiteAuditSummary("Đã lấy mẫu tại kho thành phẩm lô sản xuất số VL-0426 theo đúng phương thức 5.");
        p1.setCreatedBy("hongphong");
        p1.setCreatedAt(new Date(System.currentTimeMillis() - 86400000L * 15));
        p1.setUpdatedAt(new Date());
        AuditProject savedP1 = auditProjectRepository.save(p1);

        // Samples for Project 1
        AuditSample s1 = new AuditSample();
        s1.setProjectId(savedP1.getId());
        s1.setSampleCode("SMP-VLXD-01");
        s1.setSampleName("Gạch ceramic tráng men 600x600 Viglacera Eco");
        s1.setSamplingLocation("Kho thành phẩm Lô B3");
        s1.setSamplingDate(new Date(System.currentTimeMillis() - 86400000L * 7));
        s1.setSamplerName("hongphong");
        s1.setTestingLab("Trung tâm Thử nghiệm Vật liệu Xây dựng VILAS 145");
        s1.setTestReportNumber("KQ-2026/889/VLXD");
        s1.setTestReportDate(new Date(System.currentTimeMillis() - 86400000L * 2));
        s1.setTestResult("PASS");
        s1.setNotes("Chỉ tiêu độ hút nước E <= 3.0%, độ bền uốn >= 30 N/mm2 đều ĐẠT theo QCVN 16:2023/BXD.");
        s1.setCreatedAt(new Date());
        auditSampleRepository.save(s1);

        // NCR for Project 1
        AuditNcr ncr1 = new AuditNcr();
        ncr1.setProjectId(savedP1.getId());
        ncr1.setNcrCode("NCR-01");
        ncr1.setSeverity("MINOR");
        ncr1.setStandardClause("Điều 2.1 QCVN 16:2023/BXD & ISO 9001 Điều 7.1.5");
        ncr1.setDescription("Thiết bị đo nhiệt độ lò nung số 2 chưa dán tem kiểm định/hiệu chuẩn định kỳ.");
        ncr1.setEvidence("Ảnh chụp tại trạm nhiệt lò nung dây chuyền 2, tem cũ hết hạn tháng 03/2026.");
        cal.setTime(new Date());
        cal.add(Calendar.DAY_OF_YEAR, 15);
        ncr1.setDeadline(cal.getTime());
        ncr1.setStatus("SUBMITTED");
        ncr1.setRootCause("Nhân viên phụ trách bảo trì thiết bị nghỉ ốm, chưa bàn giao kịp lịch hiệu chuẩn.");
        ncr1.setCorrectiveAction("Đã liên hệ Trung tâm Kỹ thuật Tiêu chuẩn Đo lường Chất lượng 1 đến hiệu chuẩn và cấp tem mới.");
        ncr1.setCreatedBy("hongphong");
        ncr1.setCreatedAt(new Date(System.currentTimeMillis() - 86400000L * 5));
        auditNcrRepository.save(ncr1);

        // Project 2: ISO 9001:2015 Hệ thống Quản lý Chất lượng - ĐÃ CẤP GCN
        AuditProject p2 = new AuditProject();
        p2.setProjectCode("PRJ-2026-002");
        p2.setCompanyName("Công ty Cổ phần Nhựa Tiền Phong");
        p2.setTaxCode("0200114889");
        p2.setAddress("Số 2 An Đà, Phường Lạch Tray, Quận Ngô Quyền, TP Hải Phòng");
        p2.setContactPerson("Bà Trần Thị Bích (Trưởng ban ISO)");
        p2.setContactPhone("0988776655");
        p2.setContactEmail("bichtran@nhuatienphong.vn");
        p2.setStandard("ISO 9001:2015");
        p2.setAuditType("SURVEILLANCE_1");
        p2.setCertificationScope("Thiết kế, sản xuất và cung ứng các loại ống và phụ tùng nối ống nhựa uPVC, HDPE, PP-R");
        p2.setLeadAuditor("hongphong");
        p2.setTechnicalReviewer("emmadirector");
        p2.setStatus("CERTIFIED");
        p2.setDeskReviewNotes("Hệ thống tài liệu duy trì và cập nhật tốt.");
        p2.setOnSiteAuditSummary("Đánh giá giám sát năm thứ 1 đạt yêu cầu, không có điểm không phù hợp nặng.");
        p2.setTechnicalReviewNotes("Hồ sơ đầy đủ, thẩm xét đạt yêu cầu duy trì hiệu lực chứng nhận.");
        p2.setCreatedBy("hongphong");
        p2.setCreatedAt(new Date(System.currentTimeMillis() - 86400000L * 30));
        p2.setUpdatedAt(new Date());
        AuditProject savedP2 = auditProjectRepository.save(p2);

        // Certificate for Project 2
        Certificate cert2 = new Certificate();
        cert2.setProjectId(savedP2.getId());
        cert2.setCertificateNumber("VIC.ISO9001.2026.0028");
        cert2.setCompanyName(savedP2.getCompanyName());
        cert2.setTaxCode(savedP2.getTaxCode());
        cert2.setAddress(savedP2.getAddress());
        cert2.setStandard(savedP2.getStandard());
        cert2.setScope(savedP2.getCertificationScope());
        Date certDate = new Date(System.currentTimeMillis() - 86400000L * 25);
        cert2.setIssueDate(certDate);
        cal.setTime(certDate);
        cal.add(Calendar.YEAR, 3);
        cert2.setExpiryDate(cal.getTime());
        cal.setTime(certDate);
        cal.add(Calendar.MONTH, 12);
        cert2.setSurveillance1Date(cal.getTime());
        cal.setTime(certDate);
        cal.add(Calendar.MONTH, 24);
        cert2.setSurveillance2Date(cal.getTime());
        cert2.setQrVerificationCode("VIC-NTP-9001-QR88");
        cert2.setStatus("VALID");
        cert2.setApprovedBy("emmadirector");
        cert2.setApprovedAt(certDate);
        certificateRepository.save(cert2);
    }
}
