package com.base.controller;

import com.base.model.Req.StartJobReq;
import com.base.model.Res.BaseResponse;
import com.base.repo.DmsDocRepository;
import com.base.service.CamundaService;
import com.base.service.DmsDocService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController()
@RequestMapping("/doc")
public class DmsDocController {
    @Autowired
    private DmsDocService service;
    @GetMapping("/fetchDoc")
    public ResponseEntity<BaseResponse> startProcess() {
        return ResponseEntity.ok(service.fetchDoc());
    }
}
