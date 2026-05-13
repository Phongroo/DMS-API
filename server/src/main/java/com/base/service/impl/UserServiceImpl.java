package com.base.service.impl;

import com.base.model.User;
import com.base.model.UserRole;
import com.base.repo.RoleRepository;
import com.base.repo.UserRepository;
import com.base.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;


import org.springframework.stereotype.Service;

import java.util.Set;
@Service

public class UserServiceImpl implements UserService {


    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;

    @Override
    public User createUser(User user, Set<UserRole> userRole) throws Exception {
        User local = this.userRepository.findByUsername(user.getUsername());
        if (local != null) {
            System.out.println("User is already there");
            throw new Exception("User already present");
        } else {
            for (UserRole ur : userRole) {
                roleRepository.save(ur.getRole());
            }
            user.getUserRoles().addAll(userRole);
            local = this.userRepository.save(user);
        }
        return local;
    }

    @Override
    public User getUser(String username) {
        return this.userRepository.findByUsername(username);
    }

    @Override
    public void deleteUser(Long userId) {

        this.userRepository.deleteById(userId);

    }
}