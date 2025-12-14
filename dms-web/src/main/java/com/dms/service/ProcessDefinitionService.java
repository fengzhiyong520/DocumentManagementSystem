package com.dms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dms.entity.ProcessDefinition;
import com.dms.utils.PageUtils;
import com.dms.vo.ProcessDefinitionWithNodes;

import java.util.List;

/**
 * 流程定义服务接口
 */
public interface ProcessDefinitionService extends IService<ProcessDefinition> {
    /**
     * 保存流程定义
     */
    void saveProcessDefinition(ProcessDefinition processDefinition);

    /**
     * 更新流程定义
     */
    void updateProcessDefinition(ProcessDefinition processDefinition);

    /**
     * 获取启用的流程定义列�?
     */
    List<ProcessDefinition> getEnabledProcessDefinitions();

    /**
     * 分页查询流程定义列表
     */
    PageUtils<ProcessDefinition> pageProcessDefinition(Long current, Long size, String keyword);

    /**
     * 根据流程Key获取流程定义
     */
    ProcessDefinition getByKey(String key);

    /**
     * 部署流程定义（生成并部署 BPMN�?
     */
    void deployProcessDefinition(Long id);

    /**
     * 获取流程定义及其节点列表（返回包含节点的对象�?
     */
    ProcessDefinitionWithNodes getProcessDefinitionWithNodesVO(Long id);
}

