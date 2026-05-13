package com.base.model;

import org.springframework.security.core.GrantedAuthority;

public class Authority implements GrantedAuthority {
    private String authority;
    private String position;

    public Authority(String authority, String position) {
        this.authority = authority;
        this.position = position;
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