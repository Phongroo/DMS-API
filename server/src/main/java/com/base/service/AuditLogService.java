package com.base.service;

import com.base.model.AuditLog;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.List;

public interface AuditLogService {
    AuditLog save(AuditLog log);
    List<AuditLog> getAllLogs();
    SseEmitter subscribe();
    void log(String action, String level); // automatically pulls current user and remote IP from context
    void log(String username, String ip, String action, String level);
}
