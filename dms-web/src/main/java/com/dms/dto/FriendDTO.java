package com.dms.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 好友DTO
 */
@Data
public class FriendDTO {
    private Long id;
    
    @NotNull(message = "好友ID不能为空")
    private Long friendId;
    
    private String remark; // 备注名称
}

