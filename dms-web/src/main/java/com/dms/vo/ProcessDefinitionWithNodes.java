package com.dms.vo;

import com.dms.entity.ProcessDefinition;
import com.dms.entity.ProcessNode;
import lombok.Data;

import java.util.List;

/**
 * 流程定义及节�?VO
 */
@Data
public class ProcessDefinitionWithNodes {
    private ProcessDefinition processDefinition;
    private List<ProcessNode> nodes;
}

