package com.dms.service;

import com.dms.entity.ProcessDefinition;
import com.dms.entity.ProcessNode;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * BPMN 动态生成服�?
 */
@Service
public class BpmnGeneratorService {

    /**
     * 根据流程定义和节点配置生�?BPMN XML
     */
    public String generateBpmnXml(ProcessDefinition processDefinition, List<ProcessNode> nodes) {
        StringBuilder bpmnXml = new StringBuilder();
        
        // BPMN 2.0 命名空间和根元素
        bpmnXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        bpmnXml.append("<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n");
        bpmnXml.append("             xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        bpmnXml.append("             xmlns:flowable=\"http://flowable.org/bpmn\"\n");
        bpmnXml.append("             targetNamespace=\"http://flowable.org/bpmn\"\n");
        bpmnXml.append("             typeLanguage=\"http://www.w3.org/2001/XMLSchema\"\n");
        bpmnXml.append("             expressionLanguage=\"http://www.w3.org/1999/XPath\">\n\n");
        
        // 规范化流程Key，确保符合NCName规范
        String processKey = normalizeId(processDefinition.getKey());
        
        // 流程定义
        bpmnXml.append("  <process id=\"").append(processKey).append("\"");
        bpmnXml.append(" name=\"").append(escapeXml(processDefinition.getName())).append("\"");
        bpmnXml.append(" isExecutable=\"true\">\n");
        
        // 开始事�?
        ProcessNode startNode = nodes.stream()
                .filter(node -> "startEvent".equals(node.getNodeType()))
                .findFirst()
                .orElse(null);
        
        String startNodeId;
        if (startNode != null) {
            startNodeId = normalizeId(startNode.getNodeId());
            bpmnXml.append("    <startEvent id=\"").append(startNodeId).append("\"");
            bpmnXml.append(" name=\"").append(escapeXml(startNode.getNodeName())).append("\"/>\n");
        } else {
            // 如果没有开始节点，创建一个默认的
            startNodeId = "startEvent";
            bpmnXml.append("    <startEvent id=\"").append(startNodeId).append("\" name=\"开始\"/>\n");
        }
        
        // 用户任务节点（按排序顺序�?
        List<ProcessNode> userTasks = nodes.stream()
                .filter(node -> "userTask".equals(node.getNodeType()))
                .sorted((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
                .toList();
        
        String previousNodeId = startNodeId;
        
        for (int i = 0; i < userTasks.size(); i++) {
            ProcessNode task = userTasks.get(i);
            String taskId = normalizeId(task.getNodeId());
            
            // 创建用户任务
            bpmnXml.append("    <userTask id=\"").append(taskId).append("\"");
            bpmnXml.append(" name=\"").append(escapeXml(task.getNodeName())).append("\"");
            
            // 设置审批人（优先级：assignee > candidateUsers > candidateGroups�?
            String assignee = task.getAssignee();
            String candidateUsers = task.getCandidateUsers();
            String candidateGroups = task.getCandidateGroups();
            
            // 处理审批人：如果 assignee 不为空，优先使用 assignee
            if (assignee != null && !assignee.trim().isEmpty()) {
                // 如果 assignee 包含逗号（多个用户），应该使�?candidateUsers 属�?
                // 但这里我们直接使�?assignee 的值作�?candidateUsers
                if (assignee.contains(",")) {
                    // 多个用户，使�?candidateUsers 属�?
                    bpmnXml.append(" flowable:candidateUsers=\"").append(escapeXml(assignee.trim())).append("\"");
                } else {
                    // 单个用户，使�?assignee 属�?
                    bpmnXml.append(" flowable:assignee=\"").append(escapeXml(assignee.trim())).append("\"");
                }
            } else if (candidateUsers != null && !candidateUsers.trim().isEmpty()) {
                // 如果没有 assignee，使�?candidateUsers
                bpmnXml.append(" flowable:candidateUsers=\"").append(escapeXml(candidateUsers.trim())).append("\"");
            }
            
            // 设置候选组（可以与候选用户同时存在）
            if (candidateGroups != null && !candidateGroups.trim().isEmpty()) {
                bpmnXml.append(" flowable:candidateGroups=\"").append(escapeXml(candidateGroups.trim())).append("\"");
            }
            
            bpmnXml.append("/>\n");
            
            // 创建顺序流（从前一个节点到当前任务�?
            String flowId = "flow" + (i + 1);
            bpmnXml.append("    <sequenceFlow id=\"").append(flowId).append("\"");
            bpmnXml.append(" sourceRef=\"").append(previousNodeId).append("\"");
            bpmnXml.append(" targetRef=\"").append(taskId).append("\"/>\n");
            
            previousNodeId = taskId;
        }
        
        // 结束事件
        ProcessNode endNode = nodes.stream()
                .filter(node -> "endEvent".equals(node.getNodeType()))
                .findFirst()
                .orElse(null);
        
        String endNodeId;
        if (endNode != null) {
            endNodeId = normalizeId(endNode.getNodeId());
            bpmnXml.append("    <endEvent id=\"").append(endNodeId).append("\"");
            bpmnXml.append(" name=\"").append(escapeXml(endNode.getNodeName())).append("\"/>\n");
            
            // 从最后一个任务到结束事件的顺序流
            bpmnXml.append("    <sequenceFlow id=\"flowEnd\"");
            bpmnXml.append(" sourceRef=\"").append(previousNodeId).append("\"");
            bpmnXml.append(" targetRef=\"").append(endNodeId).append("\"/>\n");
        } else {
            // 如果没有结束节点，创建一个默认的
            endNodeId = "endEvent";
            bpmnXml.append("    <endEvent id=\"").append(endNodeId).append("\" name=\"结束\"/>\n");
            bpmnXml.append("    <sequenceFlow id=\"flowEnd\"");
            bpmnXml.append(" sourceRef=\"").append(previousNodeId).append("\"");
            bpmnXml.append(" targetRef=\"").append(endNodeId).append("\"/>\n");
        }
        
        bpmnXml.append("  </process>\n\n");
        bpmnXml.append("</definitions>");
        
        return bpmnXml.toString();
    }
    
    /**
     * 规范�?ID，确保符�?NCName 规范
     * NCName 规范�?
     * 1. 不能以数字开�?
     * 2. 只能包含字母、数字、下划线、连字符、点�?
     * 3. 不能包含空格和特殊字�?
     */
    private String normalizeId(String id) {
        if (id == null || id.trim().isEmpty()) {
            return "node_" + System.currentTimeMillis();
        }
        
        String normalized = id.trim();
        
        // 如果以数字开头，添加前缀
        if (normalized.matches("^\\d.*")) {
            normalized = "node_" + normalized;
        }
        
        // 替换非法字符为下划线
        // 只保留字母、数字、下划线、连字符、点�?
        normalized = normalized.replaceAll("[^a-zA-Z0-9_\\-\\.]", "_");
        
        // 确保不以点号或连字符开头或结尾
        normalized = normalized.replaceAll("^[\\-\\.]+", "");
        normalized = normalized.replaceAll("[\\-\\.]+$", "");
        
        // 如果规范化后为空，生成一个默认ID
        if (normalized.isEmpty()) {
            normalized = "node_" + System.currentTimeMillis();
        }
        
        return normalized;
    }
    
    /**
     * 转义 XML 特殊字符
     */
    private String escapeXml(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}

