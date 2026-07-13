package com.base.service;

public interface EmailVerificationService {
    void sendVerificationCode(String email, String contractName, Double amount, String description, String processInstanceId, String externalTaskId);
    boolean confirmVerificationCode(String email, String code, String decision);
    boolean isEmailVerified(String email);
    java.util.Optional<com.base.model.EmailVerification> getVerification(String email, String code);
}
