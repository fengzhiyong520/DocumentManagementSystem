package com.dms.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 权限VO
 */
@Data
public class PermissionVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String permissionCode;
    private String permissionName;
    private String resource;
    private String method;
    private String description;
    private LocalDateTime createTime;
}

