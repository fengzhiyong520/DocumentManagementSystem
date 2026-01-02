package com.dms.service.impl;

import com.dms.mapper.RoleMenuMapper;
import com.dms.service.RoleMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 角色菜单关联服务实现
 */
@Service
@RequiredArgsConstructor
public class RoleMenuServiceImpl implements RoleMenuService {
    
    private final RoleMenuMapper roleMenuMapper;
    
    @Override
    public List<Long> getMenuIdsByRoleId(Long roleId) {
        return roleMenuMapper.selectMenuIdsByRoleId(roleId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRoleMenus(Long roleId, List<Long> menuIds) {
        // 先删除原有菜单关联
        roleMenuMapper.deleteByRoleId(roleId);
        // 如果有新菜单，则批量插入
        if (menuIds != null && !menuIds.isEmpty()) {
            roleMenuMapper.insertBatch(roleId, menuIds);
        }
    }
    
    @Override
    public List<Long> getMenuIdsByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        return roleMenuMapper.selectMenuIdsByRoleIds(roleIds);
    }
}

