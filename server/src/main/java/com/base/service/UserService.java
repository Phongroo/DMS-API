package com.base.service;

import com.base.model.User;
import com.base.model.UserRole;

import java.util.Set;

public interface UserService {
    public User createUser(User user, Set<UserRole> userRole) throws Exception;
    public User getUser(String username);
    public void deleteUser(Long userId);


}
