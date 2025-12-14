package com.dms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dms.utils.PageUtils;
import com.dms.dto.RoleDTO;
import com.dms.entity.Role;
import com.dms.mapper.RoleMapper;
import com.dms.service.RoleService;
import com.dms.vo.RoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 角色服务实现
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    @Override
    public PageUtils<RoleVO> pageRole(Long current, Long size, String keyword) {
        Page<Role> page = new Page<>(current, size);
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Role::getRoleCode, keyword)
                    .or().like(Role::getRoleName, keyword)
                    .or().like(Role::getDescription, keyword));
        }

        wrapper.orderByDesc(Role::getCreateTime);

        Page<Role> result = this.page(page, wrapper);
        PageUtils<RoleVO> pageUtils = new PageUtils<>();
        pageUtils.setCurrent(result.getCurrent());
        pageUtils.setSize(result.getSize());
        pageUtils.setTotal(result.getTotal());
        pageUtils.setRecords(result.getRecords().stream().map(role -> {
            RoleVO vo = new RoleVO();
            BeanUtils.copyProperties(role, vo);
            return vo;
        }).toList());
        return pageUtils;
    }

    @Override
    public RoleVO getRoleById(Long id) {
        Role role = this.getById(id);
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }
        RoleVO vo = new RoleVO();
        BeanUtils.copyProperties(role, vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createRole(RoleDTO roleDTO) {
        // 检查角色编码是否已存在
        long count = this.count(new LambdaQueryWrapper<Role>()
                .eq(Role::getRoleCode, roleDTO.getRoleCode()));
        if (count > 0) {
            throw new RuntimeException("角色编码已存在");
        }

        Role role = new Role();
        BeanUtils.copyProperties(roleDTO, role);
        if (role.getStatus() == null) {
            role.setStatus(1); // 默认启用
        }
        this.save(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(RoleDTO roleDTO) {
        Role role = this.getById(roleDTO.getId());
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }

        // 检查角色编码是否已被其他角色使用
        if (StringUtils.hasText(roleDTO.getRoleCode()) && !roleDTO.getRoleCode().equals(role.getRoleCode())) {
            long count = this.count(new LambdaQueryWrapper<Role>()
                    .eq(Role::getRoleCode, roleDTO.getRoleCode())
                    .ne(Role::getId, roleDTO.getId()));
            if (count > 0) {
                throw new RuntimeException("角色编码已存在");
            }
        }

        BeanUtils.copyProperties(roleDTO, role, "id");
        this.updateById(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        Role role = this.getById(id);
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }
        this.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long id, Integer status) {
        Role role = this.getById(id);
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }
        role.setStatus(status);
        this.updateById(role);
    }
}

