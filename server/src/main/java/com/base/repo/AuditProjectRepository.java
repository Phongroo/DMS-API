package com.base.repo;

import com.base.model.AuditProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuditProjectRepository extends JpaRepository<AuditProject, Long> {
    Optional<AuditProject> findByProjectCode(String projectCode);
    List<AuditProject> findByStatus(String status);
    List<AuditProject> findByStandardContainingIgnoreCase(String standard);
    List<AuditProject> findByCompanyNameContainingIgnoreCase(String companyName);
}
