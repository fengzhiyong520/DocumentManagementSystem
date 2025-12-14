package com.dms.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 资料实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("file_document")
public class Document extends BaseEntity {
    private String title;
    private String description;
    private String category; // 分类
    private String fileType; // 文件类型：image, text, file
    private String fileUrl; // 文件URL
    private String fileName; // 文件�?
    private Long fileSize; // 文件大小（字节）
    private Integer isPublic; // 0-私有 1-公开
    private Long uploadUserId; // 上传用户ID
    private Integer status; // 0-禁用 1-启用
}

