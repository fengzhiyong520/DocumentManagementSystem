package com.dms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dms.entity.ProcessNode;
import com.dms.mapper.ProcessNodeMapper;
import com.dms.service.ProcessNodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 流程节点服务实现
 */
@Service
@RequiredArgsConstructor
public class ProcessNodeServiceImpl extends ServiceImpl<ProcessNodeMapper, ProcessNode> implements ProcessNodeService {

    @Override
    public List<ProcessNode> getNodesByProcessDefinitionId(Long processDefinitionId) {
        return this.list(new LambdaQueryWrapper<ProcessNode>()
                .eq(ProcessNode::getProcessDefinitionId, processDefinitionId)
                .eq(ProcessNode::getDeleted, 0)
                .orderByAsc(ProcessNode::getSortOrder));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveNodes(Long processDefinitionId, List<ProcessNode> nodes) {
        if (nodes != null && !nodes.isEmpty()) {
            for (ProcessNode node : nodes) {
                node.setId(null); // 确保是新节点
                node.setProcessDefinitionId(processDefinitionId);
                node.setDeleted(0);
                this.save(node);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNodes(Long processDefinitionId, List<ProcessNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            // 如果节点列表为空，删除所有节�?
            this.remove(new LambdaQueryWrapper<ProcessNode>()
                    .eq(ProcessNode::getProcessDefinitionId, processDefinitionId));
            return;
        }
        
        // 获取现有的节点ID集合
        List<ProcessNode> existingNodes = this.list(new LambdaQueryWrapper<ProcessNode>()
                .eq(ProcessNode::getProcessDefinitionId, processDefinitionId)
                .eq(ProcessNode::getDeleted, 0));
        
        Set<Long> existingNodeIds = existingNodes.stream()
                .map(ProcessNode::getId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        
        // 处理传入的节点列�?
        Set<Long> incomingNodeIds = new HashSet<>();
        
        for (ProcessNode node : nodes) {
            node.setProcessDefinitionId(processDefinitionId);
            node.setDeleted(0);
            
            if (node.getId() != null && existingNodeIds.contains(node.getId())) {
                // 已有节点，直接更�?
                this.updateById(node);
                incomingNodeIds.add(node.getId());
            } else {
                // 新增节点，插�?
                node.setId(null);
                this.save(node);
                if (node.getId() != null) {
                    incomingNodeIds.add(node.getId());
                }
            }
        }
        
        // 删除不在传入列表中的节点（逻辑删除�?
        for (ProcessNode existingNode : existingNodes) {
            if (existingNode.getId() != null && !incomingNodeIds.contains(existingNode.getId())) {
                this.removeById(existingNode.getId());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNodesByProcessDefinitionId(Long processDefinitionId) {
        this.remove(new LambdaQueryWrapper<ProcessNode>()
                .eq(ProcessNode::getProcessDefinitionId, processDefinitionId));
    }
}

