package com.dms.controller;

import com.dms.core.domain.R;
import com.dms.utils.PageUtils;
import com.dms.dto.PermissionDTO;
import com.dms.service.PermissionService;
import com.dms.vo.PermissionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限管理控制�?
 */
@Tag(name = "权限管理")
@RestController
@RequestMapping("/permission")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @Operation(summary = "分页查询权限")
    @GetMapping("/page")
    public R<PageUtils<PermissionVO>> pagePermission(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String keyword) {
        PageUtils<PermissionVO> page = permissionService.pagePermission(current, size, keyword);
        return R.success(page);
    }

    @Operation(summary = "获取所有权限列表")
    @GetMapping("/list")
    public R<List<PermissionVO>> listPermissions() {
        List<PermissionVO> permissions = permissionService.listAllPermissions();
        return R.success(permissions);
    }

    @Operation(summary = "根据ID获取权限")
    @GetMapping("/{id}")
    public R<PermissionVO> getPermissionById(@PathVariable Long id) {
        PermissionVO permission = permissionService.getPermissionById(id);
        return R.success(permission);
    }

    @Operation(summary = "创建权限")
    @PostMapping
    public R<?> createPermission(@Valid @RequestBody PermissionDTO permissionDTO) {
        permissionService.createPermission(permissionDTO);
        return R.success("创建成功");
    }

    @Operation(summary = "更新权限")
    @PutMapping
    public R<?> updatePermission(@Valid @RequestBody PermissionDTO permissionDTO) {
        permissionService.updatePermission(permissionDTO);
        return R.success("更新成功");
    }

    @Operation(summary = "删除权限")
    @DeleteMapping("/{id}")
    public R<?> deletePermission(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return R.success("删除成功");
    }
}

