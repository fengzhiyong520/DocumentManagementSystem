package com.dms.controller;

import com.dms.core.domain.R;
import com.dms.service.ProcessMonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 流程监控控制�?
 */
@Tag(name = "流程监控")
@RestController
@RequestMapping("/process-monitor")
public class ProcessMonitorController {

    @Autowired(required = false)
    private ProcessMonitorService processMonitorService;

    @Operation(summary = "获取所有运行中的流程实例")
    @GetMapping("/running")
    public R<List<ProcessInstance>> getRunningProcessInstances() {
        if (processMonitorService == null) {
            return R.error("Flowable未配置，无法使用流程监控功能");
        }
        List<ProcessInstance> instances = processMonitorService.getRunningProcessInstances();
        return R.success(instances);
    }

    @Operation(summary = "获取流程实例的当前任务")
    @GetMapping("/tasks/{processInstanceId}")
    public R<List<Task>> getProcessInstanceTasks(@PathVariable String processInstanceId) {
        if (processMonitorService == null) {
            return R.error("Flowable未配置，无法使用流程监控功能");
        }
        List<Task> tasks = processMonitorService.getProcessInstanceTasks(processInstanceId);
        return R.success(tasks);
    }

    @Operation(summary = "获取流程实例的变量")
    @GetMapping("/variables/{processInstanceId}")
    public R<Map<String, Object>> getProcessInstanceVariables(@PathVariable String processInstanceId) {
        if (processMonitorService == null) {
            return R.error("Flowable未配置，无法使用流程监控功能");
        }
        Map<String, Object> variables = processMonitorService.getProcessInstanceVariables(processInstanceId);
        return R.success(variables);
    }

    @Operation(summary = "获取流程统计信息")
    @GetMapping("/statistics")
    public R<Map<String, Object>> getProcessStatistics() {
        if (processMonitorService == null) {
            return R.error("Flowable未配置，无法使用流程监控功能");
        }
        Map<String, Object> stats = processMonitorService.getProcessStatistics();
        return R.success(stats);
    }

    @Operation(summary = "根据业务键获取流程实例")
    @GetMapping("/business-key/{businessKey}")
    public R<ProcessInstance> getProcessInstanceByBusinessKey(@PathVariable String businessKey) {
        if (processMonitorService == null) {
            return R.error("Flowable未配置，无法使用流程监控功能");
        }
        ProcessInstance instance = processMonitorService.getProcessInstanceByBusinessKey(businessKey);
        return R.success(instance);
    }
}

