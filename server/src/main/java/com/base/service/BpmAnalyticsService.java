package com.base.service;

import java.util.List;
import java.util.Map;

public interface BpmAnalyticsService {
    List<Map<String, Object>> getStepDurationStats();
    List<Map<String, Object>> getBottlenecks(int slaHours);
    Map<String, Object> getOverviewStats();
    Map<String, Object> getCombinedDashboardStats(int slaHours);
    List<Map<String, Object>> getDocTypeDurationStats();
    List<Map<String, Object>> getUserTaskPerformanceStats();
}
