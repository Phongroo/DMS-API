package com.base.controller;

import com.base.model.Req.ProcessTaskReq;
import com.base.model.Req.StartJobReq;
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
            MultipartFile file

    ) {

        return ResponseEntity.ok(
                camundaService.startProcess(
                        file
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
}
