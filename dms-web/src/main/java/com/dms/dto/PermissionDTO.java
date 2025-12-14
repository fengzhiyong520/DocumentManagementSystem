package com.dms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 权限DTO
 */
@Data
public class PermissionDTO {
    private Long id;
    
    @NotBlank(message = "权限编码不能为空")
    private String permissionCode;
    
    @NotBlank(message = "权限名称不能为空")
    private String permissionName;
    
    private String resource;
    private String method; // GET, POST, PUT, DELETE
    private String description;
}

