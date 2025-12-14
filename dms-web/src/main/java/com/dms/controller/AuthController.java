package com.dms.controller;

import com.dms.core.domain.R;
import com.dms.dto.UserDTO;
import com.dms.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@Tag(name = "认证管理")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public R<String> login(@RequestParam String username, @RequestParam String password) {
        String token = userService.login(username, password);
        return R.success("登录成功", token);
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public R<?> register(@Valid @RequestBody UserDTO userDTO) {
        userService.register(userDTO);
        return R.success("注册成功");
    }

    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public R<?> logout() {
        cn.dev33.satoken.stp.StpUtil.logout();
        return R.success("登出成功");
    }

    @Operation(summary = "重置管理员密码（临时工具，生产环境请删除）")
    @PostMapping("/reset-admin-password")
    public R<?> resetAdminPassword(@RequestParam String newPassword) {
        userService.resetAdminPassword(newPassword);
        return R.success("管理员密码重置成功");
    }
}
