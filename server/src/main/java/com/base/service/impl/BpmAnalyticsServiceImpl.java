package com.base.service.impl;

import com.base.model.DmsDoc;
import com.base.model.SystemSettings;
import com.base.repo.DmsDocRepository;
import com.base.service.BpmAnalyticsService;
import com.base.service.SystemSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class BpmAnalyticsServiceImpl implements BpmAnalyticsService {

    @Autowired
    private DmsDocRepository dmsDocRepository;

    @Autowired
    private SystemSettingsService systemSettingsService;

    @Value("${camunda.url}")
    private String defaultCamundaUrl;

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

    private Date parseCamundaDate(Object dateObj) {
        if (dateObj == null) return null;
        String dateStr = String.valueOf(dateObj);
        try {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").parse(dateStr);
        } catch (Exception e) {
            try {
                return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").parse(dateStr);
            } catch (Exception ex) {
                return new Date(); // fallback
            }
        }
    }

    @Override
    public List<Map<String, Object>> getStepDurationStats() {
        String url = getCamundaUrl() + "/engine-rest/history/task?finished=true";
        RestTemplate restTemplate = getRestTemplate();
        
        try {
            List<Map<String, Object>> taskHistory = restTemplate.getForObject(url, List.class);
            if (taskHistory == null || taskHistory.isEmpty()) {
                return Collections.emptyList();
            }

            Map<String, List<Long>> groupedDurations = new HashMap<>();
            for (Map<String, Object> task : taskHistory) {
                String name = (String) task.get("name");
                if (name == null) name = "Unknown Task";
                
                Object durationObj = task.get("duration");
                if (durationObj != null) {
                    long duration = ((Number) durationObj).longValue();
                    groupedDurations.computeIfAbsent(name, k -> new ArrayList<>()).add(duration);
                }
            }

            List<Map<String, Object>> statsList = new ArrayList<>();
            for (Map.Entry<String, List<Long>> entry : groupedDurations.entrySet()) {
                String name = entry.getKey();
                List<Long> durations = entry.getValue();
                
                long total = 0;
                long min = Long.MAX_VALUE;
                long max = Long.MIN_VALUE;
                for (long d : durations) {
                    total += d;
                    if (d < min) min = d;
                    if (d > max) max = d;
                }
                double avg = (double) total / durations.size();

                Map<String, Object> stats = new HashMap<>();
                stats.put("taskName", name);
                stats.put("completedTasksCount", durations.size());
                stats.put("averageDurationMs", avg);
                stats.put("minDurationMs", min);
                stats.put("maxDurationMs", max);
                stats.put("averageDurationHours", avg / (1000.0 * 3600.0));
                statsList.add(stats);
            }
            return statsList;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> getBottlenecks(int slaHours) {
        String url = getCamundaUrl() + "/engine-rest/history/task?unfinished=true";
        RestTemplate restTemplate = getRestTemplate();
        
        try {
            List<Map<String, Object>> unfinishedTasks = restTemplate.getForObject(url, List.class);
            if (unfinishedTasks == null || unfinishedTasks.isEmpty()) {
                return Collections.emptyList();
            }

            long thresholdMs = (long) slaHours * 3600 * 1000;
            List<Map<String, Object>> bottlenecks = new ArrayList<>();

            Map<String, DmsDoc> docMap = new HashMap<>();
            List<DmsDoc> allDocs = dmsDocRepository.findAll();
            for (DmsDoc doc : allDocs) {
                if (doc.getProcessInstanceId() != null) {
                    docMap.put(doc.getProcessInstanceId(), doc);
                }
            }

            for (Map<String, Object> task : unfinishedTasks) {
                Date start = parseCamundaDate(task.get("startTime"));
                if (start == null) continue;

                long pendingMs = System.currentTimeMillis() - start.getTime();
                if (pendingMs > thresholdMs) {
                    String procInstId = (String) task.get("processInstanceId");
                    DmsDoc doc = docMap.get(procInstId);
                    if(doc == null) continue;
                    Map<String, Object> item = new HashMap<>();
                    item.put("taskId", task.get("id"));
                    item.put("taskName", task.get("name"));
                    item.put("assignee", task.get("assignee"));
                    item.put("startTime", start);
                    item.put("pendingDurationMs", pendingMs);
                    item.put("pendingDurationHours", pendingMs / (1000.0 * 3600.0));
                    item.put("slaThresholdHours", slaHours);
                    item.put("docId", doc.getDocId());
                    item.put("createdBy", doc.getCreatedBy());
                    item.put("createdDate", doc.getCreatedDate());
                    item.put("status", doc.getStatus());
                    bottlenecks.add(item);
                }
            }
            return bottlenecks;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public Map<String, Object> getOverviewStats() {
        String baseUrl = getCamundaUrl() + "/engine-rest/history/process-instance/count";
        RestTemplate restTemplate = getRestTemplate();

        Map<String, Object> stats = new HashMap<>();
        try {
            Map<String, Object> totalCountRes = restTemplate.getForObject(baseUrl, Map.class);
            Map<String, Object> finishedCountRes = restTemplate.getForObject(baseUrl + "?finished=true", Map.class);
            Map<String, Object> activeCountRes = restTemplate.getForObject(baseUrl + "?active=true", Map.class);

            long total = totalCountRes != null ? ((Number) totalCountRes.get("count")).longValue() : 0L;
            long finished = finishedCountRes != null ? ((Number) finishedCountRes.get("count")).longValue() : 0L;
            long active = activeCountRes != null ? ((Number) activeCountRes.get("count")).longValue() : 0L;

            stats.put("totalProcessesStarted", total);
            stats.put("completedProcesses", finished);
            stats.put("activeProcesses", active);

            List<DmsDoc> docs = dmsDocRepository.findAll();
            long approvedCount = 0;
            long rejectedCount = 0;
            long draftCount = 0;
            long pendingCount = 0;

            for (DmsDoc d : docs) {
                String status = d.getStatus();
                if (status != null) {
                    if (status.endsWith("APPROVED")) {
                        approvedCount++;
                    } else if (status.endsWith("REJECTED")) {
                        rejectedCount++;
                    } else if (status.equals("DRAFT")) {
                        draftCount++;
                    } else if (status.startsWith("PENDING_")) {
                        pendingCount++;
                    }
                }
            }

            stats.put("localDocsApprovedCount", approvedCount);
            stats.put("localDocsRejectedCount", rejectedCount);
            stats.put("localDocsDraftCount", draftCount);
            stats.put("localDocsPendingCount", pendingCount);

            Map<String, Long> docTypeCounts = new HashMap<>();
            for (DmsDoc d : docs) {
                String dt = d.getDocType();
                if (dt == null || dt.trim().isEmpty()) {
                    dt = "Chưa phân loại";
                }
                docTypeCounts.put(dt, docTypeCounts.getOrDefault(dt, 0L) + 1);
            }
            stats.put("docsCountByDocType", docTypeCounts);

            double approvalRate = (approvedCount + rejectedCount) > 0 ? (double) approvedCount / (approvedCount + rejectedCount) * 100 : 0.0;
            stats.put("approvalRatePercentage", approvalRate);

        } catch (Exception e) {
            stats.put("error", e.getMessage());
        }
        return stats;
    }

    @Override
    public Map<String, Object> getCombinedDashboardStats(int slaHours) {
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("overview", getOverviewStats());
        dashboard.put("stepDurations", getStepDurationStats());
        dashboard.put("bottlenecks", getBottlenecks(slaHours));
        dashboard.put("docTypeDurations", getDocTypeDurationStats());
        dashboard.put("userPerformance", getUserTaskPerformanceStats());
        return dashboard;
    }

    @Override
    public List<Map<String, Object>> getDocTypeDurationStats() {
        String url = getCamundaUrl() + "/engine-rest/history/process-instance?finished=true";
        RestTemplate restTemplate = getRestTemplate();
        try {
            List<Map<String, Object>> processHistory = restTemplate.getForObject(url, List.class);
            if (processHistory == null || processHistory.isEmpty()) {
                return Collections.emptyList();
            }

            Map<String, DmsDoc> docMap = new HashMap<>();
            List<DmsDoc> allDocs = dmsDocRepository.findAll();
            for (DmsDoc doc : allDocs) {
                if (doc.getProcessInstanceId() != null) {
                    docMap.put(doc.getProcessInstanceId(), doc);
                }
            }

            Map<String, List<Long>> groupedDurations = new HashMap<>();
            for (Map<String, Object> proc : processHistory) {
                String procInstId = (String) proc.get("id");
                Object durationObj = proc.get("durationInMillis");
                if (procInstId != null && durationObj != null) {
                    DmsDoc doc = docMap.get(procInstId);
                    String docType = (doc != null && doc.getDocType() != null) ? doc.getDocType() : "Chưa phân loại";
                    long duration = ((Number) durationObj).longValue();
                    groupedDurations.computeIfAbsent(docType, k -> new ArrayList<>()).add(duration);
                }
            }

            List<Map<String, Object>> statsList = new ArrayList<>();
            for (Map.Entry<String, List<Long>> entry : groupedDurations.entrySet()) {
                String docType = entry.getKey();
                List<Long> durations = entry.getValue();
                long total = 0;
                long min = Long.MAX_VALUE;
                long max = Long.MIN_VALUE;
                for (long d : durations) {
                    total += d;
                    if (d < min) min = d;
                    if (d > max) max = d;
                }
                double avg = (double) total / durations.size();
                Map<String, Object> stats = new HashMap<>();
                stats.put("docType", docType);
                stats.put("completedCount", durations.size());
                stats.put("averageDurationMs", avg);
                stats.put("averageDurationHours", avg / (1000.0 * 3600.0));
                stats.put("minDurationHours", (double) min / (1000.0 * 3600.0));
                stats.put("maxDurationHours", (double) max / (1000.0 * 3600.0));
                statsList.add(stats);
            }
            return statsList;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> getUserTaskPerformanceStats() {
        String url = getCamundaUrl() + "/engine-rest/history/task?finished=true";
        RestTemplate restTemplate = getRestTemplate();
        try {
            List<Map<String, Object>> taskHistory = restTemplate.getForObject(url, List.class);
            if (taskHistory == null || taskHistory.isEmpty()) {
                return Collections.emptyList();
            }

            Map<String, List<Long>> userDurations = new HashMap<>();
            for (Map<String, Object> task : taskHistory) {
                String assignee = (String) task.get("assignee");
                if (assignee == null) assignee = "Chưa phân công";
                Object durationObj = task.get("duration");
                if (durationObj != null) {
                    long duration = ((Number) durationObj).longValue();
                    userDurations.computeIfAbsent(assignee, k -> new ArrayList<>()).add(duration);
                }
            }

            List<Map<String, Object>> performanceList = new ArrayList<>();
            for (Map.Entry<String, List<Long>> entry : userDurations.entrySet()) {
                String assignee = entry.getKey();
                List<Long> durations = entry.getValue();
                long total = 0;
                long min = Long.MAX_VALUE;
                long max = Long.MIN_VALUE;
                for (long d : durations) {
                    total += d;
                    if (d < min) min = d;
                    if (d > max) max = d;
                }
                double avg = (double) total / durations.size();
                Map<String, Object> stats = new HashMap<>();
                stats.put("assignee", assignee);
                stats.put("completedTasksCount", durations.size());
                stats.put("averageDurationMs", avg);
                stats.put("averageDurationHours", avg / (1000.0 * 3600.0));
                stats.put("minDurationHours", (double) min / (1000.0 * 3600.0));
                stats.put("maxDurationHours", (double) max / (1000.0 * 3600.0));
                performanceList.add(stats);
            }
            return performanceList;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
