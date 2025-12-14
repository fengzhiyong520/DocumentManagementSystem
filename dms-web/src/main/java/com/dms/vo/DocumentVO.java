package com.dms.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 资料VO
 */
@Data
public class DocumentVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String title;
    private String description;
    private String category;
    private String fileType;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private Integer isPublic;
    private String uploadUserName;
    private Integer status;
    private LocalDateTime createTime;
}

