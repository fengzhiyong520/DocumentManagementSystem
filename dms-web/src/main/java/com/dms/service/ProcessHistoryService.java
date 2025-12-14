package com.dms.service;

import com.dms.vo.HistoricTaskVO;
import com.dms.entity.User;
import com.dms.mapper.UserMapper;
import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 流程历史查询服务
 */
@Service
@RequiredArgsConstructor
public class ProcessHistoryService {

    private final HistoryService historyService;
    private final UserMapper userMapper;
    
    /**
     * 获取 HistoryService（供其他服务使用�?
     */
    public HistoryService getHistoryService() {
        return historyService;
    }

    /**
     * 根据流程实例ID查询流程历史
     */
    public HistoricProcessInstance getProcessInstanceHistory(String processInstanceId) {
        return historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
    }

    /**
     * 查询流程实例的所有历史任务（包含审批结果和备注）
     */
    public List<HistoricTaskVO> getProcessInstanceTasks(String processInstanceId) {
        // 先查询流程实例，获取流程定义信息
        HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        
        String processDefinitionKey = null;
        String processDefinitionName = null;
        if (processInstance != null) {
            processDefinitionKey = processInstance.getProcessDefinitionKey();
            processDefinitionName = processInstance.getProcessDefinitionName();
        }
        
        List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricTaskInstanceStartTime()
                .asc()
                .list();
        
        // 查询所有任务的历史变量（审批结果和备注�?
        // 注意：Flowable 中，任务完成时设置的变量保存在流程实例级�?
        // 我们通过查询每个任务相关的变量来获取审批结果和备�?
        // 先查询所有变量，然后按时间匹配到对应的任�?
        List<HistoricVariableInstance> allVariables = new java.util.ArrayList<>();
        try {
            // 分别查询 approved �?remark 变量
            @SuppressWarnings("unchecked")
            List<HistoricVariableInstance> approvedVars = (List<HistoricVariableInstance>) historyService
                    .createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .variableName("approved")
                    .list();
            
            @SuppressWarnings("unchecked")
            List<HistoricVariableInstance> remarkVars = (List<HistoricVariableInstance>) historyService
                    .createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .variableName("remark")
                    .list();
            
            // 合并两个列表
            allVariables.addAll(approvedVars);
            allVariables.addAll(remarkVars);
            
            // 按创建时间排�?
            allVariables.sort((v1, v2) -> {
                if (v1.getCreateTime() == null && v2.getCreateTime() == null) {
                    return 0;
                }
                if (v1.getCreateTime() == null) {
                    return 1;
                }
                if (v2.getCreateTime() == null) {
                    return -1;
                }
                return v1.getCreateTime().compareTo(v2.getCreateTime());
            });
        } catch (NoClassDefFoundError e) {
            // 如果 HistoricVariableInstance 类不存在，跳过变量查�?
        }
        
        // 为每个任务匹配变�?
        // 策略：按任务顺序，为每个任务匹配在其结束时间之后、下一个任务开始时间之前的变量
        Map<String, Map<String, Object>> taskVariablesMap = new java.util.HashMap<>();
        for (int i = 0; i < tasks.size(); i++) {
            HistoricTaskInstance task = tasks.get(i);
            Map<String, Object> varMap = new java.util.HashMap<>();
            
            // 如果任务已完成，查找该任务完成时的变�?
            if (task.getEndTime() != null && !allVariables.isEmpty()) {
                Date taskEndTime = task.getEndTime();
                // 下一个任务的开始时间（如果有）
                Date nextTaskStartTime = null;
                if (i < tasks.size() - 1) {
                    HistoricTaskInstance nextTask = tasks.get(i + 1);
                    if (nextTask.getStartTime() != null) {
                        nextTaskStartTime = nextTask.getStartTime();
                    }
                }
                
                // 查找在该任务完成时间点设置的变量
                // 策略：查找在任务结束时间之后、下一个任务开始时间之前（或没有下一个任务）的变�?
                HistoricVariableInstance approvedVar = null;
                HistoricVariableInstance remarkVar = null;
                long minApprovedDiff = Long.MAX_VALUE;
                long minRemarkDiff = Long.MAX_VALUE;
                
                for (HistoricVariableInstance var : allVariables) {
                    String varName = var.getVariableName();
                    if (var.getCreateTime() != null) {
                        long varTime = var.getCreateTime().getTime();
                        long taskEndTimeMs = taskEndTime.getTime();
                        long timeDiff = varTime - taskEndTimeMs;
                        
                        // 变量应该在任务结束时间之后（任务完成时设置的变量�?
                        // 如果有下一个任务，变量应该在下一个任务开始时间之�?
                        // 扩大时间窗口�?20秒，确保能匹配到变量
                        boolean inTimeRange = timeDiff >= -5000 && timeDiff <= 120000; // 允许任务结束前后5秒到120�?
                        if (nextTaskStartTime != null) {
                            long nextTaskStartTimeMs = nextTaskStartTime.getTime();
                            inTimeRange = inTimeRange && varTime < nextTaskStartTimeMs;
                        }
                        
                        if (inTimeRange) {
                            long absTimeDiff = Math.abs(timeDiff);
                            // 优先匹配在任务结束时间之后设置的变量（更准确�?
                            if ("approved".equals(varName)) {
                                // 如果变量在任务结束时间之后，优先选择
                                if (timeDiff >= 0 && timeDiff < minApprovedDiff) {
                                    approvedVar = var;
                                    minApprovedDiff = timeDiff;
                                } else if (approvedVar == null && absTimeDiff < minApprovedDiff) {
                                    // 如果没有找到任务结束后的变量，使用最接近�?
                                    approvedVar = var;
                                    minApprovedDiff = absTimeDiff;
                                }
                            } else if ("remark".equals(varName)) {
                                if (timeDiff >= 0 && timeDiff < minRemarkDiff) {
                                    remarkVar = var;
                                    minRemarkDiff = timeDiff;
                                } else if (remarkVar == null && absTimeDiff < minRemarkDiff) {
                                    remarkVar = var;
                                    minRemarkDiff = absTimeDiff;
                                }
                            }
                        }
                    }
                }
                
                // 设置审批结果
                if (approvedVar != null) {
                    Object value = approvedVar.getValue();
                    if (value instanceof Boolean) {
                        varMap.put("approved", value);
                    } else if (value != null) {
                        varMap.put("approved", Boolean.parseBoolean(value.toString()));
                    }
                }
                
                // 设置审批备注
                if (remarkVar != null) {
                    Object value = remarkVar.getValue();
                    if (value != null) {
                        varMap.put("remark", value.toString());
                    }
                }
            }
            
            taskVariablesMap.put(task.getId(), varMap);
        }
        
        // 转换为VO
        String finalProcessDefinitionKey = processDefinitionKey;
        String finalProcessDefinitionName = processDefinitionName;
        return tasks.stream().map(task -> {
            HistoricTaskVO vo = new HistoricTaskVO();
            vo.setId(task.getId());
            vo.setName(task.getName());
            vo.setAssignee(task.getAssignee());
            // 查询审批人昵�?
            if (task.getAssignee() != null && !task.getAssignee().trim().isEmpty()) {
                try {
                    Long assigneeId = Long.parseLong(task.getAssignee());
                    User assigneeUser = userMapper.selectById(assigneeId);
                    if (assigneeUser != null) {
                        vo.setAssigneeName(assigneeUser.getNickname() != null ? assigneeUser.getNickname() : assigneeUser.getUsername());
                    }
                } catch (NumberFormatException e) {
                    // 如果 assignee 不是数字，可能是其他格式，忽�?
                }
            }
            vo.setOwner(task.getOwner());
            
            // 转换时间（Date �?LocalDateTime�?
            if (task.getStartTime() != null) {
                Date startDate = task.getStartTime();
                vo.setStartTime(LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(startDate.getTime()),
                        ZoneId.systemDefault()
                ));
            }
            if (task.getEndTime() != null) {
                Date endDate = task.getEndTime();
                vo.setEndTime(LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(endDate.getTime()),
                        ZoneId.systemDefault()
                ));
            }
            
            vo.setDurationInMillis(task.getDurationInMillis());
            vo.setDeleteReason(task.getDeleteReason());
            vo.setProcessInstanceId(task.getProcessInstanceId());
            vo.setProcessDefinitionId(task.getProcessDefinitionId());
            // 从流程实例获取流程定义Key和Name
            vo.setProcessDefinitionKey(finalProcessDefinitionKey);
            vo.setProcessDefinitionName(finalProcessDefinitionName);
            vo.setTaskDefinitionKey(task.getTaskDefinitionKey());
            vo.setPriority(task.getPriority());
            vo.setCategory(task.getCategory());
            vo.setFormKey(task.getFormKey());
            vo.setParentTaskId(task.getParentTaskId());
            vo.setDescription(task.getDescription());
            
            // 从任务变量中获取审批结果和备�?
            Map<String, Object> taskVariables = taskVariablesMap.get(task.getId());
            if (taskVariables != null) {
                // 获取审批结果
                Object approvedObj = taskVariables.get("approved");
                if (approvedObj instanceof Boolean) {
                    vo.setApproved((Boolean) approvedObj);
                } else if (approvedObj != null) {
                    vo.setApproved(Boolean.parseBoolean(approvedObj.toString()));
                }
                
                // 获取审批备注
                Object remarkObj = taskVariables.get("remark");
                if (remarkObj != null) {
                    vo.setRemark(remarkObj.toString());
                }
            }
            
            return vo;
        }).collect(Collectors.toList());
    }
    
    /**
     * 查询流程实例的所有历史任务（原始方法，保持兼容性）
     */
    public List<HistoricTaskInstance> getProcessInstanceTasksRaw(String processInstanceId) {
        return historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricTaskInstanceStartTime()
                .asc()
                .list();
    }

    /**
     * 查询用户的历史任�?
     */
    public List<HistoricTaskInstance> getUserHistoryTasks(String userId) {
        return historyService.createHistoricTaskInstanceQuery()
                .taskAssignee(userId)
                .finished()
                .orderByHistoricTaskInstanceEndTime()
                .desc()
                .list();
    }

    /**
     * 根据业务键查询流程历�?
     */
    public HistoricProcessInstance getProcessInstanceByBusinessKey(String businessKey) {
        return historyService.createHistoricProcessInstanceQuery()
                .processInstanceBusinessKey(businessKey)
                .singleResult();
    }

    /**
     * 查询所有已完成的流程实�?
     */
    public List<HistoricProcessInstance> getFinishedProcessInstances() {
        return historyService.createHistoricProcessInstanceQuery()
                .finished()
                .orderByProcessInstanceEndTime()
                .desc()
                .list();
    }
}

