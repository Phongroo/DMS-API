package com.base.controller;

import com.base.model.DmsDoc;
import com.base.model.Position;
import com.base.model.Role;
import com.base.model.SystemSettings;
import com.base.model.User;
import com.base.model.Branch;
import com.base.model.UserGroup;
import com.base.model.Req.AdminUserReq;
import com.base.model.Res.BaseResponse;
import com.base.service.AdminService;
import com.base.service.AuditLogService;
import com.base.service.SystemSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private SystemSettingsService systemSettingsService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private com.base.repo.BranchRepository branchRepository;

    @Autowired
    private com.base.repo.UserGroupRepository userGroupRepository;

    // --- stats ---
    @GetMapping("/stats")
    public ResponseEntity<BaseResponse> getStatsSummary() {
        try {
            Map<String, Object> stats = adminService.getStatsSummary();
            return ResponseEntity.ok(new BaseResponse(200, stats, "success"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new BaseResponse(500, null, e.getMessage()));
        }
    }

    // --- user management ---
    @GetMapping("/users")
    public ResponseEntity<BaseResponse> searchUsers(@RequestParam(value = "query", required = false) String query) {
        try {
            List<User> users = adminService.searchUsers(query);
            return ResponseEntity.ok(new BaseResponse(200, users, "success"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new BaseResponse(500, null, e.getMessage()));
        }
    }

    @PostMapping("/users")
    public ResponseEntity<BaseResponse> createUser(@RequestBody AdminUserReq req) {
        try {
            User created = adminService.createUser(req.getUser(), req.getRoleName(), req.getPositionName());
            return ResponseEntity.ok(new BaseResponse(200, created, "success"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(new BaseResponse(400, null, e.getMessage()));
        }
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<BaseResponse> updateUser(@PathVariable("id") Long id, @RequestBody AdminUserReq req) {
        try {
            User updated = adminService.updateUser(id, req.getUser(), req.getRoleName(), req.getPositionName());
            return ResponseEntity.ok(new BaseResponse(200, updated, "success"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(new BaseResponse(400, null, e.getMessage()));
        }
    }

    @PutMapping("/users/{id}/toggle-status")
    public ResponseEntity<BaseResponse> toggleUserStatus(@PathVariable("id") Long id) {
        try {
            User updated = adminService.toggleUserStatus(id);
            if (updated != null) {
                return ResponseEntity.ok(new BaseResponse(200, updated, "success"));
            }
            return ResponseEntity.status(404).body(new BaseResponse(404, null, "User not found"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new BaseResponse(500, null, e.getMessage()));
        }
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<BaseResponse> deleteUser(@PathVariable("id") Long id) {
        try {
            adminService.deleteUser(id);
            return ResponseEntity.ok(new BaseResponse(200, null, "success"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new BaseResponse(500, null, e.getMessage()));
        }
    }

    @GetMapping("/roles")
    public ResponseEntity<BaseResponse> getAllRoles() {
        try {
            List<Role> roles = adminService.getAllRoles();
            return ResponseEntity.ok(new BaseResponse(200, roles, "success"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new BaseResponse(500, null, e.getMessage()));
        }
    }

    @GetMapping("/positions")
    public ResponseEntity<BaseResponse> getAllPositions() {
        try {
            List<Position> positions = adminService.getAllPositions();
            return ResponseEntity.ok(new BaseResponse(200, positions, "success"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new BaseResponse(500, null, e.getMessage()));
        }
    }

    // --- document catalog ---
    @GetMapping("/docs")
    public ResponseEntity<BaseResponse> getAllDocs(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "docType", required = false) String docType) {
        try {
            List<DmsDoc> docs = adminService.getAllDocs(status, docType);
            return ResponseEntity.ok(new BaseResponse(200, docs, "success"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new BaseResponse(500, null, e.getMessage()));
        }
    }

    @PutMapping("/docs/{id}/security-level")
    public ResponseEntity<BaseResponse> updateSecurityLevel(@PathVariable("id") UUID id, @RequestBody Map<String, String> body) {
        try {
            String securityLevel = body.get("securityLevel");
            if (securityLevel == null) {
                return ResponseEntity.status(400).body(new BaseResponse(400, null, "securityLevel is required"));
            }
            DmsDoc updated = adminService.updateSecurityLevel(id, securityLevel);
            if (updated != null) {
                return ResponseEntity.ok(new BaseResponse(200, updated, "success"));
            }
            return ResponseEntity.status(404).body(new BaseResponse(404, null, "Document not found"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new BaseResponse(500, null, e.getMessage()));
        }
    }

    @PutMapping("/docs/{id}/access-control")
    public ResponseEntity<BaseResponse> updateAccessControl(@PathVariable("id") UUID id, @RequestBody Map<String, String> body) {
        try {
            String securityLevel = body.get("securityLevel");
            String allowedRole = body.get("allowedRole");
            if (allowedRole == null) {
                allowedRole = body.get("allowedRoles");
            }
            String allowedPositions = body.get("allowedPositions");
            if (securityLevel == null) {
                return ResponseEntity.status(400).body(new BaseResponse(400, null, "securityLevel is required"));
            }
            DmsDoc updated = adminService.updateAccessControl(id, securityLevel, allowedRole, allowedPositions);
            if (updated != null) {
                return ResponseEntity.ok(new BaseResponse(200, updated, "success"));
            }
            return ResponseEntity.status(404).body(new BaseResponse(404, null, "Document not found"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new BaseResponse(500, null, e.getMessage()));
        }
    }

    @DeleteMapping("/docs/{id}")
    public ResponseEntity<BaseResponse> deleteDoc(@PathVariable("id") UUID id) {
        try {
            adminService.deleteDoc(id);
            return ResponseEntity.ok(new BaseResponse(200, null, "success"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new BaseResponse(500, null, e.getMessage()));
        }
    }

    // --- system settings ---
    @GetMapping("/settings")
    public ResponseEntity<BaseResponse> getSettings() {
        try {
            SystemSettings settings = systemSettingsService.getSettings();
            return ResponseEntity.ok(new BaseResponse(200, settings, "success"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new BaseResponse(500, null, e.getMessage()));
        }
    }

    @PutMapping("/settings")
    public ResponseEntity<BaseResponse> updateSettings(@RequestBody SystemSettings settings) {
        try {
            SystemSettings updated = systemSettingsService.updateSettings(settings);
            auditLogService.log("Cập nhật cấu hình tham số hệ thống", "INFO");
            return ResponseEntity.ok(new BaseResponse(200, updated, "success"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(new BaseResponse(400, null, e.getMessage()));
        }
    }

    // --- audit logs ---
    @GetMapping("/audit-trail")
    public ResponseEntity<BaseResponse> getAuditLogs() {
        try {
            return ResponseEntity.ok(new BaseResponse(200, auditLogService.getAllLogs(), "success"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new BaseResponse(500, null, e.getMessage()));
        }
    }

    @GetMapping(value = "/audit-trail/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAuditLogs() {
        return auditLogService.subscribe();
    }

    // --- Branch Management ---
    @GetMapping("/branches")
    public ResponseEntity<BaseResponse> getAllBranches() {
        try {
            return ResponseEntity.ok(new BaseResponse(200, branchRepository.findAll(), "success"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new BaseResponse(500, null, e.getMessage()));
        }
    }

    @PostMapping("/branches")
    public ResponseEntity<BaseResponse> createBranch(@RequestBody Branch branch) {
        try {
            Branch saved = branchRepository.save(branch);
            auditLogService.log("Thêm chi nhánh mới: " + branch.getName(), "INFO");
            return ResponseEntity.ok(new BaseResponse(200, saved, "success"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(new BaseResponse(400, null, e.getMessage()));
        }
    }

    @PutMapping("/branches/{id}")
    public ResponseEntity<BaseResponse> updateBranch(@PathVariable("id") Long id, @RequestBody Branch details) {
        try {
            Branch branch = branchRepository.findById(id).orElseThrow(() -> new Exception("Branch not found"));
            branch.setCode(details.getCode());
            branch.setName(details.getName());
            branch.setAddress(details.getAddress());
            branch.setPhone(details.getPhone());
            Branch updated = branchRepository.save(branch);
            auditLogService.log("Cập nhật chi nhánh: " + branch.getName(), "INFO");
            return ResponseEntity.ok(new BaseResponse(200, updated, "success"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(new BaseResponse(400, null, e.getMessage()));
        }
    }

    @DeleteMapping("/branches/{id}")
    public ResponseEntity<BaseResponse> deleteBranch(@PathVariable("id") Long id) {
        try {
            Branch branch = branchRepository.findById(id).orElseThrow(() -> new Exception("Branch not found"));
            branchRepository.deleteById(id);
            auditLogService.log("Xóa chi nhánh: " + branch.getName(), "WARN");
            return ResponseEntity.ok(new BaseResponse(200, null, "success"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new BaseResponse(500, null, e.getMessage()));
        }
    }

    // --- UserGroup Management ---
    @GetMapping("/groups")
    public ResponseEntity<BaseResponse> getAllGroups() {
        try {
            return ResponseEntity.ok(new BaseResponse(200, userGroupRepository.findAll(), "success"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new BaseResponse(500, null, e.getMessage()));
        }
    }

    @PostMapping("/groups")
    public ResponseEntity<BaseResponse> createGroup(@RequestBody UserGroup group) {
        try {
            UserGroup saved = userGroupRepository.save(group);
            auditLogService.log("Thêm nhóm quản lý mới: " + group.getName(), "INFO");
            return ResponseEntity.ok(new BaseResponse(200, saved, "success"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(new BaseResponse(400, null, e.getMessage()));
        }
    }

    @PutMapping("/groups/{id}")
    public ResponseEntity<BaseResponse> updateGroup(@PathVariable("id") Long id, @RequestBody UserGroup details) {
        try {
            UserGroup group = userGroupRepository.findById(id).orElseThrow(() -> new Exception("Group not found"));
            group.setName(details.getName());
            group.setDescription(details.getDescription());
            group.setRoles(details.getRoles());
            group.setPermissions(details.getPermissions());
            UserGroup updated = userGroupRepository.save(group);
            auditLogService.log("Cập nhật nhóm quản lý: " + group.getName(), "INFO");
            return ResponseEntity.ok(new BaseResponse(200, updated, "success"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(new BaseResponse(400, null, e.getMessage()));
        }
    }

    @DeleteMapping("/groups/{id}")
    public ResponseEntity<BaseResponse> deleteGroup(@PathVariable("id") Long id) {
        try {
            UserGroup group = userGroupRepository.findById(id).orElseThrow(() -> new Exception("Group not found"));
            userGroupRepository.deleteById(id);
            auditLogService.log("Xóa nhóm quản lý: " + group.getName(), "WARN");
            return ResponseEntity.ok(new BaseResponse(200, null, "success"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new BaseResponse(500, null, e.getMessage()));
        }
    }
}
