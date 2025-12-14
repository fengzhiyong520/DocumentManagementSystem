package com.dms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dms.entity.ProcessNode;

import java.util.List;

/**
 * 流程节点服务接口
 */
public interface ProcessNodeService extends IService<ProcessNode> {
    /**
     * 根据流程定义ID获取节点列表
     */
    List<ProcessNode> getNodesByProcessDefinitionId(Long processDefinitionId);

    /**
     * 保存流程节点列表
     */
    void saveNodes(Long processDefinitionId, List<ProcessNode> nodes);

    /**
     * 更新流程节点列表（先删除旧节点，再插入新节点�?
     */
    void updateNodes(Long processDefinitionId, List<ProcessNode> nodes);

    /**
     * 删除流程定义的所有节�?
     */
    void deleteNodesByProcessDefinitionId(Long processDefinitionId);
}

