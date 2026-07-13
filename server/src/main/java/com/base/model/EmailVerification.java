package com.base.model;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "email_verifications")
public class EmailVerification {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private Date expiryTime;

    private boolean verified = false;

    @Column(name = "contract_name")
    private String contractName;

    private Double amount;

    @Column(length = 2000)
    private String description;

    private String decision;

    @Column(name = "process_instance_id")
    private String processInstanceId;

    @Column(name = "external_task_id")
    private String externalTaskId;

    public EmailVerification() {}

    public EmailVerification(String email, String code, Date expiryTime) {
        this.email = email;
        this.code = code;
        this.expiryTime = expiryTime;
        this.verified = false;
    }

    public EmailVerification(String email, String code, Date expiryTime, String contractName, Double amount, String description) {
        this.email = email;
        this.code = code;
        this.expiryTime = expiryTime;
        this.contractName = contractName;
        this.amount = amount;
        this.description = description;
        this.verified = false;
    }

    public EmailVerification(String email, String code, Date expiryTime, String contractName, Double amount, String description, String processInstanceId) {
        this.email = email;
        this.code = code;
        this.expiryTime = expiryTime;
        this.contractName = contractName;
        this.amount = amount;
        this.description = description;
        this.processInstanceId = processInstanceId;
        this.verified = false;
    }

    public EmailVerification(String email, String code, Date expiryTime, String contractName, Double amount, String description, String processInstanceId, String externalTaskId) {
        this.email = email;
        this.code = code;
        this.expiryTime = expiryTime;
        this.contractName = contractName;
        this.amount = amount;
        this.description = description;
        this.processInstanceId = processInstanceId;
        this.externalTaskId = externalTaskId;
        this.verified = false;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Date getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(Date expiryTime) {
        this.expiryTime = expiryTime;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public String getContractName() {
        return contractName;
    }

    public void setContractName(String contractName) {
        this.contractName = contractName;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getExternalTaskId() {
        return externalTaskId;
    }

    public void setExternalTaskId(String externalTaskId) {
        this.externalTaskId = externalTaskId;
    }
}
