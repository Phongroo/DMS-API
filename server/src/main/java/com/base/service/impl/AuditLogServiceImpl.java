package com.base.service.impl;

import com.base.model.AuditLog;
import com.base.model.SystemSettings;
import com.base.repo.AuditLogRepository;
import com.base.repo.SystemSettingsRepository;
import com.base.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private SystemSettingsRepository systemSettingsRepository;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @Override
    public AuditLog save(AuditLog log) {
        SystemSettings settings = systemSettingsRepository.findById(1L).orElse(new SystemSettings());
        if (!settings.isSystemMonitoring() && !"WARN".equals(log.getLevel())) {
            return log;
        }

        AuditLog saved = auditLogRepository.save(log);

        // Broadcast to SSE clients
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("audit-log").data(saved));
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
        return saved;
    }

    @Override
    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findAll(Sort.by(Sort.Direction.DESC, "timestamp"));
    }

    @Override
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(24 * 60 * 60 * 1000L); // 24 hours
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((e) -> emitters.remove(emitter));
        try {
            emitter.send(SseEmitter.event().name("ping").data("connected"));
        } catch (IOException e) {
            emitters.remove(emitter);
        }
        return emitter;
    }

    @Override
    public void log(String action, String level) {
        String username = "system";
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            username = SecurityContextHolder.getContext().getAuthentication().getName();
        }

        String ip = "unknown";
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
        }

        log(username, ip, action, level);
    }

    @Override
    public void log(String username, String ip, String action, String level) {
        AuditLog auditLog = new AuditLog(new Date(), username, ip, action, level);
        save(auditLog);
    }
}
