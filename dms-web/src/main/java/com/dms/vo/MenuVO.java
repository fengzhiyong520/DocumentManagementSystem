package com.dms.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜单VO
 */
@Data
public class MenuVO {
    private Long id;
    private String menuCode;
    private String menuName;
    private String menuType;
    private String path;
    private String component;
    private String icon;
    private Long parentId;
    private Integer sortOrder;
    private Integer status;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    // 子菜单列表（用于树形结构）
    private List<MenuVO> children;
}

