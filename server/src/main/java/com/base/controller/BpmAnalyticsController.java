package com.base.controller;

import com.base.model.Res.BaseResponse;
import com.base.service.BpmAnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/admin/analytics")
public class BpmAnalyticsController {

    @Autowired
    private BpmAnalyticsService bpmAnalyticsService;

    @GetMapping("/step-durations")
    public ResponseEntity<BaseResponse> getStepDurationStats() {
        try {
            List<Map<String, Object>> stats = bpmAnalyticsService.getStepDurationStats();
            return ResponseEntity.ok(new BaseResponse(200, stats, "success"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new BaseResponse(500, null, e.getMessage()));
        }
    }

    @GetMapping("/bottlenecks")
    public ResponseEntity<BaseResponse> getBottlenecks(@RequestParam(value = "slaHours", defaultValue = "24") int slaHours) {
        try {
            List<Map<String, Object>> bottlenecks = bpmAnalyticsService.getBottlenecks(slaHours);
            return ResponseEntity.ok(new BaseResponse(200, bottlenecks, "success"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new BaseResponse(500, null, e.getMessage()));
        }
    }

    @GetMapping("/overview")
    public ResponseEntity<BaseResponse> getOverviewStats() {
        try {
            Map<String, Object> overview = bpmAnalyticsService.getOverviewStats();
            return ResponseEntity.ok(new BaseResponse(200, overview, "success"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new BaseResponse(500, null, e.getMessage()));
        }
    }

    @GetMapping("/dashboard")
    public ResponseEntity<BaseResponse> getCombinedDashboardStats(@RequestParam(value = "slaHours", defaultValue = "24") int slaHours) {
        try {
            Map<String, Object> dashboard = bpmAnalyticsService.getCombinedDashboardStats(slaHours);
            return ResponseEntity.ok(new BaseResponse(200, dashboard, "success"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new BaseResponse(500, null, e.getMessage()));
        }
    }

    @GetMapping("/doc-type-durations")
    public ResponseEntity<BaseResponse> getDocTypeDurationStats() {
        try {
            List<Map<String, Object>> stats = bpmAnalyticsService.getDocTypeDurationStats();
            return ResponseEntity.ok(new BaseResponse(200, stats, "success"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new BaseResponse(500, null, e.getMessage()));
        }
    }

    @GetMapping("/user-performance")
    public ResponseEntity<BaseResponse> getUserTaskPerformanceStats() {
        try {
            List<Map<String, Object>> stats = bpmAnalyticsService.getUserTaskPerformanceStats();
            return ResponseEntity.ok(new BaseResponse(200, stats, "success"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new BaseResponse(500, null, e.getMessage()));
        }
    }
}
