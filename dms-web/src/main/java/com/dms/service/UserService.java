package com.dms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dms.utils.PageUtils;
import com.dms.dto.UserDTO;
import com.dms.entity.User;
import com.dms.vo.UserVO;

/**
 * 用户服务接口
 */
public interface UserService extends IService<User> {
    /**
     * 用户登录
     */
    String login(String username, String password);

    /**
     * 用户注册
     */
    void register(UserDTO userDTO);

    /**
     * 分页查询用户
     */
    PageUtils<UserVO> pageUser(Long current, Long size, String keyword);

    /**
     * 根据ID获取用户
     */
    UserVO getUserById(Long id);

    /**
     * 创建用户
     */
    void createUser(UserDTO userDTO);

    /**
     * 更新用户
     */
    void updateUser(UserDTO userDTO);

    /**
     * 删除用户
     */
    void deleteUser(Long id);

    /**
     * 启用/禁用用户
     */
    void changeStatus(Long id, Integer status);

    /**
     * 批量导入用户
     */
    void importUsers(java.util.List<UserDTO> userList);

    /**
     * 导出用户列表
     */
    java.util.List<UserVO> exportUsers();

    /**
     * 上传头像
     */
    String uploadAvatar(Long userId, org.springframework.web.multipart.MultipartFile file);

    /**
     * 重置管理员密码（临时工具，生产环境请删除�?
     */
    void resetAdminPassword(String newPassword);
    
    /**
     * 获取当前用户的菜单列表
     */
    java.util.List<com.dms.vo.MenuVO> getCurrentUserMenus();
}

