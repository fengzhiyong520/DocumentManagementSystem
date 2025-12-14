package com.dms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dms.utils.PageUtils;
import com.dms.dto.PermissionDTO;
import com.dms.entity.Permission;
import com.dms.vo.PermissionVO;

import java.util.List;

/**
 * 权限服务接口
 */
public interface PermissionService extends IService<Permission> {
    /**
     * 分页查询权限
     */
    PageUtils<PermissionVO> pagePermission(Long current, Long size, String keyword);
    
    /**
     * 获取所有权限列�?
     */
    List<PermissionVO> listAllPermissions();
    
    /**
     * 根据ID获取权限
     */
    PermissionVO getPermissionById(Long id);
    
    /**
     * 创建权限
     */
    void createPermission(PermissionDTO permissionDTO);
    
    /**
     * 更新权限
     */
    void updatePermission(PermissionDTO permissionDTO);
    
    /**
     * 删除权限
     */
    void deletePermission(Long id);
}

