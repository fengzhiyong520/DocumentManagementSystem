package com.dms.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 历史任务VO，包含审批结果和备注
 */
@Data
public class HistoricTaskVO {
    private String id; // 任务ID
    private String name; // 任务名称
    private String assignee; // 审批人ID
    private String assigneeName; // 审批人昵�?
    private String owner; // 任务所有�?
    private LocalDateTime startTime; // 开始时�?
    private LocalDateTime endTime; // 结束时间
    private Long durationInMillis; // 持续时间（毫秒）
    private String deleteReason; // 删除原因
    private String processInstanceId; // 流程实例ID
    private String processDefinitionId; // 流程定义ID
    private String processDefinitionKey; // 流程定义Key
    private String processDefinitionName; // 流程定义名称
    private String taskDefinitionKey; // 任务定义Key
    private Integer priority; // 优先�?
    private String category; // 分类
    private String formKey; // 表单Key
    private String parentTaskId; // 父任务ID
    private String description; // 描述
    
    // 审批结果和备注（从流程变量中获取�?
    private Boolean approved; // 审批结果：true-批准，false-拒绝，null-未审�?
    private String remark; // 审批备注
}

