package com.base.service.impl;

import com.base.model.EmailVerification;
import com.base.repo.EmailVerificationRepository;
import com.base.service.EmailVerificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import javax.mail.internet.MimeMessage;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import java.util.Random;

@Service
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationServiceImpl.class);
    private static final long EXPIRY_DURATION_MS = 5 * 60 * 1000; // 5 minutes

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired
    private com.base.repo.DmsDocRepository dmsDocRepository;

    @Autowired
    private com.base.service.impl.MinioService minioService;

    @Value("${app.callback-base-url}")
    private String callbackBaseUrl;

    private String loadEmailTemplate() {
        try (InputStream is = getClass().getResourceAsStream("/templates/email-verification.html")) {
            if (is == null) {
                log.warn("Email HTML template not found in classpath resources.");
                return null;
            }
            return new String(StreamUtils.copyToByteArray(is), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error loading email HTML template from classpath: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void sendVerificationCode(String email, String contractName, Double amount, String description, String processInstanceId, String externalTaskId) {
        // Generate a unique token/code under the hood
        String code = String.format("%06d", new Random().nextInt(1000000));
        Date expiryTime = new Date(System.currentTimeMillis() + EXPIRY_DURATION_MS);

        String displayContractName = (contractName != null && !contractName.trim().isEmpty()) ? contractName : "Tài liệu chưa xác định";
        String displayAmount = "Chưa xác định";
        if (amount != null) {
            displayAmount = String.format("%,.0f VNĐ", amount);
        }
        String displayDesc = (description != null && !description.trim().isEmpty()) ? description : "Không có mô tả chi tiết.";

        // Save the verification record
        EmailVerification verification = new EmailVerification(email, code, expiryTime, displayContractName, amount, displayDesc, processInstanceId, externalTaskId);
        emailVerificationRepository.save(verification);

        log.info("Generated validation token for email {}: {} (Expires at {})", email, code, expiryTime);
        System.out.println("==================================================");
        System.out.println("Validation Token for " + email + ": " + code);
        System.out.println("==================================================");

        // Try to fetch file to attach
        byte[] fileBytes = null;
        String attachmentFileName = null;
        String attachmentContentType = null;
        if (processInstanceId != null && dmsDocRepository != null && minioService != null) {
            try {
                com.base.model.DmsDoc doc = dmsDocRepository.findByProcessInstanceId(processInstanceId);
                if (doc != null && doc.getFiles() != null && !doc.getFiles().isEmpty()) {
                    com.base.model.DmsFile file = doc.getFiles().get(0);
                    if (file.getObjectKey() != null) {
                        try (java.io.InputStream is = minioService.download(file.getObjectKey())) {
                            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = is.read(buffer)) != -1) {
                                baos.write(buffer, 0, bytesRead);
                            }
                            fileBytes = baos.toByteArray();
                            attachmentFileName = file.getFileName();
                            attachmentContentType = file.getContentType();
                            log.info("Successfully loaded attachment file from MinIO for email. FileName: {}, Size: {} bytes", attachmentFileName, fileBytes.length);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Failed to retrieve document file for email attachment: {}", e.getMessage(), e);
            }
        }

        // Send email
        if (mailSender != null) {
            try {
                String encodedEmail = URLEncoder.encode(email, "UTF-8");
                String callbackAgreeUrl = callbackBaseUrl + "/api/verify/callback?email=" + encodedEmail + "&code=" + code + "&decision=AGREE";
                String callbackRejectUrl = callbackBaseUrl + "/api/verify/callback?email=" + encodedEmail + "&code=" + code + "&decision=REJECT";

                String htmlTemplate = loadEmailTemplate();
                if (htmlTemplate != null) {
                    String htmlContent = htmlTemplate
                            .replace("[[contractName]]", displayContractName)
                            .replace("[[amount]]", displayAmount)
                            .replace("[[description]]", displayDesc)
                            .replace("[[callbackAgreeUrl]]", callbackAgreeUrl)
                            .replace("[[callbackRejectUrl]]", callbackRejectUrl);

                    MimeMessage mimeMessage = mailSender.createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
                    helper.setTo(email);
                    helper.setSubject("Xác nhận thông tin tài liệu - Hệ thống DMS");
                    helper.setText(htmlContent, true);

                    if (fileBytes != null) {
                        String finalName = (attachmentFileName != null && !attachmentFileName.trim().isEmpty()) ? attachmentFileName : "document.pdf";
                        String finalType = (attachmentContentType != null && !attachmentContentType.trim().isEmpty()) ? attachmentContentType : "application/pdf";
                        helper.addAttachment(finalName, new org.springframework.core.io.ByteArrayResource(fileBytes), finalType);
                    }

                    mailSender.send(mimeMessage);
                    log.info("Verification HTML email with buttons and attachment sent successfully to {}", email);
                } else {
                    MimeMessage mimeMessage = mailSender.createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
                    helper.setTo(email);
                    helper.setSubject("Xác nhận thông tin tài liệu - Hệ thống DMS");
                    String text = "Xin chào,\n\nBạn có yêu cầu xác nhận tài liệu:\n" 
                            + "- Tên tài liệu: " + displayContractName + "\n"
                            + "- Số tiền: " + displayAmount + "\n"
                            + "- Mô tả: " + displayDesc + "\n\n"
                            + "Vui lòng click vào một trong các link sau để xác nhận:\n"
                            + "- ĐỒNG Ý: " + callbackAgreeUrl + "\n"
                            + "- TỪ CHỐI: " + callbackRejectUrl + "\n\n"
                            + "Trân trọng.";
                    helper.setText(text, false);

                    if (fileBytes != null) {
                        String finalName = (attachmentFileName != null && !attachmentFileName.trim().isEmpty()) ? attachmentFileName : "document.pdf";
                        String finalType = (attachmentContentType != null && !attachmentContentType.trim().isEmpty()) ? attachmentContentType : "application/pdf";
                        helper.addAttachment(finalName, new org.springframework.core.io.ByteArrayResource(fileBytes), finalType);
                    }

                    mailSender.send(mimeMessage);
                    log.info("Verification plain text email with attachment sent successfully to {}", email);
                }
            } catch (Exception e) {
                log.error("Failed to send verification email to {}: {}. Code printed to console: {}", email, e.getMessage(), code);
            }
        } else {
            log.warn("JavaMailSender bean not configured. Printed code to console: {}", code);
        }
    }

    @Override
    public boolean confirmVerificationCode(String email, String code, String decision) {
        Optional<EmailVerification> opt = emailVerificationRepository.findByEmailAndCode(email, code);
        if (opt.isPresent()) {
            EmailVerification verification = opt.get();
            if (verification.getExpiryTime().after(new Date())) {
                verification.setDecision(decision);
                if ("AGREE".equalsIgnoreCase(decision)) {
                    verification.setVerified(true);
                } else {
                    verification.setVerified(false);
                }
                emailVerificationRepository.save(verification);
                log.info("Email {} decision saved: {} with code {}", email, decision, code);
                return "AGREE".equalsIgnoreCase(decision);
            } else {
                log.warn("Validation token for email {} has expired.", email);
            }
        } else {
            log.warn("Invalid validation token {} for email {}", code, email);
        }
        return false;
    }

    @Override
    public boolean isEmailVerified(String email) {
        Optional<EmailVerification> opt = emailVerificationRepository.findTopByEmailAndVerifiedTrueOrderByExpiryTimeDesc(email);
        if (opt.isPresent()) {
            EmailVerification verification = opt.get();
            // Valid if verified and not expired
            boolean isValid = verification.getExpiryTime().after(new Date());
            if (isValid) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Optional<EmailVerification> getVerification(String email, String code) {
        return emailVerificationRepository.findByEmailAndCode(email, code);
    }
}
