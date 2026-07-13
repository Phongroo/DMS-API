package com.base.repo;

import com.base.model.DmsDoc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
@Repository
public interface DmsDocRepository extends JpaRepository<DmsDoc, UUID> {
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(d) FROM DmsDoc d WHERE d.status LIKE 'PENDING_%'")
    long countPendingDocuments();

    java.util.List<DmsDoc> findByStatus(String status);

    DmsDoc findByProcessInstanceId(String processInstanceId);

    @org.springframework.data.jpa.repository.Query("SELECT d FROM DmsDoc d WHERE " +
           "(:status IS NULL OR :status = 'ALL' OR d.status = :status) AND " +
           "(:docType IS NULL OR :docType = 'ALL' OR d.docType = :docType)")
    java.util.List<DmsDoc> findByStatusAndDocType(
            @org.springframework.data.repository.query.Param("status") String status,
            @org.springframework.data.repository.query.Param("docType") String docType
    );
}
