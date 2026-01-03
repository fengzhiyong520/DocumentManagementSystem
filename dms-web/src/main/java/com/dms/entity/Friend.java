package com.dms.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 好友实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_friend")
public class Friend extends BaseEntity {
    private Long userId; // 用户ID（当前用户）
    private Long friendId; // 好友ID
    private String remark; // 备注名称
    private Integer status; // 状态：0-已删除 1-正常（已同意） 2-待同意（申请中）
}

