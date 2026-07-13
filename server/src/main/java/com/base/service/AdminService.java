package com.base.service;

import com.base.model.DmsDoc;
import com.base.model.Position;
import com.base.model.Role;
import com.base.model.User;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface AdminService {
    Map<String, Object> getStatsSummary();
    List<User> searchUsers(String query);
    User createUser(User user, String roleName, String positionName) throws Exception;
    User updateUser(Long id, User userDetails, String roleName, String positionName) throws Exception;
    User toggleUserStatus(Long id);
    void deleteUser(Long id);
    List<Role> getAllRoles();
    List<Position> getAllPositions();
    List<DmsDoc> getAllDocs(String status, String docType);
    DmsDoc updateSecurityLevel(UUID docId, String securityLevel);
    DmsDoc updateAccessControl(UUID docId, String securityLevel, String allowedRole, String allowedPositions);
    void deleteDoc(UUID docId);
}
