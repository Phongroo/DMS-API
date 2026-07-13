package com.base.service.impl;

import com.base.model.ActionButton;
import com.base.model.Authority;
import com.base.model.DmsDoc;
import com.base.model.ProcessActionConfig;
import com.base.model.Req.WorkflowContext;
import com.base.model.Res.BaseResponse;
import com.base.model.Res.TaskResponse;
import com.base.repo.DmsDocRepository;
import com.base.service.DmsDocService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.core.io.ByteArrayResource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Objects;

@Service
public class DmsDocServiceImpl implements DmsDocService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DmsDocServiceImpl.class);

    @Autowired
    private DmsDocRepository dmsDocRepository;
    @Autowired
    private com.base.repo.UserRepository userRepository;

    @Autowired
    private com.base.repo.DmsFileRepository dmsFileRepository;

    @Autowired
    private com.base.service.DmsFileService dmsFileService;

    @Autowired
    private com.base.service.GeminiClientService geminiClientService;

    @Autowired
    private com.base.service.DmsDocHistoryService dmsDocHistoryService;

    @Value("${ocr.url}")
    private String ocrUrl;
    @Override
    public BaseResponse fetchDoc() {
        try {
            Authentication auth =
                    SecurityContextHolder.getContext()
                            .getAuthentication();

            String position = auth.getAuthorities()
                    .stream()
                    .filter(a -> a instanceof Authority)
                    .map(a -> ((Authority) a).getPosition())
                    .findFirst()
                    .orElse(null);

            List<TaskResponse> taskResponses = new ArrayList<>();
            List<DmsDoc> dmsDoc = dmsDocRepository.findAll();
            dmsDoc.forEach(x->{
                if (!hasAccess(x)) {
                    return; // Skip if user has no access to this document
                }
                TaskResponse task = new TaskResponse();
                task.setId(x.getId());
                task.setProcessInstanceId(x.getProcessInstanceId());
                task.setDocId(x.getDocId());
                task.setCreatedDate(x.getCreatedDate());
                task.setStatus(x.getStatus());
                task.setSecurityLevel(x.getSecurityLevel());
                task.setAllowedRole(x.getAllowedRole());
                task.setAllowedPositions(x.getAllowedPositions());
                task.setBranchId(x.getBranchId());
                task.setDocType(x.getDocType());
                task.setCreatorName(x.getCreatorName());
                task.setCreatedBy(x.getCreatedBy());
                if(x.getStatus() != null){
                    List<ActionButton> buttons =
                            ProcessActionConfig.getButtons(
                                     x.getStatus(),
                                     position
                            );

                    task.setActionButtons(buttons);
                    task.setDmsFiles(x.getFiles());
                }
                taskResponses.add(task);

            });


            return new BaseResponse(200, taskResponses, "success");
        }catch (Exception e){
            return new BaseResponse(200, null, e.getMessage());
        }

    }

    @Override
    public void handleTask(WorkflowContext context) {
        DmsDoc dmsDoc = context.getDmsDoc();
        dmsDoc.setStatus(context.getStatusHandle());
        dmsDocRepository.save(dmsDoc);
    }

    @Override
    public boolean hasAccess(DmsDoc doc) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        String username = auth.getName();

        // Admin always has access
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN") || a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            log.info("[Access Control] Admin bypass granted for user: {}", username);
            return true;
        }

        // Creator / Email always has access
        if (username != null && (username.equals(doc.getCreatedBy()) || username.equals(doc.getEmail()))) {
            log.info("[Access Control] Creator/Email bypass granted for user: {} on doc: {}", username, doc.getDocId());
            return true;
        }

        // --- BRANCH ACCESS CHECK ---
        com.base.model.User currentUser = userRepository.findByUsername(username);
        Long userBranchId = (currentUser != null) ? currentUser.getBranchId() : null;
        Long docBranchId = doc.getBranchId();
        
        log.info("[Access Control] Branch check - User: {} (branch: {}), Doc: {} (branch: {})", 
                 username, userBranchId, doc.getDocId(), docBranchId);

        if (!Objects.equals(userBranchId, docBranchId)) {
            log.warn("[Access Control] Blocked user: {} (branch: {}) from doc: {} (branch: {}) - Branch Mismatch", 
                     username, userBranchId, doc.getDocId(), docBranchId);
            return false; // User and Document must belong to the exact same branch
        }

        // Extract user roles and positions
        Set<String> roles = new HashSet<>();
        Set<String> positions = new HashSet<>();
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if (ga instanceof Authority) {
                Authority customAuth = (Authority) ga;
                if (customAuth.getAuthority() != null) roles.add(customAuth.getAuthority());
                if (customAuth.getPosition() != null) positions.add(customAuth.getPosition());
            } else {
                roles.add(ga.getAuthority());
            }
        }

        // --- FINE-GRAINED ACCESS CONTROL CHECKS ---
        boolean roleAllowed = true;
        if (doc.getAllowedRole() != null && !doc.getAllowedRole().trim().isEmpty()) {
            roleAllowed = roles.contains(doc.getAllowedRole().trim());
        }

        boolean positionAllowed = true;
        if (doc.getAllowedPositions() != null && !doc.getAllowedPositions().trim().isEmpty()) {
            positionAllowed = false;
            String[] allowedPositionsArr = doc.getAllowedPositions().split(",");
            for (String allowedPos : allowedPositionsArr) {
                if (positions.contains(allowedPos.trim())) {
                    positionAllowed = true;
                    break;
                }
            }
        }

        if (!roleAllowed || !positionAllowed) {
            return false;
        }

        String securityLevel = doc.getSecurityLevel();
        if (securityLevel == null) {
            securityLevel = "Nội bộ"; // Default
        }

        switch (securityLevel) {
            case "Công khai":
                return true;

            case "Nội bộ":
                // Any logged-in internal employee (having a position or valid roles)
                return !positions.isEmpty() || roles.contains("NORMAL") || roles.contains("STAFF") || roles.contains("MANAGER") || roles.contains("DIRECTOR");

            case "Mật":
                // Only MANAGER, DIRECTOR, or ADMIN
                return roles.contains("MANAGER") || roles.contains("DIRECTOR") || positions.contains("MANAGER") || positions.contains("DIRECTOR");

            case "Tối mật":
                // Only DIRECTOR or ADMIN
                return roles.contains("DIRECTOR") || positions.contains("DIRECTOR");

            default:
                return false;
        }
    }

    @Override
    @Transactional
    public BaseResponse reUploadFile(java.util.UUID docId, MultipartFile file) throws Exception {
        DmsDoc doc = dmsDocRepository.findById(docId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : "system";

        // Read bytes first to prevent closed stream exception
        final byte[] fileBytes = file.getBytes();
        final String originalFilename = file.getOriginalFilename();
        final String contentType = file.getContentType();
        final String name = file.getName();

        MultipartFile repeatableFile = new MultipartFile() {
            @Override
            public String getName() { return name; }
            @Override
            public String getOriginalFilename() { return originalFilename; }
            @Override
            public String getContentType() { return contentType; }
            @Override
            public boolean isEmpty() { return fileBytes.length == 0; }
            @Override
            public long getSize() { return fileBytes.length; }
            @Override
            public byte[] getBytes() throws java.io.IOException { return fileBytes; }
            @Override
            public java.io.InputStream getInputStream() throws java.io.IOException {
                return new java.io.ByteArrayInputStream(fileBytes);
            }
            @Override
            public void transferTo(java.io.File dest) throws java.io.IOException, IllegalStateException {
                java.nio.file.Files.write(dest.toPath(), fileBytes);
            }
        };

        // 1. Delete existing files associated with this document (both from repository and collection)
        if (doc.getFiles() != null && !doc.getFiles().isEmpty()) {
            dmsFileRepository.deleteAll(doc.getFiles());
            doc.getFiles().clear();
        }

        // 2. Save the new file using repeatable file
        com.base.model.Req.DmsFileReq dmsFileReq = new com.base.model.Req.DmsFileReq();
        dmsFileReq.setDmsDoc(doc);
        dmsFileService.save(dmsFileReq, repeatableFile);

        // 3. Re-run OCR check amount using repeatable file
        double amount = 0.0;
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource fileResource = new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return originalFilename != null ? originalFilename : "document.pdf";
                }
            };
            body.add("file", fileResource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            String requestUrl = ocrUrl;
            if (requestUrl == null || requestUrl.trim().isEmpty()) {
                requestUrl = "http://host.docker.internal:30088";
            }
            if (!requestUrl.contains("/api/ocr/upload")) {
                if (requestUrl.endsWith("/")) {
                    requestUrl += "api/ocr/upload";
                } else {
                    requestUrl += "/api/ocr/upload";
                }
            }

            com.fasterxml.jackson.databind.JsonNode ocrResponse = restTemplate.postForObject(requestUrl, requestEntity, com.fasterxml.jackson.databind.JsonNode.class);

            String textContent = "";
            if (ocrResponse != null && ocrResponse.has("content")) {
                textContent = ocrResponse.get("content").asText();
            }

            if (textContent.length() > 8000) {
                textContent = textContent.substring(0, 8000);
            }

            if (!textContent.trim().isEmpty()) {
                String prompt = "Dưới đây là nội dung của một văn bản tài liệu/hợp đồng. Hãy phân tích và trích xuất ra duy nhất một con số thể hiện TỔNG GIÁ TRỊ HỢP ĐỒNG hoặc SỐ TIỀN THANH TOÁN (đơn vị: VNĐ). Chỉ trả về duy nhất một con số (ví dụ: 150000000 hoặc 50000000), không thêm bất kỳ ký tự chữ hay dấu nào khác. Nếu không tìm thấy số tiền hoặc không phân tích được, hãy trả về 0. \n\nNội dung tài liệu:\n" + textContent;
                BaseResponse aiRes = geminiClientService.generate(prompt);
                if (aiRes != null && aiRes.getData() != null) {
                    com.base.model.Res.MessageRes msg = (com.base.model.Res.MessageRes) aiRes.getData();
                    String reply = msg.getMessage();
                    String digitsOnly = reply.replaceAll("[^0-9]", "");
                    if (!digitsOnly.isEmpty()) {
                        amount = Double.parseDouble(digitsOnly);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to run OCR for re-uploaded file", e);
        }

        // 4. Update document amount and status back to DRAFT or trigger resubmission in Camunda variables?
        if (amount > 0) {
            doc.setAmount(amount);
        }
        
        // Save doc
        dmsDocRepository.save(doc);

        // 5. Log history
        dmsDocHistoryService.log(doc.getId(), username, "Tải lên lại hồ sơ mới: " + file.getOriginalFilename(), doc.getStatus());

        return new BaseResponse(200, doc, "success");
    }
}
