package com.base.controller;

import com.base.model.Req.ProcessTaskReq;
import com.base.model.Req.StartJobReq;
import com.base.model.Req.StartProcessReq;
import com.base.model.Res.BaseResponse;
import com.base.service.CamundaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;


@RestController
public class StartProcessControler {
    @Autowired
    private CamundaService camundaService;

    @PostMapping(
            value = "/start",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<BaseResponse> startProcess(

            @RequestPart("file")
            MultipartFile file,
            @RequestParam(value = "docType", required = false)
            String docType,
            @RequestParam(value = "email", required = false)
            String email

    ) {

        return ResponseEntity.ok(
                camundaService.startProcess(
                        file,
                        docType,
                        email
                )
        );
    }

    @PostMapping(
            value = "/start",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<BaseResponse> startProcessFromJson(
            @RequestBody StartProcessReq req
    ) {
        return ResponseEntity.ok(
                camundaService.startProcessFromTemplate(
                        req.getDocType(),
                        req.getTemplateData(),
                        req.getEmail()
                )
        );
    }

    @PostMapping("/process")
    public ResponseEntity<BaseResponse> processTask(
            @RequestBody ProcessTaskReq req) {
        return ResponseEntity.ok(camundaService.processTask(req));
    }
    @GetMapping("/viewer/{processInstanceId}")
    public ResponseEntity<?> getViewerUrl(
            @PathVariable String processInstanceId) {
        return ResponseEntity.ok(camundaService.viewTask(processInstanceId));
    }

    @PostMapping("/deploy-bpmn")
    public ResponseEntity<BaseResponse> deployBpmn(
            @RequestBody java.util.Map<String, String> payload
    ) {
        String xml = payload.get("xml");
        String filename = payload.get("filename");
        if (xml == null || xml.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new com.base.model.Res.BaseResponse(400, null, "XML content is required"));
        }
        return ResponseEntity.ok(camundaService.deployBpmn(xml, filename));
    }
}
