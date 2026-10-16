package com.base.repo;

import com.base.model.AuditSample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditSampleRepository extends JpaRepository<AuditSample, Long> {
    List<AuditSample> findByProjectId(Long projectId);
    List<AuditSample> findByProjectIdAndTestResult(Long projectId, String testResult);
    long countByTestResult(String testResult);
}
