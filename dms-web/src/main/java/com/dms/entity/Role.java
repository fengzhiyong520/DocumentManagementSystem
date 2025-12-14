package com.dms.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class Role extends BaseEntity {
    private String roleCode;
    private String roleName;
    private String description;
    private Integer status; // 0-禁用 1-启用
}

