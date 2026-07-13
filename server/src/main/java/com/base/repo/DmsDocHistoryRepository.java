package com.base.repo;

import com.base.model.DmsDocHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DmsDocHistoryRepository extends JpaRepository<DmsDocHistory, Long> {
    List<DmsDocHistory> findByDocIdOrderByTimestampAsc(UUID docId);
}
