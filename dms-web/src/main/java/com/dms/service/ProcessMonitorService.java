package com.dms.service;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程监控服务
 */
@Service
@RequiredArgsConstructor
public class ProcessMonitorService {

    private final RuntimeService runtimeService;
    private final TaskService taskService;

    /**
     * 获取所有运行中的流程实�?
     */
    public List<ProcessInstance> getRunningProcessInstances() {
        return runtimeService.createProcessInstanceQuery()
                .active()
                .list();
    }

    /**
     * 获取流程实例的当前任�?
     */
    public List<Task> getProcessInstanceTasks(String processInstanceId) {
        return taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .active()
                .list();
    }

    /**
     * 获取流程实例的变�?
     */
    public Map<String, Object> getProcessInstanceVariables(String processInstanceId) {
        return runtimeService.getVariables(processInstanceId);
    }

    /**
     * 获取流程统计信息
     */
    public Map<String, Object> getProcessStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // 运行中的流程实例数量
        long runningCount = runtimeService.createProcessInstanceQuery()
                .active()
                .count();
        stats.put("runningCount", runningCount);
        
        // 待办任务数量
        long todoTaskCount = taskService.createTaskQuery()
                .active()
                .count();
        stats.put("todoTaskCount", todoTaskCount);
        
        // 已分配任务数�?
        long assignedTaskCount = taskService.createTaskQuery()
                .active()
                .count();
        stats.put("assignedTaskCount", assignedTaskCount);
        
        return stats;
    }

    /**
     * 根据业务键获取流程实�?
     */
    public ProcessInstance getProcessInstanceByBusinessKey(String businessKey) {
        return runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(businessKey)
                .active()
                .singleResult();
    }
}

