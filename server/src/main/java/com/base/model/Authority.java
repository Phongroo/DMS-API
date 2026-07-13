package com.base.model;

import org.springframework.security.core.GrantedAuthority;

public class Authority implements GrantedAuthority {
    private String authority;
    private String position;
    private Long branchId;

    public Authority(String authority, String position) {
        this.authority = authority;
        this.position = position;
    }

    public Authority(String authority, String position, Long branchId) {
        this.authority = authority;
        this.position = position;
        this.branchId = branchId;
    }

    public Long getBranchId() {
        return branchId;
    }

    public void setBranchId(Long branchId) {
        this.branchId = branchId;
    }

    public void setBranchId(int branchId) {
        this.branchId = (long) branchId;
    }

    public Authority(String authority, String position, int branchId) {
        this.authority = authority;
        this.position = position;
        this.branchId = (long) branchId;
    }

    @Override
    public String getAuthority() {
        return this.authority;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }
}