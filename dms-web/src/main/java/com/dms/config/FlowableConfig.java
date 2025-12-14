package com.dms.config;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import javax.sql.DataSource;

/**
 * Flowable 配置
 * 此配置类用于自定�?Flowable 引擎配置
 * Flowable 的自动配置会通过 spring-boot-starter 自动启用
 */
@Configuration
@ConditionalOnClass(name = "org.flowable.engine.ProcessEngine")
public class FlowableConfig implements EngineConfigurationConfigurer<SpringProcessEngineConfiguration> {

    @Override
    public void configure(SpringProcessEngineConfiguration engineConfiguration) {
        // 设置部署模式：never-fail-部署失败不抛出异常（因为我们手动部署�?
        engineConfiguration.setDeploymentMode("never-fail");
        // 设置数据库类�?
        engineConfiguration.setDatabaseType("mysql");
        // 设置数据库表前缀
        engineConfiguration.setDatabaseTablePrefix("act_");
        // 设置历史级别（已�?application.yml 中配置，这里再次确认�?
        engineConfiguration.setHistoryLevel(HistoryLevel.FULL);
        // 确保数据库表结构自动更新
        engineConfiguration.setDatabaseSchemaUpdate("true");
    }
}

