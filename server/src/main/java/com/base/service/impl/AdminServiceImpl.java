package com.base.service.impl;

import com.base.model.DmsDoc;
import com.base.model.Position;
import com.base.model.Role;
import com.base.model.User;
import com.base.model.UserRole;
import com.base.repo.DmsDocRepository;
import com.base.repo.PositionRepository;
import com.base.repo.RoleRepository;
import com.base.repo.UserRepository;
import com.base.repo.BranchRepository;
import com.base.service.AdminService;
import com.base.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private DmsDocRepository dmsDocRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private com.base.service.DmsDocHistoryService dmsDocHistoryService;

    @Override
    public Map<String, Object> getStatsSummary() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAccounts", userRepository.count());
        stats.put("activeAccounts", userRepository.countByEnable(true));
        stats.put("totalDocuments", dmsDocRepository.count());
        stats.put("pendingDocuments", dmsDocRepository.countPendingDocuments());

        try {
            List<DmsDoc> allDocs = dmsDocRepository.findAll();
            
            // 1. Group by security level
            Map<String, Long> securityStats = new HashMap<>();
            for (DmsDoc d : allDocs) {
                String sec = d.getSecurityLevel() != null ? d.getSecurityLevel() : "Nội bộ";
                securityStats.put(sec, securityStats.getOrDefault(sec, 0L) + 1);
            }
            stats.put("docsBySecurity", securityStats);

            // 2. Group by branch
            List<com.base.model.Branch> branches = branchRepository.findAll();
            Map<Long, String> branchMap = new HashMap<>();
            for (com.base.model.Branch b : branches) {
                branchMap.put(b.getId(), b.getName());
            }
            
            Map<String, Long> branchStats = new HashMap<>();
            for (DmsDoc d : allDocs) {
                String branchName = d.getBranchId() != null ? branchMap.getOrDefault(d.getBranchId(), "Văn phòng trung tâm") : "Chung / Không gán";
                branchStats.put(branchName, branchStats.getOrDefault(branchName, 0L) + 1);
            }
            stats.put("docsByBranch", branchStats);

            // 3. Group by month (createdDate)
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM");
            Map<String, Long> monthlyStats = new TreeMap<>();
            for (DmsDoc d : allDocs) {
                if (d.getCreatedDate() != null) {
                    String month = sdf.format(d.getCreatedDate());
                    monthlyStats.put(month, monthlyStats.getOrDefault(month, 0L) + 1);
                }
            }
            stats.put("docsByMonth", monthlyStats);
        } catch (Exception e) {
            stats.put("docsBySecurity", new HashMap<>());
            stats.put("docsByBranch", new HashMap<>());
            stats.put("docsByMonth", new HashMap<>());
        }

        return stats;
    }

    @Override
    public List<User> searchUsers(String query) {
        if (query == null || query.trim().isEmpty()) {
            return userRepository.findAll();
        }
        return userRepository.searchUsers(query);
    }

    @Override
    public User createUser(User user, String roleName, String positionName) throws Exception {
        User existing = userRepository.findByUsername(user.getUsername());
        if (existing != null) {
            throw new Exception("Username already exists");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        Role role = roleRepository.findByRoleName(roleName);
        if (role == null) {
            role = new Role();
            role.setRoleId(System.currentTimeMillis());
            role.setRoleName(roleName);
            role = roleRepository.save(role);
        }

        Position position = null;
        if (positionName != null && !positionName.trim().isEmpty()) {
            position = positionRepository.findByPositionName(positionName);
            if (position == null) {
                position = new Position();
                position.setPositionId(System.currentTimeMillis());
                position.setPositionName(positionName);
                position = positionRepository.save(position);
            }
        }
        user.setPosition(position);

        if (user.getBranchId() != null) {
            com.base.model.Branch branchObj = branchRepository.findById(user.getBranchId()).orElse(null);
            user.setBranch(branchObj);
        }

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);

        user.getUserRoles().add(userRole);
        User savedUser = userRepository.save(user);

        auditLogService.log("Thêm người dùng mới: " + user.getUsername() + " với quyền " + roleName, "INFO");

        return savedUser;
    }

    @Override
    public User updateUser(Long id, User userDetails, String roleName, String positionName) throws Exception {
        User user = userRepository.findById(id).orElseThrow(() -> new Exception("User not found"));

        user.setFirstName(userDetails.getFirstName());
        user.setLastName(userDetails.getLastName());
        user.setEmail(userDetails.getEmail());
        user.setPhone(userDetails.getPhone());
        user.setProfile(userDetails.getProfile());
        
        if (userDetails.getBranchId() != null) {
            com.base.model.Branch branchObj = branchRepository.findById(userDetails.getBranchId()).orElse(null);
            user.setBranch(branchObj);
        } else {
            user.setBranch(null);
        }

        if (userDetails.getPassword() != null && !userDetails.getPassword().trim().isEmpty() && !userDetails.getPassword().startsWith("$2a$")) {
            user.setPassword(passwordEncoder.encode(userDetails.getPassword()));
        }

        Role role = roleRepository.findByRoleName(roleName);
        if (role == null) {
            role = new Role();
            role.setRoleId(System.currentTimeMillis());
            role.setRoleName(roleName);
            role = roleRepository.save(role);
        }

        Position position = null;
        if (positionName != null && !positionName.trim().isEmpty()) {
            position = positionRepository.findByPositionName(positionName);
            if (position == null) {
                position = new Position();
                position.setPositionId(System.currentTimeMillis());
                position.setPositionName(positionName);
                position = positionRepository.save(position);
            }
        }
        user.setPosition(position);

        UserRole userRole = null;
        if (user.getUserRoles() != null && !user.getUserRoles().isEmpty()) {
            List<UserRole> list = new ArrayList<>(user.getUserRoles());
            userRole = list.get(0);
            userRole.setRole(role);
            if (list.size() > 1) {
                user.getUserRoles().clear();
                user.getUserRoles().add(userRole);
            }
        } else {
            userRole = new UserRole();
            userRole.setUser(user);
            userRole.setRole(role);
            user.getUserRoles().add(userRole);
        }

        User updated = userRepository.save(user);

        auditLogService.log("Cập nhật thông tin người dùng: " + user.getUsername(), "INFO");

        return updated;
    }

    @Override
    public User toggleUserStatus(Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user != null) {
            boolean currentStatus = user.isEnable();
            user.setEnable(!currentStatus);
            User saved = userRepository.save(user);

            String statusStr = saved.isEnable() ? "Mở khóa" : "Khóa";
            auditLogService.log(statusStr + " tài khoản người dùng: " + user.getUsername(), "INFO");
            return saved;
        }
        return null;
    }

    @Override
    public void deleteUser(Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user != null) {
            userRepository.deleteById(id);
            auditLogService.log("Xóa tài khoản người dùng: " + user.getUsername(), "WARN");
        }
    }

    @Override
    public List<Role> getAllRoles() {
        List<Role> roles = roleRepository.findAll();
        if (roles.isEmpty()) {
            String[] names = {"ADMIN", "NORMAL", "STAFF", "MANAGER", "DIRECTOR"};
            long id = 44;
            for (String name : names) {
                Role r = new Role(id++, name);
                roleRepository.save(r);
            }
            roles = roleRepository.findAll();
        }
        return roles;
    }

    @Override
    public List<Position> getAllPositions() {
        List<Position> positions = positionRepository.findAll();
        if (positions.isEmpty()) {
            String[] names = {"STAFF", "MANAGER", "DIRECTOR"};
            long id = 1;
            for (String name : names) {
                Position p = new Position(id++, name, new HashSet<>());
                positionRepository.save(p);
            }
            positions = positionRepository.findAll();
        }
        return positions;
    }

    @Override
    public List<DmsDoc> getAllDocs(String status, String docType) {
        String cleanStatus = (status == null || status.trim().isEmpty()) ? "ALL" : status;
        String cleanDocType = (docType == null || docType.trim().isEmpty()) ? "ALL" : docType;
        return dmsDocRepository.findByStatusAndDocType(cleanStatus, cleanDocType);
    }

    @Override
    public DmsDoc updateSecurityLevel(UUID docId, String securityLevel) {
        DmsDoc doc = dmsDocRepository.findById(docId).orElse(null);
        if (doc != null) {
            String oldLevel = doc.getSecurityLevel();
            doc.setSecurityLevel(securityLevel);
            DmsDoc saved = dmsDocRepository.save(doc);

            dmsDocHistoryService.log(docId, "Thay đổi độ bảo mật của tệp từ \"" + oldLevel + "\" sang \"" + securityLevel + "\"", doc.getStatus());
            auditLogService.log("Thay đổi độ bảo mật tài liệu " + doc.getDocId() + " từ " + oldLevel + " sang " + securityLevel, "INFO");
            return saved;
        }
        return null;
    }

    @Override
    public DmsDoc updateAccessControl(UUID docId, String securityLevel, String allowedRole, String allowedPositions) {
        DmsDoc doc = dmsDocRepository.findById(docId).orElse(null);
        if (doc != null) {
            String oldLevel = doc.getSecurityLevel();
            String oldRole = doc.getAllowedRole();
            String oldPositions = doc.getAllowedPositions();

            doc.setSecurityLevel(securityLevel);
            doc.setAllowedRole(allowedRole);
            doc.setAllowedPositions(allowedPositions);
            DmsDoc saved = dmsDocRepository.save(doc);

            StringBuilder historyMsg = new StringBuilder();
            historyMsg.append("Cập nhật quyền truy cập tệp:");
            if (!Objects.equals(oldLevel, securityLevel)) {
                historyMsg.append(" [Độ bảo mật: ").append(oldLevel).append(" -> ").append(securityLevel).append("]");
            }
            if (!Objects.equals(oldRole, allowedRole)) {
                historyMsg.append(" [Vai trò: ").append(oldRole != null ? oldRole : "Tất cả").append(" -> ").append(allowedRole != null && !allowedRole.isEmpty() ? allowedRole : "Tất cả").append("]");
            }
            if (!Objects.equals(oldPositions, allowedPositions)) {
                historyMsg.append(" [Chức vụ: ").append(oldPositions != null ? oldPositions : "Tất cả").append(" -> ").append(allowedPositions != null && !allowedPositions.isEmpty() ? allowedPositions : "Tất cả").append("]");
            }

            dmsDocHistoryService.log(docId, historyMsg.toString(), doc.getStatus());
            auditLogService.log("Cập nhật quyền truy cập tài liệu " + doc.getDocId() + ": " + historyMsg, "INFO");
            return saved;
        }
        return null;
    }

    @Override
    public void deleteDoc(UUID docId) {
        DmsDoc doc = dmsDocRepository.findById(docId).orElse(null);
        if (doc != null) {
            dmsDocRepository.deleteById(docId);
            auditLogService.log("Xóa tài liệu " + doc.getDocId() + " khỏi kho DMS", "WARN");
        }
    }
}
