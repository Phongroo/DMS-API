package com.base.repo;

import com.base.model.AuditNcr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditNcrRepository extends JpaRepository<AuditNcr, Long> {
    List<AuditNcr> findByProjectId(Long projectId);
    List<AuditNcr> findByProjectIdAndStatus(Long projectId, String status);
    List<AuditNcr> findByProjectIdAndSeverity(Long projectId, String severity);
    long countByStatus(String status);
}
