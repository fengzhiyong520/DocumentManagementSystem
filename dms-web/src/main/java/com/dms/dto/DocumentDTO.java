package com.dms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 资料DTO
 */
@Data
public class DocumentDTO {
    private Long id;
    
    @NotBlank(message = "标题不能为空")
    private String title;
    
    private String description;
    private String category;
    private String fileType;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private Integer isPublic;
    private Integer status;
}

