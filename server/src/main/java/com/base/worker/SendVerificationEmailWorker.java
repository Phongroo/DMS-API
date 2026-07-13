package com.base.worker;

import com.base.model.DmsDoc;
import com.base.repo.DmsDocRepository;
import com.base.service.EmailVerificationService;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ExternalTaskSubscription("sendVerificationEmail")
public class SendVerificationEmailWorker implements ExternalTaskHandler {

    private static final Logger log = LoggerFactory.getLogger(SendVerificationEmailWorker.class);

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Autowired
    private DmsDocRepository dmsDocRepository;

    @Override
    public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        String processInstanceId = externalTask.getProcessInstanceId();
        log.info("Executing SendVerificationEmailWorker for processInstanceId: {}", processInstanceId);

        try {
            // Retrieve process variables from Camunda
            String email = externalTask.getVariable("email");
            String contractName = externalTask.getVariable("contractName");
            Double amount = externalTask.getVariable("amount");
            String description = externalTask.getVariable("description");

            // Look up associated DmsDoc to enrich details if variables are not passed in process context
            DmsDoc doc = dmsDocRepository.findByProcessInstanceId(processInstanceId);
            if (doc != null) {
                if (email == null || email.trim().isEmpty()) {
                    email = doc.getEmail();
                }
                if (contractName == null || contractName.trim().isEmpty()) {
                    contractName = doc.getDocType() != null ? doc.getDocType() : "Hợp đồng quy trình";
                }
                if (amount == null) {
                    amount = doc.getAmount();
                }
                if (description == null || description.trim().isEmpty()) {
                    description = "Quy trình xử lý tài liệu trên hệ thống DMS. Người tạo: " + doc.getCreatedBy();
                }
            }

            if (email == null || email.trim().isEmpty()) {
                log.error("Email process variable is missing for processInstanceId: {}", processInstanceId);
                externalTaskService.handleFailure(externalTask, "Missing email variable", "The process variable 'email' must be set to send the verification code", 0, 0);
                return;
            }

            log.info("Sending verification email to {} for contract '{}', amount: {}, desc: '{}' (ExternalTaskId: {})", email, contractName, amount, description, externalTask.getId());
            
            // Trigger sending of verification email with contract details, processInstanceId, and externalTaskId
            emailVerificationService.sendVerificationCode(email, contractName, amount, description, processInstanceId, externalTask.getId());

            log.info("SendVerificationEmailWorker initiated verification email. Waiting for client callback to complete external task: {}", externalTask.getId());
        } catch (Exception e) {
            log.error("Error executing sendVerificationEmail task: {}", e.getMessage(), e);
            externalTaskService.handleFailure(externalTask, "Execution error", e.getMessage(), 0, 0);
        }
    }
}
