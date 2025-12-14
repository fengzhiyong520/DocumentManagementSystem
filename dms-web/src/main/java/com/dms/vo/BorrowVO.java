package com.dms.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 借阅VO
 */
@Data
public class BorrowVO {
    private Long id;
    private Long documentId;
    private String documentTitle;
    private Long userId;
    private String userName;
    private LocalDateTime borrowTime;
    private LocalDateTime returnTime;
    private LocalDateTime expectedReturnTime;
    private Integer status;
    private String reason;
    private String approverName;
    private String remark;
    private LocalDateTime createTime;
    // 流程相关字段
    private Long processDefinitionId;
    private String processInstanceId;
    private String taskId;
    private String taskName; // 当前任务名称
    private String taskAssignee; // 当前任务审批人ID
    private String taskAssigneeName; // 当前任务审批人昵�?
    private Boolean canApprove; // 当前用户是否可以审批（后端计算）
    private String processStatus; // 流程状态：待审批、审批中、审批完�?
}

