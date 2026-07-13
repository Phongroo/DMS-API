package com.base.service.impl;

import com.base.Enum.StatusEnum;
import com.base.model.Authority;
import com.base.model.DmsDoc;
import com.base.model.Req.DmsFileReq;
import com.base.model.Req.ProcessTaskReq;
import com.base.model.Req.StartJobReq;
import com.base.model.Req.WorkflowContext;
import com.base.model.Res.BaseResponse;
import com.base.repo.DmsDocRepository;
import com.base.service.CamundaService;
import com.base.service.DmsDocService;
import com.base.service.DmsFileService;
import com.base.util.ByteArrayMultipartFile;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

import static com.base.constant.APIConstants.COMPLETE_TASK;
import static com.base.constant.APIConstants.START_TASK;

@Service
public class CamundaServiceImpl implements CamundaService {

    private static final Logger log = LoggerFactory.getLogger(CamundaServiceImpl.class);

    @Autowired
    private DmsDocRepository dmsDocRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private DmsDocService dmsDocService;
    @Autowired
    DmsFileService dmsFileService;
    @Autowired
    private com.base.service.DocumentTemplateService documentTemplateService;
    @Value("${camunda.url}")
    private String defaultCamundaUrl;
    @Value("${camunda.bpm.client.worker-id:dms-worker}")
    private String workerId;
    @Autowired
    private com.base.service.SystemSettingsService systemSettingsService;
    @Autowired
    private com.base.service.DmsDocHistoryService dmsDocHistoryService;
    @Autowired
    private com.base.service.AuditLogService auditLogService;
    @Autowired
    private com.base.repo.UserRepository userRepository;
    @Autowired
    private com.base.service.EmailVerificationService emailVerificationService;

    private String getCamundaUrl() {
        try {
            return systemSettingsService.getSettings().getCamundaUrl();
        } catch (Exception e) {
            return defaultCamundaUrl;
        }
    }

    private RestTemplate getRestTemplate() {
        RestTemplate rt = new RestTemplate();
        try {
            int timeout = systemSettingsService.getSettings().getApiTimeout();
            org.springframework.http.client.SimpleClientHttpRequestFactory factory = 
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(timeout);
            factory.setReadTimeout(timeout);
            rt.setRequestFactory(factory);
        } catch (Exception e) {
            // fallback
        }
        return rt;
    }

    @Override
    public BaseResponse startProcess(MultipartFile file, String docType, String email) {
        String camundaUrl = getCamundaUrl();
        RestTemplate restTemplate = getRestTemplate();
        try {
            Authentication auth =
                    SecurityContextHolder.getContext()
                            .getAuthentication();

            String username = auth.getName();

            // Validate that the email is provided
            if (email == null || email.trim().isEmpty()) {
                return new BaseResponse(400, null, "Email xác thực là bắt buộc để bắt đầu quy trình.");
            }

            com.base.model.User userObj = userRepository.findByUsername(username);

            String position = auth.getAuthorities()
                    .stream()
                    .filter(a -> a instanceof Authority)
                    .map(a -> ((Authority) a).getPosition())
                    .findFirst()
                    .orElse(null);

            Map<String, Object> variables = new HashMap<>();

//            variables.put("documentId",req.getDocumentId());
            variables.put("createdBy", username);
            variables.put("position", position);
            variables.put("canStartTask", true);
            variables.put("isEmailVerified", true);
            variables.put("email", email);


            ResponseEntity<Object> response =
                    restTemplate.postForEntity(
                            camundaUrl +
                            START_TASK,
                            variables,
                            Object.class
                    );
            Map<String, Object> body =
                    objectMapper.convertValue(response.getBody(), new TypeReference<Map<String, Object>>() {});
            String processInstanceId = String.valueOf(body.get("processInstanceId"));

            DmsDoc dmsDoc = new DmsDoc();
            dmsDoc.setProcessInstanceId(processInstanceId);
            dmsDoc.setStatus(StatusEnum.DRAFT.toString());
            dmsDoc.setCreatedDate(new Date());
            dmsDoc.setDocType(docType);
            dmsDoc.setEmail(email);
            if (userObj != null) {
                dmsDoc.setBranchId(userObj.getBranchId());
            }

            String createdByVal = username;
            try {
                if (userObj != null) {
                    createdByVal = userObj.getFirstName() + " " + userObj.getLastName();
                }
            } catch (Exception e) {
                // ignore
            }
            dmsDoc.setCreatedBy(username);
            dmsDoc.setCreatorName(createdByVal);

            DmsDoc doc = dmsDocRepository.save(dmsDoc);
            dmsDocHistoryService.log(doc.getId(), username, "Tạo tài liệu và tải lên phiên bản nháp v1", StatusEnum.DRAFT.toString());
            DmsFileReq dmsFileReq = new DmsFileReq();
            dmsFileReq.setDmsDoc(doc);
            dmsFileService.save(dmsFileReq, file);
            return new BaseResponse(200, response.getBody(), "success");
        }catch (Exception e){
            return new BaseResponse(400, null, e.getMessage());
        }

    }

    @Override
    public BaseResponse processTask(ProcessTaskReq req) {
        String camundaUrl = getCamundaUrl();
        RestTemplate restTemplate = getRestTemplate();
        try {
            String statusHandle = "";
            Map<String, Object> variables = new HashMap<>();

            variables.put("managerApproved", req.getManagerApproved());
            variables.put("directorApproved", req.getDirectorApproved());
            variables.put("processInstanceId", req.getProcessInstanceId());
            variables.put("action", req.getAction());

            DmsDoc doc = dmsDocRepository.findByProcessInstanceId(req.getProcessInstanceId());
            if (doc == null) {
                doc = req.getDmsDoc();
            }

            if (req.getAction() != null && !req.getAction().isEmpty()) {
                if ("1".equals(req.getAction())) {
                    statusHandle = StatusEnum.PENDING_MANAGER_APPROVAL.toString();
                    variables.put("position", "MANAGER");
                } else {
                    statusHandle = StatusEnum.CANCELLED.toString();
                    variables.put("position", "STAFF");
                }
            } else if (req.getManagerApproved() == null && req.getDirectorApproved() == null) {
                statusHandle = StatusEnum.PENDING_MANAGER_APPROVAL.toString();
                variables.put("position", "MANAGER");
            } else if (req.getProcessInstanceId() != null && req.getDirectorApproved() == null) {
                if ("1".equals(req.getManagerApproved())) {
                    // If amount <= 10,000,000, manager approval completes the process.
                    if (doc != null && doc.getAmount() != null && doc.getAmount() <= 10000000.0) {
                        statusHandle = StatusEnum.COMPLETED.toString();
                    } else {
                        statusHandle = StatusEnum.MANAGER_APPROVED.toString();
                        variables.put("position", "DIRECTOR");
                    }
                } else {
                    statusHandle = StatusEnum.MANAGER_REJECTED.toString();
                    variables.put("position", "STAFF");
                }
            } else if (req.getDirectorApproved() != null && req.getManagerApproved() == null) {
                if ("1".equals(req.getDirectorApproved())) {
                    statusHandle = StatusEnum.DIRECTOR_APPROVED.toString();
                } else {
                    statusHandle = StatusEnum.DIRECTOR_REJECTED.toString();
                    variables.put("position", "STAFF");
                }
            }

            ResponseEntity<Object> response =
                    restTemplate.postForEntity(
                            camundaUrl +
                            COMPLETE_TASK,
                            variables,
                            Object.class
                    );

            WorkflowContext workflowContext = new WorkflowContext();
            workflowContext.setStatusHandle(statusHandle);
            workflowContext.setProcessInstanceId(req.getProcessInstanceId());
            workflowContext.setDmsDoc(doc);
            dmsDocService.handleTask(workflowContext);

            String actionDesc = "Thực hiện xử lý quy trình";
            String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null 
                    ? org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName() 
                    : "system";
            
            if (req.getManagerApproved() == null && req.getDirectorApproved() == null && req.getAction() == null) {
                actionDesc = "Gửi duyệt quy trình";
            } else if (req.getAction() != null) {
                actionDesc = "1".equals(req.getAction()) ? "Gửi duyệt lại hồ sơ" : "Hủy hồ sơ";
            } else if (req.getManagerApproved() != null && req.getDirectorApproved() == null) {
                if ("1".equals(req.getManagerApproved())) {
                    actionDesc = "Quản lý phê duyệt bước 1";
                } else {
                    actionDesc = "Quản lý từ chối phê duyệt (Chuyển trả người tạo)";
                }
            } else if (req.getDirectorApproved() != null) {
                if ("1".equals(req.getDirectorApproved())) {
                    actionDesc = "Director phê duyệt hoàn tất (COMPLETED)";
                } else {
                    actionDesc = "Director từ chối phê duyệt (Chuyển trả người tạo)";
                }
            }
            
            if (doc != null) {
                dmsDocHistoryService.log(doc.getId(), username, actionDesc, statusHandle);
                String docIdStr = doc.getDocId() != null ? doc.getDocId() : "N/A";
                String logLevel = (actionDesc.contains("từ chối") || "Hủy hồ sơ".equals(actionDesc)) ? "WARN" : "INFO";
                auditLogService.log("Xử lý quy trình tài liệu " + docIdStr + ": " + actionDesc, logLevel);
            }

            return new BaseResponse(200, response.getBody(), "Process started successfully");


        } catch (Exception e) {

            return new BaseResponse(400, null, e.getMessage());

        }
    }

    @Override
    public BaseResponse startProcessFromTemplate(String docType, Map<String, Object> templateData, String email) {
        try {
            if (docType == null || docType.trim().isEmpty()) {
                return new BaseResponse(400, null, "docType is required for template-based process start.");
            }

            // Generate document using the template service (which uses poi-tl and Custom Delimiters format `{d.field}`)
            byte[] docBytes = documentTemplateService.generateDocument(docType, templateData);

            // Convert Word (.docx) file bytes to PDF (.pdf) file bytes
            byte[] pdfBytes = documentTemplateService.convertDocxToPdf(docBytes);

            // Construct original file name
            String originalFileName = docType.toUpperCase() + "_" + System.currentTimeMillis() + ".pdf";

            // Wrap in our custom ByteArrayMultipartFile
            MultipartFile file = new ByteArrayMultipartFile(
                    pdfBytes,
                    "file",
                    originalFileName,
                    "application/pdf"
            );

            // Forward to the main startProcess implementation
            return startProcess(file, docType, email);
        } catch (Exception e) {
            return new BaseResponse(500, null, "Failed to start process from template: " + e.getMessage());
        }
    }

    @Override
    public BaseResponse viewTask(String processInstanceId) {
        String camundaUrl = getCamundaUrl();
        RestTemplate restTemplate = getRestTemplate();
        try {

            // =========================
            // 1. GET PROCESS DEFINITION
            // =========================
            String definitionId = null;

            try {

                String runtimeUrl = camundaUrl +
                        "/engine-rest/process-instance/"
                        + processInstanceId;

                ResponseEntity<Map> runtimeRes =
                        restTemplate.getForEntity(
                                runtimeUrl,
                                Map.class
                        );

                Map runtimeBody =
                        runtimeRes.getBody();

                if (runtimeBody != null) {

                    definitionId =
                            (String) runtimeBody.get(
                                    "definitionId"
                            );
                }

            } catch (Exception ex) {

                // =========================
                // PROCESS DONE
                // =========================
                String historyProcessUrl = camundaUrl +
                        "/engine-rest/history/process-instance/"
                        + processInstanceId;

                ResponseEntity<Map> historyProcessRes =
                        restTemplate.getForEntity(
                                historyProcessUrl,
                                Map.class
                        );

                Map historyBody =
                        historyProcessRes.getBody();

                if (historyBody != null) {

                    definitionId =
                            (String) historyBody.get(
                                    "processDefinitionId"
                            );
                }
            }

            // =========================
            // INVALID PROCESS
            // =========================
            if (definitionId == null) {

                return new BaseResponse(
                        404,
                        null,
                        "PROCESS NOT FOUND"
                );
            }

            // =========================
            // 2. GET BPMN XML
            // =========================
            String xmlUrl = camundaUrl +
                    "/engine-rest/process-definition/"
                    + definitionId
                    + "/xml";

            ResponseEntity<Map> xmlRes =
                    restTemplate.getForEntity(
                            xmlUrl,
                            Map.class
                    );

            Map xmlBody =
                    xmlRes.getBody();

            String bpmnXml = null;

            if (xmlBody != null) {

                bpmnXml =
                        (String) xmlBody.get(
                                "bpmn20Xml"
                        );
            }

            // =========================
            // XML NULL
            // =========================
            if (bpmnXml == null) {

                return new BaseResponse(
                        500,
                        null,
                        "BPMN XML NOT FOUND"
                );
            }

            // =========================
            // 3. GET HISTORY ACTIVITY
            // =========================
            String historyUrl = camundaUrl +
                    "/engine-rest/history/activity-instance"
                    + "?processInstanceId="
                    + processInstanceId
                    + "&sortBy=startTime"
                    + "&sortOrder=asc";

            ResponseEntity<List> historyRes =
                    restTemplate.getForEntity(
                            historyUrl,
                            List.class
                    );

            List<Map> activities =
                    historyRes.getBody();

            // =========================
            // 4. BUILD DATA
            // =========================
            Set<String> running =
                    new LinkedHashSet<>();

            Set<String> completed =
                    new LinkedHashSet<>();

            Set<String> flows =
                    new LinkedHashSet<>();

            Set<String> endEvents =
                    new LinkedHashSet<>();

            Set<String> errors =
                    new LinkedHashSet<>();

            if (activities != null) {

                for (Map act : activities) {

                    if (act == null) {
                        continue;
                    }

                    Object activityIdObj =
                            act.get("activityId");

                    Object activityTypeObj =
                            act.get("activityType");

                    if (
                            activityIdObj == null ||
                            activityTypeObj == null
                    ) {
                        continue;
                    }

                    String activityId =
                            activityIdObj.toString();

                    String activityType =
                            activityTypeObj.toString();

                    String endTime =
                            act.get("endTime") != null
                                    ? act.get("endTime").toString()
                                    : null;

                    // =========================
                    // FLOW
                    // =========================
                    if (
                            "sequenceFlow".equals(
                                    activityType
                            )
                    ) {

                        flows.add(activityId);
                        continue;
                    }

                    // =========================
                    // END EVENT
                    // =========================
                    if (
                            activityId.endsWith("End")
                    ) {

                        endEvents.add(activityId);
                    }

                    // =========================
                    // RUNNING
                    // =========================
                    if (endTime == null) {

                        running.add(activityId);

                    } else {

                        completed.add(activityId);
                    }
                }
            }

            // =========================
            // REMOVE RUNNING FROM COMPLETED
            // =========================
            completed.removeAll(running);

            // =========================
            // 5. RESULT
            // =========================
            Map<String, Object> result =
                    new HashMap<>();

            result.put(
                    "bpmn20Xml",
                    bpmnXml
            );

            result.put(
                    "runningActivities",
                    new ArrayList<>(running)
            );

            result.put(
                    "completedActivities",
                    new ArrayList<>(completed)
            );

            result.put(
                    "errorActivities",
                    new ArrayList<>(errors)
            );

            result.put(
                    "flows",
                    new ArrayList<>(flows)
            );

            result.put(
                    "endEvents",
                    new ArrayList<>(endEvents)
            );

            return new BaseResponse(
                    200,
                    result,
                    "OK"
            );

        } catch (Exception e) {

            e.printStackTrace();

            return new BaseResponse(
                    500,
                    null,
                    e.getMessage()
            );
        }
    }

    @Override
    public void notifyVerificationCallback(String processInstanceId, String decision) {
        String camundaUrl = getCamundaUrl();
        RestTemplate restTemplate = getRestTemplate();
        try {
            // 1. Update variables in Camunda process instance
//            String varUrl = camundaUrl + "/engine-rest/process-instance/" + processInstanceId + "/variables";
//
//            Map<String, Object> modifications = new HashMap<>();
//
//            Map<String, Object> isEmailVerifiedVar = new HashMap<>();
//            isEmailVerifiedVar.put("value", "AGREE".equalsIgnoreCase(decision));
//            isEmailVerifiedVar.put("type", "Boolean");
//            modifications.put("isEmailVerified", isEmailVerifiedVar);
//
//            Map<String, Object> emailVerificationSentVar = new HashMap<>();
//            emailVerificationSentVar.put("value", true);
//            emailVerificationSentVar.put("type", "Boolean");
//            modifications.put("emailVerificationSent", emailVerificationSentVar);
//
//            Map<String, Object> canStartTaskVar = new HashMap<>();
//            canStartTaskVar.put("value", "AGREE".equalsIgnoreCase(decision));
//            canStartTaskVar.put("type", "Boolean");
//            modifications.put("canStartTask", canStartTaskVar);
//
//            Map<String, Object> body = new HashMap<>();
//            body.put("modifications", modifications);
//
//            restTemplate.postForEntity(varUrl, body, Object.class);
//            log.info("Successfully updated process variables in Camunda for processInstanceId: {}", processInstanceId);

            // 2. Complete the waiting task in Camunda (if there is an active task)
            try {
              log.info("Successfully updated process variables in Camunda for processInstanceId: {}", processInstanceId);

                Map<String, Object> completeVars = new HashMap<>();
                completeVars.put("processInstanceId", processInstanceId);
                completeVars.put("emailVerificationSent", "AGREE".equalsIgnoreCase(decision) ? 1 : 0);
                
                restTemplate.postForEntity(
                        camundaUrl + COMPLETE_TASK,
                        completeVars,
                        Object.class
                );
                log.info("Successfully completed waiting task in Camunda for processInstanceId: {}", processInstanceId);
            } catch (Exception te) {
                log.warn("Could not auto-complete Camunda task (maybe no active task is waiting): {}", te.getMessage());
            }

        } catch (Exception e) {
            log.error("Failed to notify Camunda of verification callback: {}", e.getMessage(), e);
        }
    }

    @Override
    public void completeExternalTask(String externalTaskId, String decision) {
        String camundaUrl = getCamundaUrl();
        RestTemplate restTemplate = getRestTemplate();
        try {
            String url = camundaUrl + "/engine-rest/external-task/" + externalTaskId + "/complete";

            Map<String, Object> payload = new HashMap<>();
            payload.put("workerId", workerId != null ? workerId : "dms-worker");

            Map<String, Object> variables = new HashMap<>();

            Map<String, Object> emailVar = new HashMap<>();
            emailVar.put("value", "AGREE".equalsIgnoreCase(decision) ? 1 : 0);
            emailVar.put("type", "Integer");
            variables.put("emailVerificationSent", emailVar);

            Map<String, Object> verifiedVar = new HashMap<>();
            verifiedVar.put("value", "AGREE".equalsIgnoreCase(decision));
            verifiedVar.put("type", "Boolean");
            variables.put("isEmailVerified", verifiedVar);

            Map<String, Object> canStartTaskVar = new HashMap<>();
            canStartTaskVar.put("value", "AGREE".equalsIgnoreCase(decision));
            canStartTaskVar.put("type", "Boolean");
            variables.put("canStartTask", canStartTaskVar);

            payload.put("variables", variables);

            log.info("Completing external task {} with decision {} and variables {}", externalTaskId, decision, variables);
            restTemplate.postForEntity(url, payload, Object.class);
            log.info("Successfully completed external task: {}", externalTaskId);
        } catch (Exception e) {
            log.error("Failed to complete external task {}: {}", externalTaskId, e.getMessage(), e);
            throw new RuntimeException("Failed to complete external task in Camunda: " + e.getMessage(), e);
        }
    }

    @Override
    public BaseResponse deployBpmn(String xml, String filename) {
        String camundaUrl = getCamundaUrl();
        RestTemplate restTemplate = getRestTemplate();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("deployment-name", "Dynamic_UI_Deployment_" + System.currentTimeMillis());
            body.add("enable-duplicate-filtering", "true");
            body.add("deploy-changed-only", "true");

            byte[] xmlBytes = xml.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            ByteArrayResource fileResource = new ByteArrayResource(xmlBytes) {
                @Override
                public String getFilename() {
                    return filename != null && !filename.isEmpty() ? filename : "process.bpmn";
                }
            };
            body.add("data", fileResource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            String url = camundaUrl + "/engine-rest/deployment/create";
            
            log.info("Deploying BPMN to Camunda at URL: {}", url);
            ResponseEntity<Object> response = restTemplate.postForEntity(url, requestEntity, Object.class);
            
            return new BaseResponse(200, response.getBody(), "Quy trình đã được deploy thành công lên Camunda!");
        } catch (Exception e) {
            log.error("Failed to deploy BPMN to Camunda", e);
            return new BaseResponse(500, null, "Lỗi deploy quy trình lên Camunda: " + e.getMessage());
        }
    }
}
