package com.dms.config;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Flowable 状态检查器
 * 在应用启动后检�?Flowable 相关 Bean 的状�?
 */
@Slf4j
@Component
@ConditionalOnClass(name = "org.flowable.engine.ProcessEngine")
public class FlowableStatusChecker {

    @Autowired(required = false)
    private ApplicationContext applicationContext;

    @PostConstruct
    public void checkFlowableStatus() {
        log.info("========== Flowable Bean 状态检查 ==========");

        // 检查 ProcessEngine
        try {
            ProcessEngine processEngine = applicationContext.getBean(ProcessEngine.class);
            if (processEngine != null) {
                log.info("✓ ProcessEngine Bean 存在: {}", processEngine.getName());
            } else {
                log.error("✗ ProcessEngine Bean 不存在");
            }
        } catch (Exception e) {
            log.error("✗ ProcessEngine Bean 不存在: {}", e.getMessage());
        }

        // 检查 RuntimeService
        try {
            RuntimeService runtimeService = applicationContext.getBean(RuntimeService.class);
            if (runtimeService != null) {
                log.info("✓ RuntimeService Bean 存在");
            } else {
                log.error("✗ RuntimeService Bean 不存在");
            }
        } catch (Exception e) {
            log.error("✗ RuntimeService Bean 不存在: {}", e.getMessage());
        }

        // 检查 TaskService
        try {
            TaskService taskService = applicationContext.getBean(TaskService.class);
            if (taskService != null) {
                log.info("✓ TaskService Bean 存在");
            } else {
                log.error("✗ TaskService Bean 不存在");
            }
        } catch (Exception e) {
            log.error("✗ TaskService Bean 不存在: {}", e.getMessage());
        }

        // 检查 RepositoryService
        try {
            RepositoryService repositoryService = applicationContext.getBean(RepositoryService.class);
            if (repositoryService != null) {
                log.info("✓ RepositoryService Bean 存在");
            } else {
                log.error("✗ RepositoryService Bean 不存在");
            }
        } catch (Exception e) {
            log.error("✗ RepositoryService Bean 不存在: {}", e.getMessage());
        }

        // 检查 FlowableWorkflowService
        try {
            Object workflowService = applicationContext.getBean("flowableWorkflowService");
            if (workflowService != null) {
                log.info("✓ FlowableWorkflowService Bean 存在");
            } else {
                log.error("✗ FlowableWorkflowService Bean 不存在");
            }
        } catch (Exception e) {
            log.error("✗ FlowableWorkflowService Bean 不存在: {}", e.getMessage());
            log.error("  原因：RuntimeService 或 TaskService Bean 不存在，导致 @ConditionalOnBean 条件不满足");
        }

        // 列出所有 Flowable 相关的 Bean
        log.info("--- 所有 Flowable 相关 Bean ---");
        String[] flowableBeans = applicationContext.getBeanNamesForType(ProcessEngine.class);
        if (flowableBeans.length > 0) {
            for (String beanName : flowableBeans) {
                log.info("  - {}", beanName);
            }
        } else {
            log.warn("  未找到任何 Flowable Bean");
        }

        log.info("========================================");
    }
}

