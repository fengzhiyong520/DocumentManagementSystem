package com.dms.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 角色VO
 */
@Data
public class RoleVO {
    private Long id;
    private String roleCode;
    private String roleName;
    private String description;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

