package com.base.controller;

import com.base.model.AuditNcr;
import com.base.model.AuditProject;
import com.base.model.AuditSample;
import com.base.model.Certificate;
import com.base.model.Res.BaseResponse;
import com.base.service.CertificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Date;
import java.util.List;
import java.util.Map;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/certification")
public class CertificationController {

    @Autowired
    private CertificationService certificationService;

    // --- Dashboard Stats ---
    @GetMapping("/stats")
    public ResponseEntity<BaseResponse> getStats() {
        try {
            Map<String, Object> stats = certificationService.getDashboardStats();
            return ResponseEntity.ok(new BaseResponse(200, stats, "Lấy thống kê thành công"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new BaseResponse(500, null, e.getMessage()));
        }
    }

    // --- Projects ---
    @GetMapping("/projects")
    public ResponseEntity<BaseResponse> getAllProjects() {
        try {
            List<AuditProject> projects = certificationService.getAllProjects();
            return ResponseEntity.ok(new BaseResponse(200, projects, "success"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new BaseResponse(500, null, e.getMessage()));
        }
    }

    @GetMapping("/projects/{id}")
    public ResponseEntity<BaseResponse> getProjectById(@PathVariable("id") Long id) {
        try {
            AuditProject project = certificationService.getProjectById(id);
            if (project == null) {
                return ResponseEntity.status(404).body(new BaseResponse(404, null, "Không tìm thấy dự án"));
            }
            return ResponseEntity.ok(new BaseResponse(200, project, "success"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new BaseResponse(500, null, e.getMessage()));
        }
    }

    @GetMapping("/projects/{id}/bpmn-viewer")
    public ResponseEntity<BaseResponse> getProjectBpmnViewer(@PathVariable("id") Long id) {
        try {
            Map<String, Object> data = certificationService.getProjectBpmnViewer(id);
            return ResponseEntity.ok(new BaseResponse(200, data, "Lấy sơ đồ Camunda thành công"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new BaseResponse(500, null, e.getMessage()));
        }
    }

    @PostMapping("/projects")
    public ResponseEntity<BaseResponse> createProject(@RequestBody AuditProject project, Principal principal) {
        try {
            String username = principal != null ? principal.getName() : "system";
            AuditProject created = certificationService.createProject(project, username);
            return ResponseEntity.ok(new BaseResponse(200, created, "Khởi tạo dự án chứng nhận thành công"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(new BaseResponse(400, null, e.getMessage()));
        }
    }

    @PutMapping("/projects/{id}")
    public ResponseEntity<BaseResponse> updateProject(@PathVariable("id") Long id, @RequestBody AuditProject project) {
        try {
            AuditProject updated = certificationService.updateProject(id, project);
            if (updated == null) {
                return ResponseEntity.status(404).body(new BaseResponse(404, null, "Không tìm thấy dự án"));
            }
            return ResponseEntity.ok(new BaseResponse(200, updated, "Cập nhật dự án thành công"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(new BaseResponse(400, null, e.getMessage()));
        }
    }

    @PutMapping("/projects/{id}/status")
    public ResponseEntity<BaseResponse> updateStatus(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> body,
            Principal principal) {
        try {
            String status = body.get("status");
            String note = body.get("note");
            String username = principal != null ? principal.getName() : "user";
            AuditProject updated = certificationService.updateProjectStatus(id, status, note, username);
            return ResponseEntity.ok(new BaseResponse(200, updated, "Chuyển bước quy trình thành công"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(new BaseResponse(400, null, e.getMessage()));
        }
    }

    // --- NCR & CAPA ---
    @GetMapping("/projects/{id}/ncrs")
    public ResponseEntity<BaseResponse> getNcrs(@PathVariable("id") Long id) {
        try {
            List<AuditNcr> ncrs = certificationService.getNcrsByProjectId(id);
            return ResponseEntity.ok(new BaseResponse(200, ncrs, "success"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new BaseResponse(500, null, e.getMessage()));
        }
    }

    @PostMapping("/projects/{id}/ncrs")
    public ResponseEntity<BaseResponse> createNcr(@PathVariable("id") Long id, @RequestBody AuditNcr ncr, Principal principal) {
        try {
            String username = principal != null ? principal.getName() : "auditor";
            AuditNcr created = certificationService.createNcr(id, ncr, username);
            return ResponseEntity.ok(new BaseResponse(200, created, "Lập biên bản NCR thành công"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(new BaseResponse(400, null, e.getMessage()));
        }
    }

    @PostMapping("/ncrs/{ncrId}/submit-capa")
    public ResponseEntity<BaseResponse> submitCapa(@PathVariable("ncrId") Long ncrId, @RequestBody Map<String, String> body) {
        try {
            String rootCause = body.get("rootCause");
            String correctiveAction = body.get("correctiveAction");
            String evidenceUrl = body.get("correctiveEvidenceUrl");
            AuditNcr updated = certificationService.submitCapa(ncrId, rootCause, correctiveAction, evidenceUrl);
            return ResponseEntity.ok(new BaseResponse(200, updated, "Đã nộp hành động khắc phục CAPA"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(new BaseResponse(400, null, e.getMessage()));
        }
    }

    @PostMapping("/ncrs/{ncrId}/review-capa")
    public ResponseEntity<BaseResponse> reviewCapa(@PathVariable("ncrId") Long ncrId, @RequestBody Map<String, Object> body, Principal principal) {
        try {
            boolean approve = Boolean.TRUE.equals(body.get("approve"));
            String username = principal != null ? principal.getName() : "lead_auditor";
            AuditNcr updated = certificationService.reviewCapa(ncrId, approve, username);
            return ResponseEntity.ok(new BaseResponse(200, updated, approve ? "Đã duyệt đóng lỗi NCR thành công" : "Đã từ chối biện pháp khắc phục"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(new BaseResponse(400, null, e.getMessage()));
        }
    }

    // --- Samples & Test Reports ---
    @GetMapping("/projects/{id}/samples")
    public ResponseEntity<BaseResponse> getSamples(@PathVariable("id") Long id) {
        try {
            List<AuditSample> samples = certificationService.getSamplesByProjectId(id);
            return ResponseEntity.ok(new BaseResponse(200, samples, "success"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new BaseResponse(500, null, e.getMessage()));
        }
    }

    @PostMapping("/projects/{id}/samples")
    public ResponseEntity<BaseResponse> createSample(@PathVariable("id") Long id, @RequestBody AuditSample sample) {
        try {
            AuditSample created = certificationService.createSample(id, sample);
            return ResponseEntity.ok(new BaseResponse(200, created, "Lập biên bản lấy mẫu thành công"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(new BaseResponse(400, null, e.getMessage()));
        }
    }

    @PutMapping("/samples/{sampleId}")
    public ResponseEntity<BaseResponse> updateSampleResult(@PathVariable("sampleId") Long sampleId, @RequestBody Map<String, Object> body) {
        try {
            String testResult = (String) body.get("testResult");
            String testReportNumber = (String) body.get("testReportNumber");
            String testReportFileUrl = (String) body.get("testReportFileUrl");
            String notes = (String) body.get("notes");
            AuditSample updated = certificationService.updateSampleResult(sampleId, testResult, testReportNumber, new Date(), testReportFileUrl, notes);
            return ResponseEntity.ok(new BaseResponse(200, updated, "Cập nhật kết quả thử nghiệm thành công"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(new BaseResponse(400, null, e.getMessage()));
        }
    }

    // --- Certificates ---
    @PostMapping("/projects/{id}/issue-certificate")
    public ResponseEntity<BaseResponse> issueCertificate(@PathVariable("id") Long id, Principal principal) {
        try {
            String director = principal != null ? principal.getName() : "cert_director";
            Certificate cert = certificationService.issueCertificate(id, director);
            return ResponseEntity.ok(new BaseResponse(200, cert, "Phê duyệt và phát hành Giấy chứng nhận thành công"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(new BaseResponse(400, null, e.getMessage()));
        }
    }

    @GetMapping("/projects/{id}/certificate")
    public ResponseEntity<BaseResponse> getCertificate(@PathVariable("id") Long id) {
        try {
            Certificate cert = certificationService.getCertificateByProjectId(id);
            return ResponseEntity.ok(new BaseResponse(200, cert, "success"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new BaseResponse(500, null, e.getMessage()));
        }
    }

    // --- PUBLIC VERIFICATION API (No Authentication Required) ---
    @GetMapping("/public/verify")
    public ResponseEntity<BaseResponse> verifyCertificatePublic(@RequestParam("query") String query) {
        try {
            Certificate cert = certificationService.verifyCertificate(query);
            if (cert == null) {
                return ResponseEntity.status(404).body(new BaseResponse(404, null, "Không tìm thấy Giấy chứng nhận hợp lệ trong hệ thống"));
            }
            return ResponseEntity.ok(new BaseResponse(200, cert, "Xác thực Giấy chứng nhận thành công"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new BaseResponse(500, null, e.getMessage()));
        }
    }
}
