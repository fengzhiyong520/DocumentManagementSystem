package com.dms.service;

import lombok.RequiredArgsConstructor;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Flowable 工作流服务
 */
@Service
@RequiredArgsConstructor
public class FlowableWorkflowService {

    private final RuntimeService runtimeService;
    private final TaskService taskService;


    /**
     * 启动流程实例
     */
    public ProcessInstance startProcess(String processDefinitionKey, String businessKey, Map<String, Object> variables) {
        return runtimeService.startProcessInstanceByKey(processDefinitionKey, businessKey, variables);
    }

    /**
     * 获取用户待办任务列表
     */
    public List<Task> getTodoTasks(String userId) {
        return taskService.createTaskQuery()
                .taskAssignee(userId)
                .active()
                .list();
    }

    /**
     * 获取用户候选任务列表
     */
    public List<Task> getCandidateTasks(String userId) {
        return taskService.createTaskQuery()
                .taskCandidateUser(userId)
                .active()
                .list();
    }

    /**
     * 完成任务
     */
    public void completeTask(String taskId, Map<String, Object> variables) {
        taskService.complete(taskId, variables);
    }

    /**
     * 获取流程实例
     */
    public ProcessInstance getProcessInstance(String processInstanceId) {
        return runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
    }

    /**
     * 获取当前任务
     */
    public Task getCurrentTask(String processInstanceId) {
        return taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .active()
                .singleResult();
    }

    /**
     * 检查用户是否可以审批任务
     */
    public boolean canUserApproveTask(String taskId, String userId) {
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .active()
                .singleResult();
        
        if (task == null) {
            return false;
        }
        
        // 检查是否是任务分配人
        if (userId.equals(task.getAssignee())) {
            return true;
        }
        
        // 检查是否是候选用户
        long candidateUserCount = taskService.createTaskQuery()
                .taskId(taskId)
                .taskCandidateUser(userId)
                .count();
        
        return candidateUserCount > 0;
    }

    /**
     * 根据任务ID获取任务
     */
    public Task getTaskById(String taskId) {
        return taskService.createTaskQuery()
                .taskId(taskId)
                .active()
                .singleResult();
    }
}
