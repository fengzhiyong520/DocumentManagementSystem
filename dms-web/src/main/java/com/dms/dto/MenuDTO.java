package com.dms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 菜单DTO
 */
@Data
public class MenuDTO {
    private Long id;
    
    @NotBlank(message = "菜单编码不能为空")
    private String menuCode;
    
    @NotBlank(message = "菜单名称不能为空")
    private String menuName;
    
    private String menuType; // menu-菜单 button-按钮
    
    private String path; // 路由路径
    
    private String component; // 组件路径
    
    private String icon; // 图标
    
    private Long parentId; // 父菜单ID，0表示顶级菜单
    
    private Integer sortOrder; // 排序
    
    private Integer status; // 0-禁用 1-启用
    
    private String description; // 描述
}

