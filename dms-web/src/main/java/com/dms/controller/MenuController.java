package com.dms.controller;

import com.dms.core.domain.R;
import com.dms.dto.MenuDTO;
import com.dms.service.MenuService;
import com.dms.vo.MenuVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单管理控制器
 */
@Tag(name = "菜单管理")
@RestController
@RequestMapping("/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @Operation(summary = "获取菜单树")
    @GetMapping("/tree")
    public R<List<MenuVO>> getMenuTree() {
        List<MenuVO> menuTree = menuService.getMenuTree();
        return R.success(menuTree);
    }

    @Operation(summary = "根据ID获取菜单")
    @GetMapping("/{id}")
    public R<MenuVO> getMenuById(@PathVariable Long id) {
        MenuVO menu = menuService.getMenuById(id);
        return R.success(menu);
    }

    @Operation(summary = "创建菜单")
    @PostMapping
    public R<?> createMenu(@Valid @RequestBody MenuDTO menuDTO) {
        menuService.createMenu(menuDTO);
        return R.success("创建成功");
    }

    @Operation(summary = "更新菜单")
    @PutMapping
    public R<?> updateMenu(@Valid @RequestBody MenuDTO menuDTO) {
        menuService.updateMenu(menuDTO);
        return R.success("更新成功");
    }

    @Operation(summary = "删除菜单")
    @DeleteMapping("/{id}")
    public R<?> deleteMenu(@PathVariable Long id) {
        menuService.deleteMenu(id);
        return R.success("删除成功");
    }
}

