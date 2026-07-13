package com.base.repo;

import com.base.model.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, UUID> {
    Optional<EmailVerification> findByEmailAndCode(String email, String code);
    Optional<EmailVerification> findTopByEmailAndVerifiedTrueOrderByExpiryTimeDesc(String email);
}
