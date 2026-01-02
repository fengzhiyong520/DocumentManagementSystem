package com.dms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dms.dto.MenuDTO;
import com.dms.entity.Menu;
import com.dms.mapper.MenuMapper;
import com.dms.service.MenuService;
import com.dms.vo.MenuVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜单服务实现
 */
@Service
@RequiredArgsConstructor
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements MenuService {

    @Override
    public List<MenuVO> getMenuTree() {
        // 查询所有启用的菜单
        List<Menu> menus = this.list(new LambdaQueryWrapper<Menu>()
                .eq(Menu::getStatus, 1)
                .orderByAsc(Menu::getSortOrder)
                .orderByAsc(Menu::getId));
        
        return buildMenuTree(menus, 0L);
    }
    
    @Override
    public List<MenuVO> getMenuTreeByIds(List<Long> menuIds) {
        if (menuIds == null || menuIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 查询指定ID的菜单
        List<Menu> menus = this.list(new LambdaQueryWrapper<Menu>()
                .in(Menu::getId, menuIds)
                .eq(Menu::getStatus, 1)
                .orderByAsc(Menu::getSortOrder)
                .orderByAsc(Menu::getId));
        
        return buildMenuTree(menus, 0L);
    }
    
    /**
     * 构建菜单树
     */
    private List<MenuVO> buildMenuTree(List<Menu> menus, Long parentId) {
        return menus.stream()
                .filter(menu -> parentId.equals(menu.getParentId() != null ? menu.getParentId() : 0L))
                .map(menu -> {
                    MenuVO vo = new MenuVO();
                    BeanUtils.copyProperties(menu, vo);
                    // 递归构建子菜单
                    List<MenuVO> children = buildMenuTree(menus, menu.getId());
                    if (!children.isEmpty()) {
                        vo.setChildren(children);
                    }
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public MenuVO getMenuById(Long id) {
        Menu menu = this.getById(id);
        if (menu == null) {
            throw new RuntimeException("菜单不存在");
        }
        MenuVO vo = new MenuVO();
        BeanUtils.copyProperties(menu, vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createMenu(MenuDTO menuDTO) {
        // 检查菜单编码是否已存在
        long count = this.count(new LambdaQueryWrapper<Menu>()
                .eq(Menu::getMenuCode, menuDTO.getMenuCode()));
        if (count > 0) {
            throw new RuntimeException("菜单编码已存在");
        }
        
        // 检查父菜单是否存在
        if (menuDTO.getParentId() != null && menuDTO.getParentId() > 0) {
            Menu parentMenu = this.getById(menuDTO.getParentId());
            if (parentMenu == null) {
                throw new RuntimeException("父菜单不存在");
            }
        }
        
        Menu menu = new Menu();
        BeanUtils.copyProperties(menuDTO, menu);
        if (menu.getParentId() == null) {
            menu.setParentId(0L);
        }
        if (menu.getMenuType() == null || menu.getMenuType().isEmpty()) {
            menu.setMenuType("menu");
        }
        if (menu.getStatus() == null) {
            menu.setStatus(1);
        }
        if (menu.getSortOrder() == null) {
            menu.setSortOrder(0);
        }
        this.save(menu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMenu(MenuDTO menuDTO) {
        Menu menu = this.getById(menuDTO.getId());
        if (menu == null) {
            throw new RuntimeException("菜单不存在");
        }
        
        // 检查菜单编码是否重复（如果修改了编码）
        if (!menu.getMenuCode().equals(menuDTO.getMenuCode())) {
            long count = this.count(new LambdaQueryWrapper<Menu>()
                    .eq(Menu::getMenuCode, menuDTO.getMenuCode())
                    .ne(Menu::getId, menuDTO.getId()));
            if (count > 0) {
                throw new RuntimeException("菜单编码已存在");
            }
        }
        
        // 检查父菜单是否存在（不能将自己设为父菜单）
        if (menuDTO.getParentId() != null && menuDTO.getParentId() > 0) {
            if (menuDTO.getParentId().equals(menuDTO.getId())) {
                throw new RuntimeException("不能将自己设为父菜单");
            }
            Menu parentMenu = this.getById(menuDTO.getParentId());
            if (parentMenu == null) {
                throw new RuntimeException("父菜单不存在");
            }
        }
        
        BeanUtils.copyProperties(menuDTO, menu);
        if (menu.getParentId() == null) {
            menu.setParentId(0L);
        }
        this.updateById(menu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMenu(Long id) {
        Menu menu = this.getById(id);
        if (menu == null) {
            throw new RuntimeException("菜单不存在");
        }
        
        // 检查是否有子菜单
        long childCount = this.count(new LambdaQueryWrapper<Menu>()
                .eq(Menu::getParentId, id));
        if (childCount > 0) {
            throw new RuntimeException("该菜单下存在子菜单，无法删除");
        }
        
        this.removeById(id);
    }
}

