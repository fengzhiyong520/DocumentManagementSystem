package com.dms.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dms.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程节点配置实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("process_node")
public class ProcessNode extends BaseEntity {
    private Long processDefinitionId; // 流程定义ID
    private String nodeId; // 节点ID（流程中的节点标识）
    private String nodeName; // 节点名称
    private String nodeType; // 节点类型：startEvent, userTask, endEvent�?
    private Integer sortOrder; // 排序
    private String assignee; // 审批人（用户ID，多个用逗号分隔�?
    private String candidateUsers; // 候选审批人（用户ID，多个用逗号分隔�?
    private String candidateGroups; // 候选审批组（角色ID，多个用逗号分隔�?
    private String formKey; // 表单Key
    private String description; // 节点描述
}

