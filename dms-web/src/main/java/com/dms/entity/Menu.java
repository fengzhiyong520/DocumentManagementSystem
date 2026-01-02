package com.dms.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 菜单实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class Menu extends BaseEntity {
    private String menuCode; // 菜单编码（唯一标识）
    private String menuName; // 菜单名称
    private String menuType; // 菜单类型：menu-菜单 button-按钮
    private String path; // 路由路径
    private String component; // 组件路径
    private String icon; // 图标
    private Long parentId; // 父菜单ID，0表示顶级菜单
    private Integer sortOrder; // 排序
    private Integer status; // 状态：0-禁用 1-启用
    private String description; // 描述
}

