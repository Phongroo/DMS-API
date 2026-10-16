package com.base.repo;

import com.base.model.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    Optional<Certificate> findByCertificateNumber(String certificateNumber);
    Optional<Certificate> findByQrVerificationCode(String qrVerificationCode);
    Optional<Certificate> findByProjectId(Long projectId);
    List<Certificate> findByStatus(String status);
    List<Certificate> findByCompanyNameContainingIgnoreCase(String companyName);
    long countByStatus(String status);
}
