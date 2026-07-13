package com.base.controller;

import com.base.model.Res.BaseResponse;
import com.base.service.DmsDocService;
import com.base.service.GeminiClientService;
import com.base.service.DocumentTemplateService;
import com.base.service.DmsDocHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController()
@RequestMapping("/doc")
public class DmsDocController {
    private static final Logger log = LoggerFactory.getLogger(DmsDocController.class);

    @Autowired
    private DmsDocService service;

    @Autowired
    private DmsDocHistoryService dmsDocHistoryService;

    @Autowired
    private GeminiClientService geminiClientService;

    @Autowired
    private DocumentTemplateService documentTemplateService;

    @Value("${ocr.url}")
    private String ocrUrl;

    @GetMapping("/fetchDoc")
    public ResponseEntity<BaseResponse> startProcess() {
        return ResponseEntity.ok(service.fetchDoc());
    }

    @GetMapping("/{id}/timeline")
    public ResponseEntity<BaseResponse> getDocumentTimeline(@PathVariable("id") UUID id) {
        try {
            List<com.base.model.DmsDocHistory> timeline = dmsDocHistoryService.getTimeline(id);
            return ResponseEntity.ok(new BaseResponse(200, timeline, "success"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new BaseResponse(500, null, e.getMessage()));
        }
    }

    @GetMapping("/templates")
    public ResponseEntity<BaseResponse> getAvailableTemplates() {
        try {
            List<String> templates = documentTemplateService.getAvailableTemplates();
            return ResponseEntity.ok(new BaseResponse(200, templates, "success"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new BaseResponse(500, null, e.getMessage()));
        }
    }

    @PostMapping("/templates/generate")
    public ResponseEntity<?> generateDocFromTemplate(@RequestBody Map<String, Object> requestBody) {
        try {
            String docType = (String) requestBody.get("docType");
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) requestBody.get("data");

            if (docType == null || data == null) {
                return ResponseEntity.badRequest().body(new BaseResponse(400, null, "docType and data are required"));
            }

            byte[] docBytes = documentTemplateService.generateDocument(docType, data);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", docType.toUpperCase() + "_rendered.docx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(docBytes);
        } catch (Exception e) {
            log.error("Error generating document from template: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(new BaseResponse(500, null, e.getMessage()));
        }
    }

    @PostMapping(value = "/check-amount", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponse> checkAmount(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                log.warn("checkAmount failed: Empty or null file uploaded.");
                return ResponseEntity.badRequest().body(new BaseResponse(400, null, "File is required"));
            }

            String fileName = file.getOriginalFilename();
            long fileSize = file.getSize();
            log.info("Received request to check amount via OCR API parsing. File: {}, size: {} bytes", fileName, fileSize);

            byte[] fileBytes = file.getBytes();

            // Call the external OCR Service
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource fileResource = new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return fileName != null ? fileName : "document.pdf";
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

            log.info("Calling OCR API at URL: {}", requestUrl);
            com.fasterxml.jackson.databind.JsonNode ocrResponse = restTemplate.postForObject(requestUrl, requestEntity, com.fasterxml.jackson.databind.JsonNode.class);

            String textContent = "";
            if (ocrResponse != null && ocrResponse.has("content")) {
                textContent = ocrResponse.get("content").asText();
                log.info("OCR API successfully extracted text. Length: {} characters", textContent.length());
            } else {
                log.warn("OCR API response does not contain 'content' field: {}", ocrResponse);
            }

            if (textContent.length() > 8000) {
                textContent = textContent.substring(0, 8000);
            }

            double amount = 0.0;
            String reply = "";
            if (!textContent.trim().isEmpty()) {
                String prompt = "Dưới đây là nội dung của một văn bản tài liệu/hợp đồng. Hãy phân tích và trích xuất ra duy nhất một con số thể hiện TỔNG GIÁ TRỊ HỢP ĐỒNG hoặc SỐ TIỀN THANH TOÁN (đơn vị: VNĐ). Chỉ trả về duy nhất một con số (ví dụ: 150000000 hoặc 50000000), không thêm bất kỳ ký tự chữ hay dấu nào khác. Nếu không tìm thấy số tiền hoặc không phân tích được, hãy trả về 0. \n\nNội dung tài liệu:\n" + textContent;
                
                log.info("Sending extracted text to Gemini...");
                BaseResponse aiRes = geminiClientService.generate(prompt);
                if (aiRes != null && aiRes.getData() != null) {
                    com.base.model.Res.MessageRes msg = (com.base.model.Res.MessageRes) aiRes.getData();
                    reply = msg.getMessage();
                    log.info("Received reply from Gemini: {}", reply);
                    String digitsOnly = reply.replaceAll("[^0-9]", ""); // Keep only digits
                    if (!digitsOnly.isEmpty()) {
                        amount = Double.parseDouble(digitsOnly);
                    }
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("amount", amount);
            result.put("fileName", fileName);
            result.put("extractedTextLength", textContent.length());
            result.put("geminiReply", reply);

            log.info("Amount extraction finished. Result amount: {} VNĐ", amount);
            return ResponseEntity.ok(new BaseResponse(200, result, "success"));
        } catch (Exception e) {
            log.error("Exception occurred during check-amount processing: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(new BaseResponse(500, null, "Error extracting amount: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/{id}/re-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponse> reUploadDocumentFile(
            @PathVariable("id") UUID id,
            @RequestParam("file") MultipartFile file
    ) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(new BaseResponse(400, null, "File is required"));
            }
            BaseResponse res = service.reUploadFile(id, file);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            log.error("Error re-uploading file: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(new BaseResponse(500, null, e.getMessage()));
        }
    }
}
