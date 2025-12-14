package com.dms.service.impl;

import com.dms.mapper.RolePermissionMapper;
import com.dms.service.RolePermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 角色权限关联服务实现
 */
@Service
@RequiredArgsConstructor
public class RolePermissionServiceImpl implements RolePermissionService {
    
    private final RolePermissionMapper rolePermissionMapper;
    
    @Override
    public List<Long> getPermissionIdsByRoleId(Long roleId) {
        return rolePermissionMapper.selectPermissionIdsByRoleId(roleId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRolePermissions(Long roleId, List<Long> permissionIds) {
        // 先删除原有权限关�?
        rolePermissionMapper.deleteByRoleId(roleId);
        // 如果有新权限，则批量插入
        if (permissionIds != null && !permissionIds.isEmpty()) {
            rolePermissionMapper.insertBatch(roleId, permissionIds);
        }
    }
}

