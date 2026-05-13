package com.base.model;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "position")
public class Position {
    @Id
    private long positionId;
    private String positionName;

    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.EAGER,mappedBy = "position",orphanRemoval = true)
    private Set<UserRole> userRoles=new HashSet<>();
    public Position(){}

    public Position(long positionId, String positionName, Set<UserRole> userRoles) {
        this.positionId = positionId;
        this.positionName = positionName;
        this.userRoles = userRoles;
    }

    public long getPositionId() {
        return positionId;
    }

    public void setPositionId(long positionId) {
        this.positionId = positionId;
    }

    public String getPositionName() {
        return positionName;
    }

    public void setPositionName(String positionName) {
        this.positionName = positionName;
    }

    public Set<UserRole> getUserRoles() {
        return userRoles;
    }

    public void setUserRoles(Set<UserRole> userRoles) {
        this.userRoles = userRoles;
    }
}
