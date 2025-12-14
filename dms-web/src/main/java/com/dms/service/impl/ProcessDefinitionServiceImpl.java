package com.dms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dms.utils.PageUtils;
import com.dms.entity.ProcessDefinition;
import com.dms.entity.ProcessNode;
import com.dms.mapper.ProcessDefinitionMapper;
import com.dms.service.BpmnGeneratorService;
import com.dms.service.ProcessDefinitionService;
import com.dms.service.ProcessNodeService;
import com.dms.vo.ProcessDefinitionWithNodes;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 流程定义服务实现
 */
@Service
@RequiredArgsConstructor
public class ProcessDefinitionServiceImpl extends ServiceImpl<ProcessDefinitionMapper, ProcessDefinition> implements ProcessDefinitionService {

    private final ProcessNodeService processNodeService;
    private final BpmnGeneratorService bpmnGeneratorService;

    // RepositoryService 注入（Flowable 自动配置后可用）
    // 如果 Flowable 未正确初始化，此字段可能为 null
    @Autowired(required = false)
    private RepositoryService repositoryService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveProcessDefinition(ProcessDefinition processDefinition) {
        // 数据验证
        if (processDefinition.getName() == null || processDefinition.getName().trim().isEmpty()) {
            throw new RuntimeException("流程名称不能为空");
        }
        if (processDefinition.getKey() == null || processDefinition.getKey().trim().isEmpty()) {
            throw new RuntimeException("流程Key不能为空");
        }
        // 设置默认值
        if (processDefinition.getStatus() == null) {
            processDefinition.setStatus(1); // 默认启用
        }
        if (processDefinition.getDescription() == null) {
            processDefinition.setDescription("");
        }
        if (processDefinition.getCategory() == null) {
            processDefinition.setCategory("");
        }
        this.save(processDefinition);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProcessDefinition(ProcessDefinition processDefinition) {
        // 数据验证
        if (processDefinition.getId() == null) {
            throw new RuntimeException("流程定义ID不能为空");
        }
        if (processDefinition.getName() == null || processDefinition.getName().trim().isEmpty()) {
            throw new RuntimeException("流程名称不能为空");
        }
        if (processDefinition.getKey() == null || processDefinition.getKey().trim().isEmpty()) {
            throw new RuntimeException("流程Key不能为空");
        }
        // 设置默认值
        if (processDefinition.getStatus() == null) {
            processDefinition.setStatus(1); // 默认启用
        }
        if (processDefinition.getDescription() == null) {
            processDefinition.setDescription("");
        }
        if (processDefinition.getCategory() == null) {
            processDefinition.setCategory("");
        }
        this.updateById(processDefinition);
    }

    @Override
    public List<ProcessDefinition> getEnabledProcessDefinitions() {
        return this.list(new LambdaQueryWrapper<ProcessDefinition>()
                .eq(ProcessDefinition::getStatus, 1)
                .eq(ProcessDefinition::getDeleted, 0));
    }

    @Override
    public ProcessDefinition getByKey(String key) {
        return this.getOne(new LambdaQueryWrapper<ProcessDefinition>()
                .eq(ProcessDefinition::getKey, key)
                .eq(ProcessDefinition::getDeleted, 0));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deployProcessDefinition(Long id) {
        if (repositoryService == null) {
            throw new RuntimeException("Flowable 未正确配置，无法部署流程定义");
        }

        ProcessDefinition processDefinition = this.getById(id);
        if (processDefinition == null) {
            throw new RuntimeException("流程定义不存在");
        }

        List<ProcessNode> nodes = processNodeService.getNodesByProcessDefinitionId(id);

        if (nodes == null || nodes.isEmpty()) {
            throw new RuntimeException("流程节点配置为空，无法部署");
        }

        // 生成 BPMN XML
        String bpmnXml = bpmnGeneratorService.generateBpmnXml(processDefinition, nodes);

        // 部署到 Flowable
        Deployment deployment = repositoryService.createDeployment()
                .name(processDefinition.getName())
                .addInputStream(
                        processDefinition.getKey() + ".bpmn20.xml",
                        new ByteArrayInputStream(bpmnXml.getBytes(StandardCharsets.UTF_8))
                )
                .deploy();

        // 更新流程定义的部署信息
        processDefinition.setDeploymentId(deployment.getId());
        org.flowable.engine.repository.ProcessDefinition flowableDef = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId())
                .singleResult();
        if (flowableDef != null) {
            processDefinition.setFlowableDefinitionId(flowableDef.getId());
        }
        this.updateById(processDefinition);
    }

    @Override
    public ProcessDefinitionWithNodes getProcessDefinitionWithNodesVO(Long id) {
        ProcessDefinition processDefinition = this.getById(id);
        if (processDefinition == null) {
            return null;
        }

        List<ProcessNode> nodes = processNodeService.getNodesByProcessDefinitionId(id);

        ProcessDefinitionWithNodes vo = new ProcessDefinitionWithNodes();
        vo.setProcessDefinition(processDefinition);
        vo.setNodes(nodes);
        return vo;
    }

    @Override
    public PageUtils<ProcessDefinition> pageProcessDefinition(Long current, Long size, String keyword) {
        Page<ProcessDefinition> page = new Page<>(current, size);
        LambdaQueryWrapper<ProcessDefinition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProcessDefinition::getDeleted, 0);
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like(ProcessDefinition::getName, keyword)
                    .or().like(ProcessDefinition::getKey, keyword)
                    .or().like(ProcessDefinition::getDescription, keyword));
        }
        wrapper.orderByDesc(ProcessDefinition::getCreateTime);

        Page<ProcessDefinition> result = this.page(page, wrapper);
        PageUtils<ProcessDefinition> pageUtils = new PageUtils<>();
        pageUtils.setCurrent(result.getCurrent());
        pageUtils.setSize(result.getSize());
        pageUtils.setTotal(result.getTotal());
        pageUtils.setRecords(result.getRecords());
        return pageUtils;
    }
}

