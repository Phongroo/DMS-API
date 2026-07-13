package com.base.model;

import javax.persistence.*;

@Entity
@Table(name = "system_settings")
public class SystemSettings {
    @Id
    private Long id = 1L; // We only need a single row for global system settings

    private String camundaUrl = "http://host.docker.internal:30081";
    private String socketHost = "http://localhost:3000";
    private Integer apiTimeout = 5000; // in milliseconds

    private boolean requireDigitalSignature = false;
    private boolean sendAutoEmail = false;
    private boolean systemMonitoring = true;

    private String defaultTheme = "spring";

    @Column(columnDefinition = "TEXT")
    private String menuPermissions;

    public SystemSettings() {}

    public String getDefaultTheme() {
        return defaultTheme;
    }

    public void setDefaultTheme(String defaultTheme) {
        this.defaultTheme = defaultTheme;
    }

    public String getMenuPermissions() {
        return menuPermissions;
    }

    public void setMenuPermissions(String menuPermissions) {
        this.menuPermissions = menuPermissions;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCamundaUrl() {
        return camundaUrl;
    }

    public void setCamundaUrl(String camundaUrl) {
        this.camundaUrl = camundaUrl;
    }

    public String getSocketHost() {
        return socketHost;
    }

    public void setSocketHost(String socketHost) {
        this.socketHost = socketHost;
    }

    public Integer getApiTimeout() {
        return apiTimeout;
    }

    public void setApiTimeout(Integer apiTimeout) {
        this.apiTimeout = apiTimeout;
    }

    public boolean isRequireDigitalSignature() {
        return requireDigitalSignature;
    }

    public void setRequireDigitalSignature(boolean requireDigitalSignature) {
        this.requireDigitalSignature = requireDigitalSignature;
    }

    public boolean isSendAutoEmail() {
        return sendAutoEmail;
    }

    public void setSendAutoEmail(boolean sendAutoEmail) {
        this.sendAutoEmail = sendAutoEmail;
    }

    public boolean isSystemMonitoring() {
        return systemMonitoring;
    }

    public void setSystemMonitoring(boolean systemMonitoring) {
        this.systemMonitoring = systemMonitoring;
    }
}
