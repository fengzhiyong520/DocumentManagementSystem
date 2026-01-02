package com.dms.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色菜单关联Mapper
 */
@Mapper
public interface RoleMenuMapper {
    
    /**
     * 批量插入角色菜单关联
     */
    @Insert("<script>" +
            "INSERT INTO sys_role_menu (role_id, menu_id) VALUES " +
            "<foreach collection='menuIds' item='menuId' separator=','>" +
            "(#{roleId}, #{menuId})" +
            "</foreach>" +
            "</script>")
    void insertBatch(@Param("roleId") Long roleId, @Param("menuIds") List<Long> menuIds);
    
    /**
     * 删除角色的所有菜单关联
     */
    @Delete("DELETE FROM sys_role_menu WHERE role_id = #{roleId}")
    void deleteByRoleId(@Param("roleId") Long roleId);
    
    /**
     * 查询角色的菜单ID列表
     */
    @Select("SELECT menu_id FROM sys_role_menu WHERE role_id = #{roleId}")
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);
    
    /**
     * 根据用户角色ID列表查询菜单ID列表
     */
    @Select("<script>" +
            "SELECT DISTINCT menu_id FROM sys_role_menu WHERE role_id IN " +
            "<foreach collection='roleIds' item='roleId' open='(' separator=',' close=')'>" +
            "#{roleId}" +
            "</foreach>" +
            "</script>")
    List<Long> selectMenuIdsByRoleIds(@Param("roleIds") List<Long> roleIds);
}

