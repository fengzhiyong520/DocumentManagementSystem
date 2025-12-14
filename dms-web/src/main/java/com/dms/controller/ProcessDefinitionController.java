package com.dms.controller;

import com.dms.core.domain.R;
import com.dms.entity.ProcessDefinition;
import com.dms.service.ProcessDefinitionService;
import com.dms.utils.PageUtils;
import com.dms.vo.ProcessDefinitionWithNodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 流程定义管理控制�?
 */
@Tag(name = "流程定义管理")
@RestController
@RequestMapping("/process-definition")
@RequiredArgsConstructor
public class ProcessDefinitionController {

    private final ProcessDefinitionService processDefinitionService;

    @Operation(summary = "获取启用的流程定义列表")
    @GetMapping("/enabled")
    public R<List<ProcessDefinition>> getEnabledProcessDefinitions() {
        List<ProcessDefinition> list = processDefinitionService.getEnabledProcessDefinitions();
        return R.success(list);
    }

    @Operation(summary = "分页查询流程定义列表")
    @GetMapping("/page")
    public R<PageUtils<ProcessDefinition>> pageProcessDefinition(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String keyword) {
        PageUtils<ProcessDefinition> page = processDefinitionService.pageProcessDefinition(current, size, keyword);
        return R.success(page);
    }

    @Operation(summary = "获取流程定义详情")
    @GetMapping("/{id}")
    public R<ProcessDefinition> getProcessDefinition(@PathVariable Long id) {
        ProcessDefinition processDefinition = processDefinitionService.getById(id);
        return R.success(processDefinition);
    }

    @Operation(summary = "保存流程定义")
    @PostMapping
    public R<?> saveProcessDefinition(@RequestBody ProcessDefinition processDefinition) {
        processDefinitionService.saveProcessDefinition(processDefinition);
        return R.success("保存成功");
    }

    @Operation(summary = "更新流程定义")
    @PutMapping
    public R<?> updateProcessDefinition(@RequestBody ProcessDefinition processDefinition) {
        processDefinitionService.updateProcessDefinition(processDefinition);
        return R.success("更新成功");
    }

    @Operation(summary = "获取流程定义详情（含节点）")
    @GetMapping("/{id}/with-nodes")
    public R<ProcessDefinitionWithNodes> getProcessDefinitionWithNodes(@PathVariable Long id) {
        ProcessDefinitionWithNodes vo = processDefinitionService.getProcessDefinitionWithNodesVO(id);
        return R.success(vo);
    }

    @Operation(summary = "删除流程定义")
    @DeleteMapping("/{id}")
    public R<?> deleteProcessDefinition(@PathVariable Long id) {
        processDefinitionService.removeById(id);
        return R.success("删除成功");
    }

    @Operation(summary = "手动部署流程定义")
    @PostMapping("/{id}/deploy")
    public R<?> deployProcessDefinition(@PathVariable Long id) {
        try {
            processDefinitionService.deployProcessDefinition(id);
            return R.success("部署成功");
        } catch (Exception e) {
            return R.error("部署失败：" + e.getMessage());
        }
    }
}

