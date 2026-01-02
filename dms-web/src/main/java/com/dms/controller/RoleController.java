package com.dms.controller;

import com.dms.core.domain.R;
import com.dms.utils.PageUtils;
import com.dms.dto.RoleDTO;
import com.dms.service.RolePermissionService;
import com.dms.service.RoleService;
import com.dms.vo.RoleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理控制�?
 */
@Tag(name = "角色管理")
@RestController
@RequestMapping("/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;
    private final RolePermissionService rolePermissionService;
    private final com.dms.service.RoleMenuService roleMenuService;

    @Operation(summary = "获取所有角色列表")
    @GetMapping("/list")
    public R<List<RoleVO>> listRoles() {
        List<RoleVO> roles = roleService.list().stream().map(role -> {
            RoleVO vo = new RoleVO();
            org.springframework.beans.BeanUtils.copyProperties(role, vo);
            return vo;
        }).toList();
        return R.success(roles);
    }

    @Operation(summary = "分页查询角色")
    @GetMapping("/page")
    public R<PageUtils<RoleVO>> pageRole(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String keyword) {
        PageUtils<RoleVO> page = roleService.pageRole(current, size, keyword);
        return R.success(page);
    }

    @Operation(summary = "根据ID获取角色")
    @GetMapping("/{id}")
    public R<RoleVO> getRoleById(@PathVariable Long id) {
        RoleVO role = roleService.getRoleById(id);
        return R.success(role);
    }

    @Operation(summary = "创建角色")
    @PostMapping
    public R<?> createRole(@Valid @RequestBody RoleDTO roleDTO) {
        roleService.createRole(roleDTO);
        return R.success("创建成功");
    }

    @Operation(summary = "更新角色")
    @PutMapping
    public R<?> updateRole(@Valid @RequestBody RoleDTO roleDTO) {
        roleService.updateRole(roleDTO);
        return R.success("更新成功");
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    public R<?> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return R.success("删除成功");
    }

    @Operation(summary = "启用/禁用角色")
    @PutMapping("/status/{id}")
    public R<?> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        roleService.changeStatus(id, status);
        return R.success("操作成功");
    }

    @Operation(summary = "获取角色的权限ID列表")
    @GetMapping("/{id}/permissions")
    public R<List<Long>> getRolePermissions(@PathVariable Long id) {
        List<Long> permissionIds = rolePermissionService.getPermissionIdsByRoleId(id);
        return R.success(permissionIds);
    }

    @Operation(summary = "保存角色的权限配置")
    @PutMapping("/{id}/permissions")
    public R<?> saveRolePermissions(@PathVariable Long id, @RequestBody List<Long> permissionIds) {
        rolePermissionService.saveRolePermissions(id, permissionIds);
        return R.success("保存成功");
    }

    @Operation(summary = "获取角色的菜单ID列表")
    @GetMapping("/{id}/menus")
    public R<List<Long>> getRoleMenus(@PathVariable Long id) {
        List<Long> menuIds = roleMenuService.getMenuIdsByRoleId(id);
        return R.success(menuIds);
    }

    @Operation(summary = "保存角色的菜单配置")
    @PutMapping("/{id}/menus")
    public R<?> saveRoleMenus(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        roleMenuService.saveRoleMenus(id, menuIds);
        return R.success("保存成功");
    }
}

