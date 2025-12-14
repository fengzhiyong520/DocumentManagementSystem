package com.dms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dms.utils.PageUtils;
import com.dms.dto.BorrowDTO;
import com.dms.entity.Borrow;
import com.dms.entity.Document;
import com.dms.mapper.BorrowMapper;
import com.dms.entity.ProcessDefinition;
import com.dms.service.BorrowService;
import com.dms.service.DocumentService;
import com.dms.service.FlowableWorkflowService;
import com.dms.service.ProcessDefinitionService;
import com.dms.service.ProcessHistoryService;
import com.dms.service.ProcessMonitorService;
import com.dms.vo.BorrowVO;
import com.dms.entity.User;
import com.dms.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cn.dev33.satoken.stp.StpUtil;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 借阅服务实现
 */
@Service
@RequiredArgsConstructor
public class BorrowServiceImpl extends ServiceImpl<BorrowMapper, Borrow> implements BorrowService {

    private final DocumentService documentService;
    private final UserMapper userMapper;
    private final ProcessDefinitionService processDefinitionService;

    // Flowable 相关服务使用可选注入，因为 Flowable 可能未正确配置
    @Autowired(required = false)
    private FlowableWorkflowService flowableWorkflowService;

    @Autowired(required = false)
    private ProcessMonitorService processMonitorService;

    @Autowired(required = false)
    private ProcessHistoryService processHistoryService;

    // 如果 Flowable 未启用，这些服务可能为 null，需要在使用前检查

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyBorrow(BorrowDTO borrowDTO) {
        Borrow borrow = new Borrow();
        BeanUtils.copyProperties(borrowDTO, borrow);
        borrow.setUserId(StpUtil.getLoginIdAsLong());
        borrow.setBorrowTime(LocalDateTime.now());
        borrow.setStatus(0); // 待审批

        // 如果指定了流程定义，启动流程实例
        if (borrowDTO.getProcessDefinitionId() != null) {
            ProcessDefinition processDefinition = processDefinitionService.getById(borrowDTO.getProcessDefinitionId());
            if (processDefinition != null && processDefinition.getStatus() == 1) {
                // 检查 Flowable 服务是否可用
                if (flowableWorkflowService == null) {
                    throw new RuntimeException("Flowable 工作流服务未正确初始化。可能的原因：1) Flowable 依赖未正确加载；2) Flowable 配置错误；3) 数据库连接问题。请检查 Flowable 配置或使用传统审批方式（不选择流程定义）。");
                }

                // 检查流程是否已部署
                if (processDefinition.getDeploymentId() == null && processDefinition.getFlowableDefinitionId() == null) {
                    throw new RuntimeException("流程定义未部署，请先部署流程后再申请。");
                }

                // 保存借阅记录，获取ID作为业务键
                this.save(borrow);

                // 启动流程实例
                Map<String, Object> variables = new HashMap<>();
                variables.put("documentId", borrow.getDocumentId());
                variables.put("userId", borrow.getUserId());
                variables.put("reason", borrow.getReason());

                ProcessInstance processInstance = flowableWorkflowService.startProcess(
                        processDefinition.getKey(),
                        borrow.getId().toString(),
                        variables
                );

                borrow.setProcessDefinitionId(processDefinition.getId());
                borrow.setProcessInstanceId(processInstance.getId());

                // 获取当前任务
                Task task = flowableWorkflowService.getCurrentTask(processInstance.getId());
                if (task != null) {
                    borrow.setTaskId(task.getId());
                }

                this.updateById(borrow);
            } else {
                // 流程定义不存在或已禁用，使用传统审批方式
                this.save(borrow);
            }
        } else {
            // 未指定流程定义，使用传统审批方式
            this.save(borrow);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveBorrow(Long id, Integer status, String remark) {
        Borrow borrow = this.getById(id);
        if (borrow == null) {
            throw new RuntimeException("借阅记录不存在");
        }

        // 如果使用流程，完成任务
        if (borrow.getProcessInstanceId() != null && borrow.getTaskId() != null && flowableWorkflowService != null) {
            // 检查当前用户是否有权限审批该任务
            Long currentUserId = StpUtil.getLoginIdAsLong();
            if (!flowableWorkflowService.canUserApproveTask(borrow.getTaskId(), currentUserId.toString())) {
                throw new RuntimeException("您没有权限审批该任务");
            }

            Map<String, Object> variables = new HashMap<>();
            variables.put("approved", status == 1);
            variables.put("remark", remark);

            flowableWorkflowService.completeTask(borrow.getTaskId(), variables);

            // 获取下一个任务
            Task nextTask = flowableWorkflowService.getCurrentTask(borrow.getProcessInstanceId());
            if (nextTask != null) {
                borrow.setTaskId(nextTask.getId());
                borrow.setStatus(0); // 继续审批中
            } else {
                // 流程结束
                borrow.setStatus(status); // 1-已批准 2-已拒绝
                borrow.setTaskId(null);
            }
        } else {
            // 传统审批方式
            borrow.setStatus(status); // 1-已批准 2-已拒绝
        }

        borrow.setApproverId(StpUtil.getLoginIdAsLong());
        borrow.setRemark(remark);
        this.updateById(borrow);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnDocument(Long id) {
        Borrow borrow = this.getById(id);
        if (borrow == null) {
            throw new RuntimeException("借阅记录不存在");
        }
        borrow.setStatus(3); // 已归还
        borrow.setReturnTime(LocalDateTime.now());
        this.updateById(borrow);
    }

    @Override
    public PageUtils<BorrowVO> pageBorrow(Long current, Long size, Integer status) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        String currentUserIdStr = currentUserId.toString();

        // 先查询当前用户创建的所有资料ID（用于传统审批方式）
        List<Document> myDocuments = documentService.list(new LambdaQueryWrapper<Document>()
                .eq(Document::getUploadUserId, currentUserId));
        List<Long> myDocumentIds = myDocuments.stream()
                .map(Document::getId)
                .toList();

        LambdaQueryWrapper<Borrow> wrapper = new LambdaQueryWrapper<>();

        // 审批管理只显示待审批的数据
        wrapper.eq(Borrow::getStatus, 0);

        // 查询条件：当前用户待审批的记录
        // 1. 传统审批方式：待审批状态且当前用户是资料创建者
        // 2. 流程审批方式：待审批状态且有流程实例ID的记录（后续通过查询任务来过滤）
        if (myDocumentIds != null && !myDocumentIds.isEmpty()) {
            wrapper.and(w -> {
                // 条件1：传统审批方式 - 待审批状态且当前用户是资料创建者
                w.in(Borrow::getDocumentId, myDocumentIds)
                        .isNull(Borrow::getProcessInstanceId)
                        // 条件2：流程审批方式 - 有流程实例ID的记录（后续通过查询任务来过滤）
                        .or(w2 -> w2.isNotNull(Borrow::getProcessInstanceId));
            });
        } else {
            // 如果用户没有创建任何资料，只查询流程审批的记录
            wrapper.isNotNull(Borrow::getProcessInstanceId);
        }

        wrapper.orderByDesc(Borrow::getCreateTime);

        // 先查询所有可能的记录（不分页），然后过滤
        List<Borrow> allRecords = this.list(wrapper);

        // 过滤结果：只保留当前用户待审批的记录
        List<Borrow> filteredRecords = allRecords.stream()
                .filter(borrow -> {
                    // 确保是待审批状态
                    if (borrow.getStatus() != 0) {
                        return false;
                    }

                    // 传统审批方式：待审批状态且当前用户是资料创建者
                    if (borrow.getProcessInstanceId() == null
                            && myDocumentIds.contains(borrow.getDocumentId())) {
                        return true;
                    }

                    // 流程审批方式：检查当前任务的任务审批人是否等于当前用户
                    if (borrow.getProcessInstanceId() != null && processMonitorService != null) {
                        try {
                            List<Task> tasks = processMonitorService.getProcessInstanceTasks(borrow.getProcessInstanceId());
                            if (!tasks.isEmpty()) {
                                Task currentTask = tasks.get(0);
                                String assignee = currentTask.getAssignee();
                                if (assignee != null && assignee.equals(currentUserIdStr)) {
                                    return true;
                                }
                                // 检查候选用户
                                if (flowableWorkflowService != null) {
                                    return flowableWorkflowService.canUserApproveTask(
                                            currentTask.getId(),
                                            currentUserIdStr
                                    );
                                }
                            }
                        } catch (Exception e) {
                            // 流程可能已结束，忽略
                        }
                    }
                    return false;
                })
                .collect(java.util.stream.Collectors.toList());

        // 手动分页
        long total = filteredRecords.size();
        int start = (int) ((current - 1) * size);
        int end = (int) Math.min(start + size, total);
        List<Borrow> pagedRecords = start < total ? filteredRecords.subList(start, end) : new ArrayList<>();

        PageUtils<BorrowVO> pageUtils = new PageUtils<>();
        pageUtils.setCurrent(current);
        pageUtils.setSize(size);
        pageUtils.setTotal(total);
        pageUtils.setRecords(pagedRecords.stream().map(borrow -> {
            BorrowVO vo = new BorrowVO();
            BeanUtils.copyProperties(borrow, vo);

            // 查询资料标题
            if (borrow.getDocumentId() != null) {
                Document document = documentService.getById(borrow.getDocumentId());
                if (document != null) {
                    vo.setDocumentTitle(document.getTitle());
                }
            }

            // 查询借阅人名称
            if (borrow.getUserId() != null) {
                User user = userMapper.selectById(borrow.getUserId());
                if (user != null) {
                    vo.setUserName(user.getNickname() != null ? user.getNickname() : user.getUsername());
                }
            }

            // 查询审批人名称
            if (borrow.getApproverId() != null) {
                User approver = userMapper.selectById(borrow.getApproverId());
                if (approver != null) {
                    vo.setApproverName(approver.getNickname() != null ? approver.getNickname() : approver.getUsername());
                }
            }

            // 查询流程任务信息
            boolean canApprove = false;

            if (borrow.getProcessInstanceId() != null && processMonitorService != null) {
                try {
                    List<Task> tasks = processMonitorService.getProcessInstanceTasks(borrow.getProcessInstanceId());
                    if (!tasks.isEmpty()) {
                        Task currentTask = tasks.get(0);
                        vo.setTaskId(currentTask.getId());
                        vo.setTaskName(currentTask.getName());
                        // 设置任务审批人（确保为字符串格式）
                        String assignee = currentTask.getAssignee();
                        vo.setTaskAssignee(assignee != null ? assignee : null);
                        // 查询审批人昵称
                        if (assignee != null && !assignee.trim().isEmpty()) {
                            try {
                                Long assigneeId = Long.parseLong(assignee);
                                User assigneeUser = userMapper.selectById(assigneeId);
                                if (assigneeUser != null) {
                                    vo.setTaskAssigneeName(assigneeUser.getNickname() != null ? assigneeUser.getNickname() : assigneeUser.getUsername());
                                }
                            } catch (NumberFormatException e) {
                                // 如果 assignee 不是数字，可能是其他格式，忽略
                            }
                        }

                        // 检查当前用户是否可以审批
                        if (flowableWorkflowService != null) {
                            canApprove = flowableWorkflowService.canUserApproveTask(
                                    currentTask.getId(),
                                    currentUserId.toString()
                            );
                        } else if (assignee != null) {
                            // 如果没有 Flowable 服务，只检查是否是分配人
                            canApprove = assignee.equals(currentUserId.toString());
                        }
                    } else {
                        // 如果没有活跃任务，检查是否有已保存的 taskId
                        if (borrow.getTaskId() != null) {
                            vo.setTaskId(borrow.getTaskId());
                            // 检查已保存的任务是否可以审批
                            if (flowableWorkflowService != null) {
                                canApprove = flowableWorkflowService.canUserApproveTask(
                                        borrow.getTaskId(),
                                        currentUserId.toString()
                                );
                            }
                        }
                    }
                } catch (Exception e) {
                    // 流程可能已结束，使用已保存的任务信息
                    if (borrow.getTaskId() != null) {
                        vo.setTaskId(borrow.getTaskId());
                        // 检查已保存的任务是否可以审批
                        if (flowableWorkflowService != null) {
                            try {
                                canApprove = flowableWorkflowService.canUserApproveTask(
                                        borrow.getTaskId(),
                                        currentUserId.toString()
                                );
                            } catch (Exception ex) {
                                // 任务可能已不存在，忽略
                            }
                        }
                    }
                }
            } else if (borrow.getTaskId() != null) {
                // 如果没有流程实例但有任务ID，使用已保存的任务ID
                vo.setTaskId(borrow.getTaskId());
                // 检查已保存的任务是否可以审批
                if (flowableWorkflowService != null) {
                    try {
                        canApprove = flowableWorkflowService.canUserApproveTask(
                                borrow.getTaskId(),
                                currentUserId.toString()
                        );
                    } catch (Exception e) {
                        // 任务可能已不存在，忽略
                    }
                }
            } else if (borrow.getStatus() == 0) {
                // 待审批状态且没有流程任务，使用传统审批方式
                // 当前用户是资料创建者，可以审批
                canApprove = true;
            }

            // 设置是否可以审批
            vo.setCanApprove(canApprove);

            return vo;
        }).toList());
        return pageUtils;
    }

    @Override
    public boolean hasApplied(Long documentId, Long userId) {
        long count = this.count(new LambdaQueryWrapper<Borrow>()
                .eq(Borrow::getDocumentId, documentId)
                .eq(Borrow::getUserId, userId));
        return count > 0;
    }

    @Override
    public Integer getBorrowStatus(Long documentId, Long userId) {
        Borrow borrow = this.getOne(new LambdaQueryWrapper<Borrow>()
                .eq(Borrow::getDocumentId, documentId)
                .eq(Borrow::getUserId, userId)
                .orderByDesc(Borrow::getCreateTime)
                .last("LIMIT 1"));
        return borrow != null ? borrow.getStatus() : null;
    }

    @Override
    public PageUtils<BorrowVO> pageApprovedRecords(Long current, Long size, Integer status) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        String currentUserIdStr = currentUserId.toString();

        // 先查询所有可能的记录（包括传统审批和流程审批）
        LambdaQueryWrapper<Borrow> wrapper = new LambdaQueryWrapper<>();

        // 查询条件：
        // 1. 传统审批：approverId = currentUserId 且 status != 0
        // 2. 流程审批：有流程实例ID的记录（后续通过查询历史任务来过滤）
        wrapper.and(w -> {
            w.eq(Borrow::getApproverId, currentUserId)
                    .ne(Borrow::getStatus, 0)
                    .or(w2 -> w2.isNotNull(Borrow::getProcessInstanceId));
        });

        if (status != null) {
            wrapper.eq(Borrow::getStatus, status);
        }
        wrapper.orderByDesc(Borrow::getCreateTime);

        // 查询所有可能的记录
        List<Borrow> allRecords = this.list(wrapper);

        // 对于流程审批，需要查询 Flowable 历史任务，找出当前用户审批过的记录
        Set<String> approvedProcessInstanceIds = new HashSet<>();
        if (processHistoryService != null) {
            try {
                // 查询所有有流程实例的记录
                List<Borrow> processRecords = allRecords.stream()
                        .filter(b -> b.getProcessInstanceId() != null)
                        .collect(java.util.stream.Collectors.toList());

                // 查询这些流程实例的历史任务
                for (Borrow borrow : processRecords) {
                    try {
                        // 查询流程历史任务
                        List<org.flowable.task.api.history.HistoricTaskInstance> historicTasks =
                                processHistoryService.getHistoryService()
                                        .createHistoricTaskInstanceQuery()
                                        .processInstanceId(borrow.getProcessInstanceId())
                                        .list();

                        // 检查是否有当前用户审批过的任务
                        boolean hasApproved = historicTasks.stream()
                                .anyMatch(task -> {
                                    String assignee = task.getAssignee();
                                    return assignee != null && assignee.equals(currentUserIdStr)
                                            && task.getEndTime() != null; // 任务已完成
                                });

                        if (hasApproved) {
                            approvedProcessInstanceIds.add(borrow.getProcessInstanceId());
                        }
                    } catch (Exception e) {
                        // 忽略查询失败的情况
                    }
                }
            } catch (Exception e) {
                // 如果 Flowable 服务不可用，忽略
            }
        }

        // 过滤结果：只保留当前用户审批过的记录
        final Set<String> finalApprovedProcessInstanceIds = approvedProcessInstanceIds;
        List<Borrow> filteredRecords = allRecords.stream()
                .filter(borrow -> {
                    // 传统审批：approverId = currentUserId 且 status != 0
                    if (borrow.getApproverId() != null && borrow.getApproverId().equals(currentUserId)
                            && borrow.getStatus() != 0) {
                        return true;
                    }

                    // 流程审批：在已审批的流程实例列表中
                    if (borrow.getProcessInstanceId() != null
                            && finalApprovedProcessInstanceIds.contains(borrow.getProcessInstanceId())) {
                        return true;
                    }

                    return false;
                })
                .collect(java.util.stream.Collectors.toList());

        // 手动分页
        long total = filteredRecords.size();
        int start = (int) ((current - 1) * size);
        int end = (int) Math.min(start + size, total);
        List<Borrow> pagedRecords = start < total ? filteredRecords.subList(start, end) : new ArrayList<>();

        PageUtils<BorrowVO> pageUtils = new PageUtils<>();
        pageUtils.setCurrent(current);
        pageUtils.setSize(size);
        pageUtils.setTotal(total);
        pageUtils.setRecords(pagedRecords.stream().map(borrow -> {
            BorrowVO vo = new BorrowVO();
            BeanUtils.copyProperties(borrow, vo);

            // 查询资料标题
            if (borrow.getDocumentId() != null) {
                Document document = documentService.getById(borrow.getDocumentId());
                if (document != null) {
                    vo.setDocumentTitle(document.getTitle());
                }
            }

            // 查询借阅人名称
            if (borrow.getUserId() != null) {
                User user = userMapper.selectById(borrow.getUserId());
                if (user != null) {
                    vo.setUserName(user.getNickname() != null ? user.getNickname() : user.getUsername());
                }
            }

            // 查询审批人名称
            if (borrow.getApproverId() != null) {
                User approver = userMapper.selectById(borrow.getApproverId());
                if (approver != null) {
                    vo.setApproverName(approver.getNickname() != null ? approver.getNickname() : approver.getUsername());
                }
            }

            // 计算流程状态
            if (borrow.getProcessInstanceId() != null && processHistoryService != null) {
                try {
                    // 查询流程实例历史
                    org.flowable.engine.history.HistoricProcessInstance processInstance =
                            processHistoryService.getHistoryService()
                                    .createHistoricProcessInstanceQuery()
                                    .processInstanceId(borrow.getProcessInstanceId())
                                    .singleResult();

                    if (processInstance != null) {
                        // 如果流程实例已结束，说明所有节点都审批完成
                        if (processInstance.getEndTime() != null) {
                            vo.setProcessStatus("审批完成");
                        } else {
                            // 流程实例未结束，查询历史任务
                            List<org.flowable.task.api.history.HistoricTaskInstance> historicTasks =
                                    processHistoryService.getHistoryService()
                                            .createHistoricTaskInstanceQuery()
                                            .processInstanceId(borrow.getProcessInstanceId())
                                            .orderByHistoricTaskInstanceStartTime()
                                            .asc()
                                            .list();

                            if (historicTasks == null || historicTasks.isEmpty()) {
                                // 没有任务，说明还没有节点审批通过
                                vo.setProcessStatus("待审批");
                            } else {
                                // 统计已完成和未完成的任务
                                long completedTaskCount = historicTasks.stream()
                                        .filter(task -> task.getEndTime() != null)
                                        .count();
                                long unfinishedTaskCount = historicTasks.stream()
                                        .filter(task -> task.getEndTime() == null)
                                        .count();

                                if (completedTaskCount == 0) {
                                    // 没有任何任务完成，说明还没有节点审批通过
                                    vo.setProcessStatus("待审批");
                                } else if (unfinishedTaskCount > 0) {
                                    // 有已完成的任务，但还有未完成的任务，说明正在审批中
                                    vo.setProcessStatus("审批中");
                                } else {
                                    // 所有任务都完成，但流程实例未结束（可能是流程定义问题或流程还未完全结束）
                                    // 这种情况也视为审批完成
                                    vo.setProcessStatus("审批完成");
                                }
                            }
                        }
                    } else {
                        // 流程实例不存在，可能是传统审批
                        vo.setProcessStatus(null);
                    }
                } catch (Exception e) {
                    // 查询失败，设置为null
                    vo.setProcessStatus(null);
                }
            } else {
                // 没有流程实例，使用传统审批，processStatus 为 null
                vo.setProcessStatus(null);
            }

            return vo;
        }).toList());
        return pageUtils;
    }
}

