package com.dms.controller;

import com.dms.core.domain.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Flowable 诊断控制器
 * 用于检查 Flowable 初始化状态
 */
@Slf4j
@Tag(name = "Flowable 诊断")
@RestController
@RequestMapping("/flowable-diagnostic")
@ConditionalOnClass(name = "org.flowable.engine.ProcessEngine")
public class FlowableDiagnosticController {

    @Autowired(required = false)
    private ApplicationContext applicationContext;

    @Operation(summary = "检查 Flowable 初始化状态")
    @GetMapping("/status")
    public R<Map<String, Object>> checkFlowableStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // 检查 ProcessEngine
        try {
            ProcessEngine processEngine = applicationContext.getBean(ProcessEngine.class);
            status.put("processEngine", Map.of(
                "exists", true,
                "name", processEngine != null ? processEngine.getName() : "null"
            ));
        } catch (Exception e) {
            status.put("processEngine", Map.of(
                "exists", false,
                "error", e.getMessage()
            ));
        }
        
        // 检查 RuntimeService
        try {
            RuntimeService runtimeService = applicationContext.getBean(RuntimeService.class);
            status.put("runtimeService", Map.of(
                "exists", runtimeService != null
            ));
        } catch (Exception e) {
            status.put("runtimeService", Map.of(
                "exists", false,
                "error", e.getMessage()
            ));
        }
        
        // 检查 TaskService
        try {
            TaskService taskService = applicationContext.getBean(TaskService.class);
            status.put("taskService", Map.of(
                "exists", taskService != null
            ));
        } catch (Exception e) {
            status.put("taskService", Map.of(
                "exists", false,
                "error", e.getMessage()
            ));
        }
        
        // 检查 RepositoryService
        try {
            RepositoryService repositoryService = applicationContext.getBean(RepositoryService.class);
            status.put("repositoryService", Map.of(
                "exists", repositoryService != null
            ));
        } catch (Exception e) {
            status.put("repositoryService", Map.of(
                "exists", false,
                "error", e.getMessage()
            ));
        }
        
        // 检查 FlowableWorkflowService
        try {
            Object workflowService = applicationContext.getBean("flowableWorkflowService");
            status.put("flowableWorkflowService", Map.of(
                "exists", workflowService != null
            ));
        } catch (Exception e) {
            status.put("flowableWorkflowService", Map.of(
                "exists", false,
                "error", e.getMessage(),
                "reason", "RuntimeService 或 TaskService Bean 不存在，导致 @ConditionalOnBean 条件不满足"
            ));
        }
        
        // 检查数据库表
        try {
            RepositoryService repoService = applicationContext.getBean(RepositoryService.class);
            if (repoService != null) {
                long deploymentCount = repoService.createDeploymentQuery().count();
                status.put("databaseTables", Map.of(
                    "exists", true,
                    "deploymentCount", deploymentCount
                ));
            }
        } catch (Exception e) {
            status.put("databaseTables", Map.of(
                "exists", false,
                "error", e.getMessage()
            ));
        }
        
        // 总结
        boolean allServicesExist = 
            (Boolean) ((Map<?, ?>) status.getOrDefault("processEngine", Map.of("exists", false))).get("exists") &&
            (Boolean) ((Map<?, ?>) status.getOrDefault("runtimeService", Map.of("exists", false))).get("exists") &&
            (Boolean) ((Map<?, ?>) status.getOrDefault("taskService", Map.of("exists", false))).get("exists");
        
        status.put("summary", Map.of(
            "flowableInitialized", allServicesExist,
            "flowableWorkflowServiceAvailable", 
                (Boolean) ((Map<?, ?>) status.getOrDefault("flowableWorkflowService", Map.of("exists", false))).get("exists")
        ));
        
        return R.success(status);
    }
}
