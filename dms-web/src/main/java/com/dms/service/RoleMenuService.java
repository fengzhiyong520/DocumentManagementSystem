package com.dms.service;

import java.util.List;

/**
 * 角色菜单关联服务接口
 */
public interface RoleMenuService {
    /**
     * 获取角色的菜单ID列表
     */
    List<Long> getMenuIdsByRoleId(Long roleId);
    
    /**
     * 保存角色的菜单配置
     */
    void saveRoleMenus(Long roleId, List<Long> menuIds);
    
    /**
     * 根据用户角色ID列表获取菜单ID列表
     */
    List<Long> getMenuIdsByRoleIds(List<Long> roleIds);
}

