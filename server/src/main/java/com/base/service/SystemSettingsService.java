package com.base.service;

import com.base.model.SystemSettings;

public interface SystemSettingsService {
    SystemSettings getSettings();
    SystemSettings updateSettings(SystemSettings settings);
}
