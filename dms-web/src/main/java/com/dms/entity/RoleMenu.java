package com.dms.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色菜单关联实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role_menu")
public class RoleMenu extends BaseEntity {
    private Long roleId; // 角色ID
    private Long menuId; // 菜单ID
}

