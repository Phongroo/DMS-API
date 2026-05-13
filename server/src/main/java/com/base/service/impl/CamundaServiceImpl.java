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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

import static com.base.constant.APIConstants.COMPLETE_TASK;
import static com.base.constant.APIConstants.START_TASK;

@Service
public class CamundaServiceImpl implements CamundaService {

    private final RestTemplate restTemplate = new RestTemplate();
    @Autowired
    private DmsDocRepository dmsDocRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private DmsDocService dmsDocService;
    @Autowired
    DmsFileService dmsFileService;

    @Override
    public BaseResponse startProcess(MultipartFile file) {
        try {
            Authentication auth =
                    SecurityContextHolder.getContext()
                            .getAuthentication();

            String username = auth.getName();
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


            ResponseEntity<Object> response =
                    restTemplate.postForEntity(
                            START_TASK,
                            variables,
                            Object.class
                    );
            Map<String, Object> body =
                    objectMapper.convertValue(response.getBody(), new TypeReference<Map<String, Object>>() {});
            String processInstanceId = String.valueOf(body.get("processInstanceId"));

            DmsDoc dmsDoc = new DmsDoc();
            dmsDoc.setCreatedBy(username);
            dmsDoc.setProcessInstanceId(processInstanceId);
            dmsDoc.setStatus(StatusEnum.DRAFT.toString());
            dmsDoc.setCreatedDate(new Date());
            DmsDoc doc = dmsDocRepository.save(dmsDoc);
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
        try {
            String statusHandle = "";
            Map<String, Object> variables = new HashMap<>();

            variables.put("managerApproved", req.getManagerApproved());
            variables.put("directorApproved", req.getDirectorApproved());
            variables.put("processInstanceId", req.getProcessInstanceId());
            if(req.getManagerApproved() == null && req.getDirectorApproved() == null){
                statusHandle = StatusEnum.PENDING_MANAGER_APPROVAL.toString();
                variables.put("position", "MANAGER");
            }else if (req.getProcessInstanceId() != null && req.getDirectorApproved() == null){
                if("1".equals(req.getManagerApproved())){
                    statusHandle = StatusEnum.MANAGER_APPROVED.toString();
                    variables.put("position", "DIRECTOR");
                }else {
                    statusHandle = StatusEnum.MANAGER_REJECTED.toString();
                }
            }else if (req.getDirectorApproved() != null && req.getManagerApproved() == null){
                if("1".equals(req.getDirectorApproved())){
                    statusHandle = StatusEnum.DIRECTOR_APPROVED.toString();
                }else {
                    statusHandle = StatusEnum.DIRECTOR_REJECTED.toString();
                }
            }

            ResponseEntity<Object> response =
                    restTemplate.postForEntity(
                            COMPLETE_TASK,
                            variables,
                            Object.class
                    );

            WorkflowContext workflowContext = new WorkflowContext();
            workflowContext.setStatusHandle(statusHandle);
            workflowContext.setProcessInstanceId(req.getProcessInstanceId());
            workflowContext.setDmsDoc(req.getDmsDoc());
            dmsDocService.handleTask(workflowContext);
            return new BaseResponse(200, response.getBody(), "Process started successfully");


        } catch (Exception e) {

            return new BaseResponse(400, null, e.getMessage());

        }
    }

    @Override
    public BaseResponse viewTask(String processInstanceId) {

        try {

            // =========================
            // 1. GET PROCESS DEFINITION
            // =========================
            String definitionId = null;

            try {

                String runtimeUrl =
                        "http://localhost:8081/engine-rest/process-instance/"
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
                String historyProcessUrl =
                        "http://localhost:8081/engine-rest/history/process-instance/"
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
            String xmlUrl =
                    "http://localhost:8081/engine-rest/process-definition/"
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
            String historyUrl =
                    "http://localhost:8081/engine-rest/history/activity-instance"
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
            // REMOVE DONE FROM RUNNING
            // =========================
            running.removeAll(completed);

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
}
