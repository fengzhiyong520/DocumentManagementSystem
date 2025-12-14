# Flowable 工作流引擎集成配置文档

## 目录
- [1. 概述](#1-概述)
- [2. 技术栈](#2-技术栈)
- [3. 依赖配置](#3-依赖配置)
- [4. 数据库配置](#4-数据库配置)
- [5. Flowable 引擎配置](#5-flowable-引擎配置)
- [6. 数据库表结构](#6-数据库表结构)
- [7. 核心服务设计](#7-核心服务设计)
- [8. API 接口设计](#8-api-接口设计)
- [9. 动态 BPMN 生成](#9-动态-bpmn-生成)
- [10. 集成流程](#10-集成流程)
- [11. 注意事项](#11-注意事项)

---

## 1. 概述

本文档描述了如何在文档管理系统中集成 Flowable 工作流引擎，实现审批流程的自动化管理。

### 1.1 功能特性
- ✅ 动态流程定义管理
- ✅ 流程节点配置（支持多级审批）
- ✅ 动态 BPMN 生成与部署
- ✅ 流程实例启动与任务处理
- ✅ 流程历史查询
- ✅ 流程监控与管理

### 1.2 版本信息
- **Flowable 版本**: 7.0.1
- **Spring Boot 版本**: 3.1.5
- **Java 版本**: 17

---

## 2. 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Flowable | 7.0.1 | 工作流引擎 |
| Spring Boot | 3.1.5 | 应用框架 |
| MyBatis-Plus | 3.5.7 | ORM 框架 |
| MySQL | 8.0.33 | 数据库 |
| Sa-Token | 1.37.0 | 认证授权 |

---

## 3. 依赖配置

### 3.1 Maven 依赖

在根 `pom.xml` 中定义版本属性：

```xml
<properties>
    <flowable.version>7.0.1</flowable.version>
</properties>
```

在 `dependencyManagement` 中声明依赖：

```xml
<!-- Flowable -->
<dependency>
    <groupId>org.flowable</groupId>
    <artifactId>flowable-spring-boot-starter</artifactId>
    <version>${flowable.version}</version>
</dependency>
```

在 `dms-file` 模块的 `pom.xml` 中添加依赖：

```xml
<dependency>
    <groupId>org.flowable</groupId>
    <artifactId>flowable-spring-boot-starter</artifactId>
</dependency>
```

### 3.2 版本选择说明

- **Flowable 6.8.0**: 不完全支持 Spring Boot 3.x
- **Flowable 7.0.1**: 完全支持 Spring Boot 3.x，推荐使用

---

## 4. 数据库配置

### 4.1 application.yml 配置

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/dms?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=GMT%2B8&nullCatalogMeansCurrent=true
    username: root
    password: mysql123
```

**重要配置项**：
- `nullCatalogMeansCurrent=true`: 确保 Flowable 在正确的 schema 中创建表

### 4.2 Flowable 配置

```yaml
flowable:
  # 数据库表结构更新策略：true-自动创建/更新表结构
  database-schema-update: true
  # 启用异步执行器
  async-executor-activate: true
  # 历史级别：full-完整历史记录
  history-level: full
  # 禁用自动检查流程定义（我们手动部署）
  check-process-definitions: false
  # 流程定义文件位置（如果使用文件部署）
  process-definition-location-prefix: classpath*:/processes/
  process-definition-location-suffixes: .bpmn20.xml,.bpmn
```

**配置说明**：
- `database-schema-update: true`: 自动创建/更新 Flowable 表结构
- `history-level: full`: 记录完整的流程历史，便于查询和审计
- `check-process-definitions: false`: 禁用自动扫描，使用手动部署

---

## 5. Flowable 引擎配置

### 5.1 FlowableConfig.java

```java
@Configuration
@ConditionalOnClass(name = "org.flowable.engine.ProcessEngine")
public class FlowableConfig implements EngineConfigurationConfigurer<SpringProcessEngineConfiguration> {

    @Override
    public void configure(SpringProcessEngineConfiguration engineConfiguration) {
        // 设置部署模式：never-fail-部署失败不抛出异常（因为我们手动部署）
        engineConfiguration.setDeploymentMode("never-fail");
        // 设置数据库类型
        engineConfiguration.setDatabaseType("mysql");
        // 设置数据库表前缀
        engineConfiguration.setDatabaseTablePrefix("act_");
        // 设置历史级别
        engineConfiguration.setHistoryLevel(HistoryLevel.FULL);
        // 确保数据库表结构自动更新
        engineConfiguration.setDatabaseSchemaUpdate("true");
    }
}
```

**配置说明**：
- `deploymentMode("never-fail")`: 部署失败时不抛出异常，便于手动处理
- `databaseTablePrefix("act_")`: Flowable 表前缀，便于识别
- `historyLevel(HistoryLevel.FULL)`: 完整历史记录

### 5.2 条件加载

使用 `@ConditionalOnClass` 和 `@ConditionalOnBean` 确保 Flowable 相关 Bean 只在 Flowable 存在时加载：

```java
@ConditionalOnClass(name = "org.flowable.engine.ProcessEngine")
@ConditionalOnBean({RuntimeService.class, TaskService.class})
```

---

## 6. 数据库表结构

### 6.1 Flowable 系统表

Flowable 会自动创建以下表（前缀 `act_`）：
- `act_re_deployment`: 部署信息表
- `act_re_procdef`: 流程定义表
- `act_ru_execution`: 运行时执行实例表
- `act_ru_task`: 运行时任务表
- `act_hi_procinst`: 历史流程实例表
- `act_hi_taskinst`: 历史任务实例表
- 等等...

### 6.2 业务表

#### 6.2.1 流程定义表 (process_definition)

```sql
CREATE TABLE IF NOT EXISTS `process_definition` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` VARCHAR(100) NOT NULL COMMENT '流程名称',
  `key` VARCHAR(50) NOT NULL COMMENT '流程Key（唯一标识）',
  `description` VARCHAR(500) COMMENT '流程描述',
  `status` INT DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `category` VARCHAR(50) COMMENT '流程分类',
  `deployment_id` VARCHAR(64) COMMENT 'Flowable部署ID',
  `flowable_definition_id` VARCHAR(64) COMMENT 'Flowable流程定义ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT DEFAULT 0 COMMENT '删除标志：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key` (`key`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程定义表';
```

#### 6.2.2 流程节点配置表 (process_node)

```sql
CREATE TABLE IF NOT EXISTS `process_node` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `process_definition_id` BIGINT NOT NULL COMMENT '流程定义ID',
  `node_id` VARCHAR(50) NOT NULL COMMENT '节点ID（流程中的节点标识）',
  `node_name` VARCHAR(100) NOT NULL COMMENT '节点名称',
  `node_type` VARCHAR(50) NOT NULL COMMENT '节点类型：startEvent, userTask, endEvent等',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `assignee` VARCHAR(500) COMMENT '审批人（用户ID，多个用逗号分隔）',
  `candidate_users` VARCHAR(500) COMMENT '候选审批人（用户ID，多个用逗号分隔）',
  `candidate_groups` VARCHAR(500) COMMENT '候选审批组（角色ID，多个用逗号分隔）',
  `form_key` VARCHAR(100) COMMENT '表单Key',
  `description` VARCHAR(500) COMMENT '节点描述',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT DEFAULT 0 COMMENT '删除标志：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_process_definition_id` (`process_definition_id`),
  KEY `idx_node_id` (`node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程节点配置表';
```

#### 6.2.3 借阅表扩展 (file_borrow)

```sql
ALTER TABLE `file_borrow` 
ADD COLUMN `process_definition_id` BIGINT COMMENT '流程定义ID' AFTER `document_id`,
ADD COLUMN `process_instance_id` VARCHAR(64) COMMENT '流程实例ID' AFTER `process_definition_id`,
ADD COLUMN `task_id` VARCHAR(64) COMMENT '当前任务ID' AFTER `process_instance_id`,
ADD KEY `idx_process_instance_id` (`process_instance_id`);
```

---

## 7. 核心服务设计

### 7.1 服务架构

```
ProcessDefinitionService (流程定义管理)
    ├── ProcessNodeService (流程节点管理)
    ├── BpmnGeneratorService (BPMN 动态生成)
    ├── FlowableWorkflowService (流程实例操作)
    ├── ProcessHistoryService (流程历史查询)
    └── ProcessMonitorService (流程监控)
```

### 7.2 服务说明

#### 7.2.1 ProcessDefinitionService

**职责**：流程定义 CRUD、部署流程

**主要方法**：
- `saveProcessDefinition(ProcessDefinition)`: 保存流程定义
- `updateProcessDefinition(ProcessDefinition)`: 更新流程定义
- `deployProcessDefinition(Long id)`: 部署流程定义到 Flowable
- `getEnabledProcessDefinitions()`: 获取启用的流程定义列表

#### 7.2.2 ProcessNodeService

**职责**：流程节点配置管理

**主要方法**：
- `getNodesByProcessDefinitionId(Long id)`: 获取流程的所有节点
- `saveNodes(Long processDefinitionId, List<ProcessNode> nodes)`: 保存节点配置
- `deleteNode(Long id)`: 删除节点
- `deleteNodesByProcessDefinitionId(Long id)`: 删除流程的所有节点

#### 7.2.3 BpmnGeneratorService

**职责**：根据流程定义和节点配置动态生成 BPMN XML

**主要方法**：
- `generateBpmnXml(ProcessDefinition, List<ProcessNode>)`: 生成 BPMN XML 字符串

**生成逻辑**：
1. 解析节点配置，按 `sortOrder` 排序
2. 生成开始事件（startEvent）
3. 生成用户任务节点（userTask），配置审批人
4. 生成结束事件（endEvent）
5. 生成序列流（sequenceFlow）连接节点

#### 7.2.4 FlowableWorkflowService

**职责**：流程实例的启动、任务处理

**主要方法**：
- `startProcess(String processDefinitionKey, String businessKey, Map<String, Object> variables)`: 启动流程实例
- `getTodoTasks(String userId)`: 获取用户待办任务
- `getCandidateTasks(String userId)`: 获取用户候选任务
- `completeTask(String taskId, String userId, Map<String, Object> variables)`: 完成任务
- `rejectTask(String taskId, String userId, String reason)`: 拒绝任务
- `getTaskById(String taskId)`: 根据任务ID获取任务
- `canUserApproveTask(String taskId, String userId)`: 检查用户是否有权限审批任务

#### 7.2.5 ProcessHistoryService

**职责**：查询流程历史记录

**主要方法**：
- `getProcessHistory(String processInstanceId)`: 获取流程实例历史
- `getTaskHistory(String processInstanceId)`: 获取任务历史
- `getCompletedProcesses(String userId)`: 获取用户已完成的流程

#### 7.2.6 ProcessMonitorService

**职责**：流程监控和管理

**主要方法**：
- `getRunningProcesses()`: 获取运行中的流程实例
- `getProcessInstanceInfo(String processInstanceId)`: 获取流程实例详情
- `suspendProcessInstance(String processInstanceId)`: 挂起流程实例
- `activateProcessInstance(String processInstanceId)`: 激活流程实例

---

## 8. API 接口设计

### 8.1 流程定义管理

**Controller**: `ProcessDefinitionController`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/process-definition/enabled` | 获取启用的流程定义列表 |
| GET | `/process-definition/page` | 分页查询流程定义 |
| GET | `/process-definition/{id}` | 获取流程定义详情 |
| POST | `/process-definition` | 保存流程定义 |
| PUT | `/process-definition` | 更新流程定义 |
| DELETE | `/process-definition/{id}` | 删除流程定义 |
| POST | `/process-definition/{id}/deploy` | 部署流程定义 |

### 8.2 流程节点管理

**Controller**: `ProcessNodeController`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/process-node/{processDefinitionId}` | 获取流程的所有节点 |
| POST | `/process-node` | 保存节点配置 |
| PUT | `/process-node` | 更新节点配置 |
| DELETE | `/process-node/{id}` | 删除节点 |
| DELETE | `/process-node/process/{processDefinitionId}` | 删除流程的所有节点 |

### 8.3 流程历史查询

**Controller**: `ProcessHistoryController`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/process-history/instance/{processInstanceId}` | 获取流程实例历史 |
| GET | `/process-history/task/{processInstanceId}` | 获取任务历史 |
| GET | `/process-history/completed` | 获取用户已完成的流程 |

### 8.4 流程监控

**Controller**: `ProcessMonitorController`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/process-monitor/running` | 获取运行中的流程实例 |
| GET | `/process-monitor/instance/{processInstanceId}` | 获取流程实例详情 |
| PUT | `/process-monitor/suspend/{processInstanceId}` | 挂起流程实例 |
| PUT | `/process-monitor/activate/{processInstanceId}` | 激活流程实例 |

---

## 9. 动态 BPMN 生成

### 9.1 生成流程

1. 读取流程定义和节点配置
2. 按 `sortOrder` 排序节点
3. 生成 BPMN XML：
   - 开始事件（startEvent）
   - 用户任务节点（userTask）
   - 结束事件（endEvent）
   - 序列流（sequenceFlow）

### 9.2 节点类型支持

- `startEvent`: 开始事件
- `userTask`: 用户任务（审批节点）
- `endEvent`: 结束事件

### 9.3 任务分配策略

优先级顺序：
1. `assignee`: 直接指定审批人（用户ID）
2. `candidateUsers`: 候选审批人（多个用户ID，逗号分隔）
3. `candidateGroups`: 候选审批组（角色ID，多个用逗号分隔）

### 9.4 BPMN XML 示例

```xml
<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xmlns:flowable="http://flowable.org/bpmn"
             targetNamespace="http://flowable.org/bpmn">
  <process id="borrowApproval" name="借阅审批流程" isExecutable="true">
    <startEvent id="startEvent" name="开始"/>
    <userTask id="task1" name="部门经理审批" flowable:assignee="1"/>
    <userTask id="task2" name="总经理审批" flowable:assignee="2"/>
    <endEvent id="endEvent" name="结束"/>
    <sequenceFlow id="flow1" sourceRef="startEvent" targetRef="task1"/>
    <sequenceFlow id="flow2" sourceRef="task1" targetRef="task2"/>
    <sequenceFlow id="flow3" sourceRef="task2" targetRef="endEvent"/>
  </process>
</definitions>
```

---

## 10. 集成流程

### 10.1 流程定义创建流程

1. **创建流程定义**
   - 调用 `POST /process-definition` 创建流程定义
   - 填写流程名称、Key、描述等信息

2. **配置流程节点**
   - 点击"配置节点"按钮
   - 添加开始事件、用户任务节点、结束事件
   - 配置每个节点的审批人

3. **部署流程**
   - 点击"部署"按钮
   - 系统自动生成 BPMN XML
   - 部署到 Flowable 引擎

### 10.2 审批流程使用流程

1. **申请审批**
   - 用户在大厅管理页面申请查看资料
   - 选择审批流程
   - 系统启动流程实例

2. **审批处理**
   - 审批人登录系统
   - 在审批管理页面查看待办任务
   - 审批通过/拒绝

3. **流程流转**
   - 审批通过后，自动流转到下一节点
   - 所有节点审批通过后，流程结束

---

## 11. 注意事项

### 11.1 条件加载

所有 Flowable 相关服务都使用条件注解，确保在 Flowable 不存在时不会报错：

```java
@ConditionalOnBean({RuntimeService.class, TaskService.class})
@Autowired(required = false)
```

### 11.2 数据库表前缀

Flowable 表使用 `act_` 前缀，业务表使用自定义前缀，避免冲突。

### 11.3 流程部署

- 流程定义创建后需要手动部署
- 部署前必须配置节点
- 部署后的流程定义不能直接修改，需要重新部署

### 11.4 历史记录

- 历史级别设置为 `full`，会记录所有历史数据
- 生产环境建议定期清理历史数据

### 11.5 任务分配

- 支持直接指定审批人（assignee）
- 支持候选审批人（candidateUsers）
- 支持候选审批组（candidateGroups）
- 优先级：assignee > candidateUsers > candidateGroups

### 11.6 错误处理

- 部署失败时不会抛出异常（deploymentMode: never-fail）
- 需要在业务代码中检查部署结果
- 流程实例启动失败时，需要回滚业务数据

---

## 12. 常见问题

### Q1: Flowable 表没有自动创建？

**A**: 检查 `application.yml` 中的 `database-schema-update: true` 配置，以及 JDBC URL 中的 `nullCatalogMeansCurrent=true`。

### Q2: 流程定义创建后无法部署？

**A**: 确保已配置流程节点，节点必须包含开始事件、至少一个用户任务、结束事件。

### Q3: 审批任务找不到？

**A**: 检查任务分配配置，确保审批人ID正确，或者使用候选用户/组。

### Q4: 流程历史查询不到数据？

**A**: 确保 `history-level: full` 配置正确，并且流程实例已启动。

---

## 13. 参考资料

- [Flowable 官方文档](https://www.flowable.com/open-source/docs/)
- [BPMN 2.0 规范](https://www.omg.org/spec/BPMN/2.0/)
- [Spring Boot 3.x 文档](https://spring.io/projects/spring-boot)

---

**文档版本**: v1.0  
**最后更新**: 2024-12-11

