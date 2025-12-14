package com.dms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dms.utils.PageUtils;
import com.dms.dto.RoleDTO;
import com.dms.entity.Role;
import com.dms.vo.RoleVO;

/**
 * 角色服务接口
 */
public interface RoleService extends IService<Role> {
    /**
     * 分页查询角色
     */
    PageUtils<RoleVO> pageRole(Long current, Long size, String keyword);

    /**
     * 根据ID获取角色
     */
    RoleVO getRoleById(Long id);

    /**
     * 创建角色
     */
    void createRole(RoleDTO roleDTO);

    /**
     * 更新角色
     */
    void updateRole(RoleDTO roleDTO);

    /**
     * 删除角色
     */
    void deleteRole(Long id);

    /**
     * 启用/禁用角色
     */
    void changeStatus(Long id, Integer status);
}

