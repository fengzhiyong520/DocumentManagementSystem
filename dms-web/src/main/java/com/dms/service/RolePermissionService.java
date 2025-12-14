package com.dms.service;

import java.util.List;

/**
 * 角色权限关联服务接口
 */
public interface RolePermissionService {
    /**
     * 获取角色的权限ID列表
     */
    List<Long> getPermissionIdsByRoleId(Long roleId);
    
    /**
     * 保存角色的权限配�?
     */
    void saveRolePermissions(Long roleId, List<Long> permissionIds);
}

