package com.base.delegate;

import com.base.model.DmsDoc;
import com.base.model.DmsFile;
import com.base.repo.DmsDocRepository;
import com.base.service.GeminiClientService;
import com.base.service.impl.MinioService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component("retrieveDocumentAmountDelegate")
public class RetrieveDocumentAmountDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(RetrieveDocumentAmountDelegate.class);

    @Autowired
    private DmsDocRepository dmsDocRepository;

    @Autowired
    private MinioService minioService;

    @Autowired
    private GeminiClientService geminiClientService;

    @Autowired
    private com.base.service.DmsDocHistoryService dmsDocHistoryService;

    @Value("${ocr.url}")
    private String ocrUrl;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String processInstanceId = execution.getProcessInstanceId();
        log.info("Executing retrieveDocumentAmountDelegate for processInstanceId: {}", processInstanceId);

        DmsDoc doc = dmsDocRepository.findByProcessInstanceId(processInstanceId);
        if (doc == null) {
            log.warn("No document found for processInstanceId: {}", processInstanceId);
            execution.setVariable("amount", 0.0);
            return;
        }

        List<DmsFile> files = doc.getFiles();
        if (files == null || files.isEmpty()) {
            log.warn("No files associated with document {}", doc.getDocId());
            execution.setVariable("amount", 0.0);
            return;
        }

        // Read the first file associated with the document (usually the contract PDF)
        DmsFile file = files.get(0);
        String objectKey = file.getObjectKey();
        log.info("Reading file objectKey: {}", objectKey);

        byte[] fileBytes;
        try (InputStream is = minioService.download(objectKey)) {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            fileBytes = baos.toByteArray();
        }

        // Call the external OCR Service
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        ByteArrayResource fileResource = new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() {
                return file.getFileName() != null ? file.getFileName() : "document.pdf";
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
        if (!textContent.trim().isEmpty()) {
            String prompt = "Dưới đây là nội dung của một văn bản tài liệu/hợp đồng. Hãy phân tích và trích xuất ra duy nhất một con số thể hiện TỔNG GIÁ TRỊ HỢP ĐỒNG hoặc SỐ TIỀN THANH TOÁN (đơn vị: VNĐ). Chỉ trả về duy nhất một con số (ví dụ: 150000000 hoặc 50000000), không thêm bất kỳ ký tự chữ hay dấu nào khác. Nếu không tìm thấy số tiền hoặc không phân tích được, hãy trả về 0. \n\nNội dung tài liệu:\n" + textContent;
            
            log.info("Sending extracted text to Gemini...");
            com.base.model.Res.BaseResponse aiRes = geminiClientService.generate(prompt);
            if (aiRes != null && aiRes.getData() != null) {
                com.base.model.Res.MessageRes msg = (com.base.model.Res.MessageRes) aiRes.getData();
                String reply = msg.getMessage();
                log.info("Received reply from Gemini: {}", reply);
                String digitsOnly = reply.replaceAll("[^0-9]", ""); // Keep only digits
                if (!digitsOnly.isEmpty()) {
                    amount = Double.parseDouble(digitsOnly);
                }
            }
        }

        log.info("Extracted Document Amount: {} VNĐ", amount);
        doc.setAmount(amount);

        if (amount <= 10000000.0) {
            // If amount <= 10,000,000, it goes to PENDING_MANAGER_APPROVAL and ends there
            doc.setStatus(com.base.Enum.StatusEnum.PENDING_MANAGER_APPROVAL.toString());
            dmsDocRepository.save(doc);
            try {
                dmsDocHistoryService.log(doc.getId(), "system", "Tự động chuyển duyệt Quản lý (Số tiền <= 10,000,000 VNĐ)", com.base.Enum.StatusEnum.PENDING_MANAGER_APPROVAL.toString());
            } catch (Exception e) {
                log.error("Failed to log history: {}", e.getMessage());
            }
        } else {
            // If amount > 10,000,000, it goes to PENDING_DIRECTOR_APPROVAL (skip manager and go straight to director)
            doc.setStatus(com.base.Enum.StatusEnum.PENDING_DIRECTOR_APPROVAL.toString());
            dmsDocRepository.save(doc);
            try {
                dmsDocHistoryService.log(doc.getId(), "system", "Tự động chuyển duyệt Giám đốc (Số tiền > 10,000,000 VNĐ)", com.base.Enum.StatusEnum.PENDING_DIRECTOR_APPROVAL.toString());
            } catch (Exception e) {
                log.error("Failed to log history: {}", e.getMessage());
            }
        }

        execution.setVariable("amount", amount);
    }
}
