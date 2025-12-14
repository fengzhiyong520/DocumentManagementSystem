package com.dms.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dms.utils.PageUtils;
import com.dms.dto.UserDTO;
import com.dms.entity.User;
import com.dms.entity.Role;
import com.dms.mapper.UserMapper;
import com.dms.mapper.RoleMapper;
import com.dms.service.UserService;
import com.dms.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 用户服务实现
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final RoleMapper roleMapper;

    @Value("${file.upload.local-path:D:/dms/upload}")
    private String localPath;

    @Override
    public String login(String username, String password) {
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new RuntimeException("用户已被禁用");
        }

        // 检查密码哈希格式是否正确
        if (user.getPassword() == null || !user.getPassword().startsWith("$2")) {
            throw new RuntimeException("用户密码格式错误，请联系管理员重置密码");
        }

        // 验证密码
        boolean passwordMatches = BCrypt.checkpw(password, user.getPassword());
        if (!passwordMatches) {
            // 如果密码不匹配，可能是数据库中的哈希值不正确
            // 尝试使用重置密码 API 来修复
            throw new RuntimeException("用户名或密码错误。如果确认密码正确，请使用 /api/auth/reset-admin-password 接口重置密码");
        }
        // 登录
        StpUtil.login(user.getId());
        // 设置角色
        if (user.getRoleId() != null) {
            StpUtil.getRoleList(user.getId()).forEach(role -> StpUtil.getSession().set("role", role));
        }
        return StpUtil.getTokenValue();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(UserDTO userDTO) {
        // 检查用户名是否已存在
        long count = this.count(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, userDTO.getUsername()));
        if (count > 0) {
            throw new RuntimeException("用户名已存在");
        }
        User user = new User();
        BeanUtils.copyProperties(userDTO, user);
        user.setPassword(BCrypt.hashpw(userDTO.getPassword(), BCrypt.gensalt()));
        user.setStatus(1);
        this.save(user);
    }

    @Override
    public PageUtils<UserVO> pageUser(Long current, Long size, String keyword) {
        Page<User> page = new Page<>(current, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getNickname, keyword)
                    .or().like(User::getEmail, keyword));
        }
        Page<User> result = this.page(page, wrapper);
        PageUtils<UserVO> pageUtils = new PageUtils<>();
        pageUtils.setCurrent(result.getCurrent());
        pageUtils.setSize(result.getSize());
        pageUtils.setTotal(result.getTotal());
        pageUtils.setRecords(result.getRecords().stream().map(user -> {
            UserVO vo = new UserVO();
            BeanUtils.copyProperties(user, vo);
            // 设置角色ID
            vo.setRoleId(user.getRoleId());
            // 查询角色名称
            if (user.getRoleId() != null) {
                Role role = roleMapper.selectById(user.getRoleId());
                if (role != null) {
                    vo.setRoleName(role.getRoleName());
                }
            }
            return vo;
        }).toList());
        return pageUtils;
    }

    @Override
    public UserVO getUserById(Long id) {
        User user = this.getById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        // 设置角色ID
        vo.setRoleId(user.getRoleId());
        // 查询角色名称
        if (user.getRoleId() != null) {
            Role role = roleMapper.selectById(user.getRoleId());
            if (role != null) {
                vo.setRoleName(role.getRoleName());
            }
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createUser(UserDTO userDTO) {
        long count = this.count(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, userDTO.getUsername()));
        if (count > 0) {
            throw new RuntimeException("用户名已存在");
        }
        User user = new User();
        BeanUtils.copyProperties(userDTO, user);
        if (StringUtils.hasText(userDTO.getPassword())) {
            user.setPassword(BCrypt.hashpw(userDTO.getPassword(), BCrypt.gensalt()));
        }
        this.save(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(UserDTO userDTO) {
        User user = this.getById(userDTO.getId());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        // 复制属性，排除密码和用户名
        BeanUtils.copyProperties(userDTO, user, "password", "username");
        // 如果提供了新密码，则更新
        if (StringUtils.hasText(userDTO.getPassword())) {
            user.setPassword(BCrypt.hashpw(userDTO.getPassword(), BCrypt.gensalt()));
        }
        this.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        this.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long id, Integer status) {
        User user = this.getById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setStatus(status);
        this.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importUsers(java.util.List<UserDTO> userList) {
        for (UserDTO userDTO : userList) {
            // 检查用户名是否已存在
            long count = this.count(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, userDTO.getUsername()));
            if (count == 0) {
                User user = new User();
                BeanUtils.copyProperties(userDTO, user);
                if (StringUtils.hasText(userDTO.getPassword())) {
                    user.setPassword(BCrypt.hashpw(userDTO.getPassword(), BCrypt.gensalt()));
                } else {
                    user.setPassword(BCrypt.hashpw("123456", BCrypt.gensalt())); // 默认密码
                }
                if (user.getStatus() == null) {
                    user.setStatus(1);
                }
                this.save(user);
            }
        }
    }

    @Override
    public java.util.List<UserVO> exportUsers() {
        // 查询所有未删除的用户（MyBatis-Plus会自动过滤逻辑删除的数据）
        java.util.List<User> users = this.list();
        return users.stream().map(user -> {
            UserVO vo = new UserVO();
            BeanUtils.copyProperties(user, vo);
            // 设置角色ID
            vo.setRoleId(user.getRoleId());
            // 查询角色名称
            if (user.getRoleId() != null) {
                Role role = roleMapper.selectById(user.getRoleId());
                if (role != null) {
                    vo.setRoleName(role.getRoleName());
                }
            }
            return vo;
        }).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String uploadAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }

        // 检查文件类型（只允许图片）
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.matches(".*\\.(jpg|jpeg|png|gif|bmp|webp)$")) {
            throw new RuntimeException("只支持图片格式：jpg、jpeg、png、gif、bmp、webp");
        }

        try {
            // 获取用户信息
            User user = this.getById(userId);
            if (user == null) {
                throw new RuntimeException("用户不存在");
            }
            String username = user.getUsername();

            // 生成文件路径：avatar/{用户名}/uuid.扩展名（不带日期）
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = cn.hutool.core.util.IdUtil.simpleUUID() + extension;
            String relativePath = "avatar/" + username + "/" + fileName;

            // 使用配置的本地路径
            String fullPath = localPath + "/" + relativePath;

            // 创建目录
            java.io.File parentDir = new java.io.File(fullPath).getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }

            // 获取旧头像URL并删除
            if (StringUtils.hasText(user.getAvatar())) {
                try {
                    String oldFullPath = localPath + "/" + user.getAvatar();
                    java.io.File oldFile = new java.io.File(oldFullPath);
                    if (oldFile.exists() && oldFile.isFile()) {
                        oldFile.delete();
                    }
                } catch (Exception e) {
                    // 忽略删除旧文件失败的情况
                }
            }

            // 保存新文件
            file.transferTo(new java.io.File(fullPath));

            // 更新用户头像URL
            user.setAvatar(relativePath);
            this.updateById(user);

            return relativePath;
        } catch (IOException e) {
            throw new RuntimeException("头像上传失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetAdminPassword(String newPassword) {
        User admin = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, "admin"));
        if (admin == null) {
            throw new RuntimeException("管理员用户不存在");
        }
        admin.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        this.updateById(admin);
    }
}

