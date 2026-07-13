package com.base.service.impl;

import com.base.model.SystemSettings;
import com.base.repo.SystemSettingsRepository;
import com.base.service.SystemSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SystemSettingsServiceImpl implements SystemSettingsService {

    @Autowired
    private SystemSettingsRepository systemSettingsRepository;

    @Override
    public SystemSettings getSettings() {
        return systemSettingsRepository.findById(1L).orElseGet(() -> {
            SystemSettings defaultSettings = new SystemSettings();
            return systemSettingsRepository.save(defaultSettings);
        });
    }

    @Override
    public SystemSettings updateSettings(SystemSettings settings) {
        settings.setId(1L);
        return systemSettingsRepository.save(settings);
    }
}
