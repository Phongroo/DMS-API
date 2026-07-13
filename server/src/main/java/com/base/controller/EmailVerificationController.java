package com.base.controller;

import com.base.model.Res.BaseResponse;
import com.base.service.EmailVerificationService;
import com.base.service.CamundaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/verify")
public class EmailVerificationController {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationController.class);

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Autowired
    private CamundaService camundaService;

    @PostMapping("/send")
    public ResponseEntity<BaseResponse> sendCode(
            @RequestParam("email") String email,
            @RequestParam(value = "contractName", required = false) String contractName,
            @RequestParam(value = "amount", required = false) Double amount,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "processInstanceId", required = false) String processInstanceId) {
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.ok(new BaseResponse(400, null, "Email is required"));
        }
        try {
            emailVerificationService.sendVerificationCode(email, contractName, amount, description, processInstanceId, null);
            return ResponseEntity.ok(new BaseResponse(200, null, "Verification email sent to " + email));
        } catch (Exception e) {
            return ResponseEntity.ok(new BaseResponse(500, null, "Failed to send code: " + e.getMessage()));
        }
    }

    @PostMapping("/confirm")
    public ResponseEntity<BaseResponse> confirmCode(
            @RequestParam("email") String email,
            @RequestParam("code") String code,
            @RequestParam(value = "decision", defaultValue = "AGREE") String decision) {
        if (email == null || email.trim().isEmpty() || code == null || code.trim().isEmpty()) {
            return ResponseEntity.ok(new BaseResponse(400, null, "Email and code are required"));
        }
        try {
            boolean success = emailVerificationService.confirmVerificationCode(email, code, decision);
            if (success) {
                Map<String, Object> data = new HashMap<>();
                data.put("canStartTask", true);
                return ResponseEntity.ok(new BaseResponse(200, data, "Email verified successfully"));
            } else {
                Map<String, Object> data = new HashMap<>();
                data.put("canStartTask", false);
                return ResponseEntity.ok(new BaseResponse(400, data, "Invalid code, expired, or verification rejected"));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(new BaseResponse(500, null, "Failed to verify code: " + e.getMessage()));
        }
    }

    @GetMapping("/callback")
    @ResponseBody
    public ResponseEntity<String> callback(
            @RequestParam("email") String email,
            @RequestParam("code") String code,
            @RequestParam("decision") String decision) {
        if (email == null || email.trim().isEmpty() || code == null || code.trim().isEmpty() || decision == null) {
            return ResponseEntity.ok(renderStatusPage(false, "Thiếu các tham số bắt buộc.", null, null, null, decision));
        }
        
        String contractName = null;
        Double amount = null;
        String description = null;
        String processInstanceId = null;
        String externalTaskId = null;
        
        try {
            Optional<com.base.model.EmailVerification> opt = emailVerificationService.getVerification(email, code);
            if (opt.isPresent()) {
                contractName = opt.get().getContractName();
                amount = opt.get().getAmount();
                description = opt.get().getDescription();
                processInstanceId = opt.get().getProcessInstanceId();
                externalTaskId = opt.get().getExternalTaskId();
            }
        } catch (Exception e) {
            // ignore
        }

        try {
            boolean success = emailVerificationService.confirmVerificationCode(email, code, decision);
            if (externalTaskId != null && !externalTaskId.trim().isEmpty()) {
                try {
                    log.info("Attempting to complete Camunda external task: {} with decision: {}", externalTaskId, decision);
                    camundaService.completeExternalTask(externalTaskId, decision);
                } catch (Exception ce) {
                    log.error("Failed to complete Camunda external task {}: {}", externalTaskId, ce.getMessage());
                }
            } else if (processInstanceId != null) {
                try {
                    log.info("Attempting to auto-complete Camunda task for process instance: {} with decision: {}", processInstanceId, decision);
                    camundaService.notifyVerificationCallback(processInstanceId, decision);
                } catch (Exception ce) {
                    log.error("Failed to notify Camunda verification callback for process instance {}: {}", processInstanceId, ce.getMessage());
                }
            }
            if (success) {
                return ResponseEntity.ok(renderStatusPage(true, "Bạn đã đồng ý xác thực tài liệu thành công!", contractName, amount, description, decision));
            } else {
                if ("REJECT".equalsIgnoreCase(decision)) {
                    return ResponseEntity.ok(renderStatusPage(true, "Bạn đã từ chối xác thực tài liệu này.", contractName, amount, description, decision));
                }
                return ResponseEntity.ok(renderStatusPage(false, "Mã xác thực không hợp lệ hoặc đã hết hạn.", contractName, amount, description, decision));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(renderStatusPage(false, "Lỗi hệ thống: " + e.getMessage(), contractName, amount, description, decision));
        }
    }

    private String renderStatusPage(boolean success, String message, String contractName, Double amount, String description, String decision) {
        String classType = success ? "success-wrapper" : "error-wrapper";
        String iconSymbol = success ? "&#10004;" : "&#10006;";
        String statusTitle = success ? "Xác nhận thành công" : "Xác nhận thất bại";
        
        String displayContractName = (contractName != null) ? contractName : "Tài liệu chưa xác định";
        String displayAmount = "Chưa xác định";
        if (amount != null) {
            displayAmount = String.format("%,.0f VNĐ", amount);
        }
        String displayDesc = (description != null) ? description : "Không có mô tả.";
        String displayDecision = "AGREE".equalsIgnoreCase(decision) ? "ĐỒNG Ý XÁC THỰC" : "TỪ CHỐI XÁC THỰC";
        String decisionClass = "AGREE".equalsIgnoreCase(decision) ? "success-text" : "danger-text";

        return "<!DOCTYPE html>\n" +
                "<html lang=\"vi\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>Kết Quả Xác Thực Tài Liệu - DMS</title>\n" +
                "    <link href=\"https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;600;800&display=swap\" rel=\"stylesheet\">\n" +
                "    <style>\n" +
                "        :root {\n" +
                "            --primary: #4F46E5;\n" +
                "            --success: #10B981;\n" +
                "            --danger: #EF4444;\n" +
                "            --card-bg: rgba(30, 41, 59, 0.75);\n" +
                "            --text-light: #F8FAFC;\n" +
                "            --text-muted: #94A3B8;\n" +
                "        }\n" +
                "        * {\n" +
                "            box-sizing: border-box;\n" +
                "            margin: 0;\n" +
                "            padding: 0;\n" +
                "        }\n" +
                "        body {\n" +
                "            font-family: 'Outfit', sans-serif;\n" +
                "            background: linear-gradient(135deg, #0b0f19 0%, #1e1b4b 100%);\n" +
                "            color: var(--text-light);\n" +
                "            display: flex;\n" +
                "            justify-content: center;\n" +
                "            align-items: center;\n" +
                "            min-height: 100vh;\n" +
                "            padding: 20px;\n" +
                "        }\n" +
                "        .container {\n" +
                "            width: 100%;\n" +
                "            max-width: 520px;\n" +
                "        }\n" +
                "        .card {\n" +
                "            background: var(--card-bg);\n" +
                "            backdrop-filter: blur(16px);\n" +
                "            -webkit-backdrop-filter: blur(16px);\n" +
                "            border: 1px solid rgba(255, 255, 255, 0.08);\n" +
                "            border-radius: 24px;\n" +
                "            padding: 35px 25px;\n" +
                "            text-align: center;\n" +
                "            box-shadow: 0 20px 45px rgba(0, 0, 0, 0.4);\n" +
                "            animation: fadeIn 0.6s ease-out;\n" +
                "        }\n" +
                "        @keyframes fadeIn {\n" +
                "            from { opacity: 0; transform: translateY(15px); }\n" +
                "            to { opacity: 1; transform: translateY(0); }\n" +
                "        }\n" +
                "        .icon-wrapper {\n" +
                "            width: 70px;\n" +
                "            height: 70px;\n" +
                "            margin: 0 auto 20px;\n" +
                "            border-radius: 50%;\n" +
                "            display: flex;\n" +
                "            justify-content: center;\n" +
                "            align-items: center;\n" +
                "            font-size: 32px;\n" +
                "        }\n" +
                "        .success-wrapper {\n" +
                "            background: rgba(16, 185, 129, 0.12);\n" +
                "            border: 2px solid var(--success);\n" +
                "            color: var(--success);\n" +
                "            box-shadow: 0 0 15px rgba(16, 185, 129, 0.25);\n" +
                "        }\n" +
                "        .error-wrapper {\n" +
                "            background: rgba(239, 68, 68, 0.12);\n" +
                "            border: 2px solid var(--danger);\n" +
                "            color: var(--danger);\n" +
                "            box-shadow: 0 0 15px rgba(239, 68, 68, 0.25);\n" +
                "        }\n" +
                "        h2 {\n" +
                "            font-size: 26px;\n" +
                "            font-weight: 800;\n" +
                "            margin-bottom: 8px;\n" +
                "        }\n" +
                "        .success-title { color: var(--success); }\n" +
                "        .error-title { color: var(--danger); }\n" +
                "        p.message {\n" +
                "            font-size: 15px;\n" +
                "            color: var(--text-muted);\n" +
                "            margin-bottom: 25px;\n" +
                "            line-height: 1.5;\n" +
                "        }\n" +
                "        .info-table {\n" +
                "            width: 100%;\n" +
                "            border-collapse: collapse;\n" +
                "            margin-bottom: 30px;\n" +
                "            text-align: left;\n" +
                "            background: rgba(15, 23, 42, 0.4);\n" +
                "            border-radius: 12px;\n" +
                "            overflow: hidden;\n" +
                "            border: 1px solid rgba(255,255,255,0.05);\n" +
                "        }\n" +
                "        .info-table td {\n" +
                "            padding: 12px 16px;\n" +
                "            border-bottom: 1px solid rgba(255,255,255,0.05);\n" +
                "            font-size: 14px;\n" +
                "        }\n" +
                "        .info-table tr:last-child td {\n" +
                "            border-bottom: none;\n" +
                "        }\n" +
                "        .label {\n" +
                "            font-weight: 600;\n" +
                "            color: var(--text-muted);\n" +
                "            width: 35%;\n" +
                "        }\n" +
                "        .val {\n" +
                "            color: var(--text-light);\n" +
                "        }\n" +
                "        .success-text { color: var(--success); font-weight: bold; }\n" +
                "        .danger-text { color: var(--danger); font-weight: bold; }\n" +
                "        .btn {\n" +
                "            display: inline-block;\n" +
                "            width: 100%;\n" +
                "            padding: 14px;\n" +
                "            background: linear-gradient(135deg, var(--primary) 0%, #6366F1 100%);\n" +
                "            color: white;\n" +
                "            text-decoration: none;\n" +
                "            border-radius: 12px;\n" +
                "            font-weight: 600;\n" +
                "            font-size: 15px;\n" +
                "            border: none;\n" +
                "            cursor: pointer;\n" +
                "            transition: all 0.2s;\n" +
                "            box-shadow: 0 6px 15px rgba(79, 70, 229, 0.25);\n" +
                "        }\n" +
                "        .btn:hover {\n" +
                "            transform: translateY(-1px);\n" +
                "            box-shadow: 0 10px 20px rgba(79, 70, 229, 0.4);\n" +
                "        }\n" +
                "        .footer {\n" +
                "            margin-top: 25px;\n" +
                "            font-size: 12px;\n" +
                "            color: rgba(255, 255, 255, 0.25);\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"container\">\n" +
                "        <div class=\"card\">\n" +
                "            <div class=\"icon-wrapper " + classType + "\">\n" +
                "                " + iconSymbol + "\n" +
                "            </div>\n" +
                "            <h2 class=\"" + (success ? "success-title" : "error-title") + "\">" + statusTitle + "</h2>\n" +
                "            <p class=\"message\">" + message + "</p>\n" +
                "            \n" +
                "            <table class=\"info-table\">\n" +
                "                <tr>\n" +
                "                    <td class=\"label\">Tên hợp đồng:</td>\n" +
                "                    <td class=\"val\">" + displayContractName + "</td>\n" +
                "                </tr>\n" +
                "                <tr>\n" +
                "                    <td class=\"label\">Số tiền:</td>\n" +
                "                    <td class=\"val\">" + displayAmount + "</td>\n" +
                "                </tr>\n" +
                "                <tr>\n" +
                "                    <td class=\"label\">Chi tiết:</td>\n" +
                "                    <td class=\"val\">" + displayDesc + "</td>\n" +
                "                </tr>\n" +
                "                <tr>\n" +
                "                    <td class=\"label\">Quyết định:</td>\n" +
                "                    <td class=\"val " + decisionClass + "\">" + displayDecision + "</td>\n" +
                "                </tr>\n" +
                "            </table>\n" +
                "            \n" +
                "            <button class=\"btn\" onclick=\"window.close()\">Đóng cửa sổ</button>\n" +
                "            <div class=\"footer\">\n" +
                "                HỆ THỐNG QUẢN LÝ TÀI LIỆU DMS &copy; 2026\n" +
                "            </div>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
    }
}
