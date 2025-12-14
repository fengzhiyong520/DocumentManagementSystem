package com.dms.config;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Flowable 诊断类
 * 用于检查 Flowable 是否正确初始化
 */
@Slf4j
@Component
@Order(1)
@ConditionalOnClass(name = "org.flowable.engine.ProcessEngine")
public class FlowableDiagnostic implements CommandLineRunner {

    @Autowired(required = false)
    private ProcessEngine processEngine;

    @Autowired(required = false)
    private RepositoryService repositoryService;

    @Autowired(required = false)
    private RuntimeService runtimeService;

    @Autowired(required = false)
    private TaskService taskService;

    @Override
    public void run(String... args) {
        log.info("========== Flowable 初始化诊断 ==========");
        
        if (processEngine != null) {
            log.info("✓ ProcessEngine 已成功初始化");
            log.info("  - ProcessEngine Name: {}", processEngine.getName());
        } else {
            log.error("✗ ProcessEngine 未初始化！请检查 Flowable 配置");
        }

        if (repositoryService != null) {
            log.info("✓ RepositoryService 已成功注入");
        } else {
            log.error("✗ RepositoryService 未注入！");
        }

        if (runtimeService != null) {
            log.info("✓ RuntimeService 已成功注入");
        } else {
            log.error("✗ RuntimeService 未注入！");
        }

        if (taskService != null) {
            log.info("✓ TaskService 已成功注入");
        } else {
            log.error("✗ TaskService 未注入！");
        }

        log.info("========================================");
        
        if (processEngine == null) {
            log.warn("警告：Flowable 未正确初始化，流程相关功能将不可用！");
            log.warn("请检查：");
            log.warn("1. Flowable 依赖是否正确添加（flowable-spring-boot-starter）");
            log.warn("2. 数据库连接配置是否正确");
            log.warn("3. Flowable 版本是否与 Spring Boot 3.x 兼容");
            log.warn("4. 查看启动日志中的 Flowable 相关错误信息");
        }
    }
}
