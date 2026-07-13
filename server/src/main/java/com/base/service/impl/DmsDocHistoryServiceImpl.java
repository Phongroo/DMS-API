package com.base.service.impl;

import com.base.model.DmsDocHistory;
import com.base.repo.DmsDocHistoryRepository;
import com.base.service.DmsDocHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class DmsDocHistoryServiceImpl implements DmsDocHistoryService {

    @Autowired
    private DmsDocHistoryRepository dmsDocHistoryRepository;

    @Override
    public void log(UUID docId, String action, String status) {
        String username = "system";
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            username = SecurityContextHolder.getContext().getAuthentication().getName();
        }
        log(docId, username, action, status);
    }

    @Override
    public void log(UUID docId, String username, String action, String status) {
        DmsDocHistory event = new DmsDocHistory(docId, new Date(), username, action, status);
        dmsDocHistoryRepository.save(event);
    }

    @Override
    public List<DmsDocHistory> getTimeline(UUID docId) {
        return dmsDocHistoryRepository.findByDocIdOrderByTimestampAsc(docId);
    }
}
