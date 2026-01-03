package com.dms.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 好友VO
 */
@Data
public class FriendVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    
    @JsonSerialize(using = ToStringSerializer.class)
    private Long friendId;
    
    private String remark; // 备注名称
    
    // 好友信息（申请者信息，当查看收到的申请时使用）
    private String friendUsername;
    private String friendNickname;
    private String friendEmail;
    private String friendPhone;
    private String friendAvatar;
    private Integer friendStatus;
    
    // 申请者信息（当查看收到的申请时，friendId是申请者的ID）
    private String applicantUsername;
    private String applicantNickname;
    private String applicantEmail;
    private String applicantPhone;
    private String applicantAvatar;
    private Integer applicantStatus;
    
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

