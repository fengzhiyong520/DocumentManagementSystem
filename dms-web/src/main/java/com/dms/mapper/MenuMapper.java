package com.dms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dms.entity.Menu;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 菜单Mapper
 */
@Mapper
public interface MenuMapper extends BaseMapper<Menu> {
    /**
     * 查询所有启用的菜单（树形结构）
     */
    List<Menu> selectMenuTree();
    
    /**
     * 根据父ID查询子菜单
     */
    List<Menu> selectByParentId(Long parentId);
}

