package com.base.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "position")
public class Position {
    @Id
    private long positionId;
    private String positionName;

    @JsonIgnore
    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.EAGER,mappedBy = "position",orphanRemoval = true)
    private Set<User> users=new HashSet<>();
    public Position(){}

    public Position(long positionId, String positionName, Set<User> users) {
        this.positionId = positionId;
        this.positionName = positionName;
        this.users = users;
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

    public Set<User> getUsers() {
        return users;
    }

    public void setUsers(Set<User> users) {
        this.users = users;
    }
}
