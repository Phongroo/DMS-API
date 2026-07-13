package com.base.service;

import com.base.model.DmsDocHistory;
import java.util.List;
import java.util.UUID;

public interface DmsDocHistoryService {
    void log(UUID docId, String action, String status);
    void log(UUID docId, String username, String action, String status);
    List<DmsDocHistory> getTimeline(UUID docId);
}
