package com.dms.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 权限实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_permission")
public class Permission extends BaseEntity {
    private String permissionCode;
    private String permissionName;
    private String resource;
    private String method; // GET, POST, PUT, DELETE
    private String description;
}

