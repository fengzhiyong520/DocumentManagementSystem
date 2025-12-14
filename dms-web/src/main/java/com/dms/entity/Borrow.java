package com.dms.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * 借阅实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("file_borrow")
public class Borrow extends BaseEntity {
    private Long documentId; // 资料ID
    private Long userId; // 借阅用户ID
    private LocalDateTime borrowTime; // 借阅时间
    private LocalDateTime returnTime; // 归还时间
    private LocalDateTime expectedReturnTime; // 预计归还时间
    private Integer status; // 0-待审�?1-已批�?2-已拒�?3-已归�?4-已逾期
    private String reason; // 借阅原因
    private Long approverId; // 审批人ID
    private String remark; // 备注
    private Long processDefinitionId; // 流程定义ID
    private String processInstanceId; // 流程实例ID
    private String taskId; // 当前任务ID
}

