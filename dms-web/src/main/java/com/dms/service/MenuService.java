package com.dms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dms.dto.MenuDTO;
import com.dms.entity.Menu;
import com.dms.vo.MenuVO;

import java.util.List;

/**
 * 菜单服务接口
 */
public interface MenuService extends IService<Menu> {
    /**
     * 获取所有菜单（树形结构）
     */
    List<MenuVO> getMenuTree();
    
    /**
     * 根据ID获取菜单
     */
    MenuVO getMenuById(Long id);
    
    /**
     * 创建菜单
     */
    void createMenu(MenuDTO menuDTO);
    
    /**
     * 更新菜单
     */
    void updateMenu(MenuDTO menuDTO);
    
    /**
     * 删除菜单
     */
    void deleteMenu(Long id);
    
    /**
     * 根据菜单ID列表获取菜单树
     */
    List<MenuVO> getMenuTreeByIds(List<Long> menuIds);
}

