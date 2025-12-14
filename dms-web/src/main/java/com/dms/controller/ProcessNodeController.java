package com.dms.controller;

import com.dms.core.domain.R;
import com.dms.entity.ProcessNode;
import com.dms.service.ProcessNodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 流程节点管理控制�?
 */
@Tag(name = "流程节点管理")
@RestController
@RequestMapping("/process-node")
@RequiredArgsConstructor
public class ProcessNodeController {

    private final ProcessNodeService processNodeService;

    @Operation(summary = "根据流程定义ID获取节点列表")
    @GetMapping("/process-definition/{processDefinitionId}")
    public R<List<ProcessNode>> getNodesByProcessDefinitionId(@PathVariable Long processDefinitionId) {
        List<ProcessNode> nodes = processNodeService.getNodesByProcessDefinitionId(processDefinitionId);
        return R.success(nodes);
    }

    @Operation(summary = "保存流程节点列表")
    @PostMapping("/process-definition/{processDefinitionId}")
    public R<?> saveNodes(@PathVariable Long processDefinitionId, @RequestBody List<ProcessNode> nodes) {
        processNodeService.saveNodes(processDefinitionId, nodes);
        return R.success("保存成功");
    }

    @Operation(summary = "更新流程节点列表")
    @PutMapping("/process-definition/{processDefinitionId}")
    public R<?> updateNodes(@PathVariable Long processDefinitionId, @RequestBody List<ProcessNode> nodes) {
        processNodeService.updateNodes(processDefinitionId, nodes);
        return R.success("更新成功");
    }

    @Operation(summary = "删除流程节点")
    @DeleteMapping("/{id}")
    public R<?> deleteNode(@PathVariable Long id) {
        processNodeService.removeById(id);
        return R.success("删除成功");
    }

    @Operation(summary = "删除流程定义的所有节点")
    @DeleteMapping("/process-definition/{processDefinitionId}")
    public R<?> deleteNodesByProcessDefinitionId(@PathVariable Long processDefinitionId) {
        processNodeService.deleteNodesByProcessDefinitionId(processDefinitionId);
        return R.success("删除成功");
    }
}

