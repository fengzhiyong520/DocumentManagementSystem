package com.dms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dms.utils.PageUtils;
import com.dms.dto.PermissionDTO;
import com.dms.entity.Permission;
import com.dms.mapper.PermissionMapper;
import com.dms.service.PermissionService;
import com.dms.vo.PermissionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 权限服务实现
 */
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {

    @Override
    public PageUtils<PermissionVO> pagePermission(Long current, Long size, String keyword) {
        Page<Permission> page = new Page<>(current, size);
        LambdaQueryWrapper<Permission> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Permission::getPermissionCode, keyword)
                    .or().like(Permission::getPermissionName, keyword)
                    .or().like(Permission::getResource, keyword)
                    .or().like(Permission::getDescription, keyword));
        }
        wrapper.orderByAsc(Permission::getPermissionCode);
        Page<Permission> result = this.page(page, wrapper);
        PageUtils<PermissionVO> pageUtils = new PageUtils<>();
        BeanUtils.copyProperties(result, pageUtils);
        pageUtils.setRecords(result.getRecords().stream().map(permission -> {
            PermissionVO vo = new PermissionVO();
            BeanUtils.copyProperties(permission, vo);
            return vo;
        }).toList());
        return pageUtils;
    }

    @Override
    public List<PermissionVO> listAllPermissions() {
        List<Permission> permissions = this.list(new LambdaQueryWrapper<Permission>()
                .orderByAsc(Permission::getPermissionCode));
        return permissions.stream().map(permission -> {
            PermissionVO vo = new PermissionVO();
            BeanUtils.copyProperties(permission, vo);
            return vo;
        }).toList();
    }

    @Override
    public PermissionVO getPermissionById(Long id) {
        Permission permission = this.getById(id);
        if (permission == null) {
            throw new RuntimeException("权限不存在");
        }
        PermissionVO vo = new PermissionVO();
        BeanUtils.copyProperties(permission, vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createPermission(PermissionDTO permissionDTO) {
        long count = this.count(new LambdaQueryWrapper<Permission>()
                .eq(Permission::getPermissionCode, permissionDTO.getPermissionCode()));
        if (count > 0) {
            throw new RuntimeException("权限编码已存在");
        }
        Permission permission = new Permission();
        BeanUtils.copyProperties(permissionDTO, permission);
        this.save(permission);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePermission(PermissionDTO permissionDTO) {
        Permission permission = this.getById(permissionDTO.getId());
        if (permission == null) {
            throw new RuntimeException("权限不存在");
        }
        // 检查权限编码是否重复（如果修改了编码）
        if (!permission.getPermissionCode().equals(permissionDTO.getPermissionCode())) {
            long count = this.count(new LambdaQueryWrapper<Permission>()
                    .eq(Permission::getPermissionCode, permissionDTO.getPermissionCode())
                    .ne(Permission::getId, permissionDTO.getId()));
            if (count > 0) {
                throw new RuntimeException("权限编码已存在");
            }
        }
        BeanUtils.copyProperties(permissionDTO, permission);
        this.updateById(permission);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePermission(Long id) {
        this.removeById(id);
    }
}

