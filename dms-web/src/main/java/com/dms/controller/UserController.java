package com.dms.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.dms.core.domain.R;
import com.dms.utils.PageUtils;
import com.dms.dto.UserDTO;
import com.dms.entity.User;
import com.dms.service.UserService;
import com.dms.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户管理控制器
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Value("${file.upload.local-path:D:/dms/upload}")
    private String localPath;

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/info")
    public R<UserVO> getCurrentUserInfo() {
        Long userId = cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
        UserVO user = userService.getUserById(userId);
        return R.success(user);
    }

    @Operation(summary = "更新当前用户信息")
    @PutMapping("/info")
    public R<?> updateCurrentUserInfo(@RequestBody UserDTO userDTO) {
        Long userId = cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
        userDTO.setId(userId);
        // 只更新昵称和头像，不更新其他字段
        User user = userService.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (StringUtils.hasText(userDTO.getNickname())) {
            user.setNickname(userDTO.getNickname());
        }
        if (StringUtils.hasText(userDTO.getAvatar())) {
            user.setAvatar(userDTO.getAvatar());
        }
        userService.updateById(user);
        return R.success("更新成功");
    }

    @Operation(summary = "上传头像")
    @PostMapping("/avatar")
    public R<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
        String avatarUrl = userService.uploadAvatar(userId, file);
        return R.success("上传成功", avatarUrl);
    }

    @Operation(summary = "获取头像")
    @GetMapping("/avatar/**")
    public ResponseEntity<Resource> getAvatar(HttpServletRequest request) {
        try {
            // 获取请求路径
            String requestPath = request.getRequestURI();
            // context-path是/api，所以完整路径是 /api/user/avatar/...
            // 需要提取 /user/avatar/ 之后的部分
            String relativePath = "";
            int avatarIndex = requestPath.indexOf("/user/avatar/");
            if (avatarIndex >= 0) {
                relativePath = requestPath.substring(avatarIndex + "/user/avatar/".length());
            } else {
                // 如果没有找到 /user/avatar/，尝试找 /avatar/
                avatarIndex = requestPath.indexOf("/avatar/");
                if (avatarIndex >= 0) {
                    relativePath = requestPath.substring(avatarIndex + "/avatar/".length());
                } else {
                    throw new RuntimeException("无效的头像路径: " + requestPath);
                }
            }

            // 构建完整文件路径
            String filePath = localPath + "/" + relativePath;
            Path file = Paths.get(filePath);
            Resource resource = new FileSystemResource(file);

            if (!resource.exists()) {
                throw new RuntimeException("头像文件不存在: " + filePath);
            }

            String contentType = Files.probeContentType(file);
            if (contentType == null) {
                contentType = "image/jpeg";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (Exception e) {
            throw new RuntimeException("获取头像失败：" + e.getMessage());
        }
    }

    @Operation(summary = "分页查询用户")
    @GetMapping("/page")
    public R<PageUtils<UserVO>> pageUser(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String keyword) {
        PageUtils<UserVO> page = userService.pageUser(current, size, keyword);
        return R.success(page);
    }

    @Operation(summary = "根据ID获取用户")
    @GetMapping("/{id}")
    public R<UserVO> getUserById(@PathVariable Long id) {
        UserVO user = userService.getUserById(id);
        return R.success(user);
    }

    @Operation(summary = "创建用户")
    @PostMapping
    public R<?> createUser(@Valid @RequestBody UserDTO userDTO) {
        userService.createUser(userDTO);
        return R.success("创建成功");
    }

    @Operation(summary = "更新用户")
    @PutMapping
    public R<?> updateUser(@Valid @RequestBody UserDTO userDTO) {
        userService.updateUser(userDTO);
        return R.success("更新成功");
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public R<?> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return R.success("删除成功");
    }

    @Operation(summary = "启用/禁用用户")
    @PutMapping("/status/{id}")
    public R<?> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.changeStatus(id, status);
        return R.success("操作成功");
    }

    @Operation(summary = "导入用户")
    @PostMapping("/import")
    public R<?> importUsers(@RequestParam("file") MultipartFile file) {
        try {
            // 解析Excel文件
            List<UserDTO> userList = parseExcelFile(file);
            userService.importUsers(userList);
            return R.success("导入成功");
        } catch (Exception e) {
            return R.error("导入失败：" + e.getMessage());
        }
    }

    @Operation(summary = "导出用户")
    @GetMapping("/export")
    public void exportUsers(HttpServletResponse response) {
        try {
            List<UserVO> userList = userService.exportUsers();
            // 生成Excel文件并下载
            exportToExcel(userList, response);
        } catch (Exception e) {
            throw new RuntimeException("导出失败：" + e.getMessage());
        }
    }

    /**
     * 解析Excel文件
     */
    private List<UserDTO> parseExcelFile(MultipartFile file) throws IOException {
        ExcelReader reader = ExcelUtil.getReader(file.getInputStream());
        List<List<Object>> readAll = reader.read();
        List<UserDTO> userList = new ArrayList<>();

        // 跳过表头，从第二行开始读取
        for (int i = 1; i < readAll.size(); i++) {
            List<Object> row = readAll.get(i);
            if (CollUtil.isEmpty(row) || row.get(0) == null) {
                continue;
            }
            UserDTO userDTO = new UserDTO();
            userDTO.setUsername(row.get(0) != null ? row.get(0).toString() : null);
            userDTO.setNickname(row.get(1) != null ? row.get(1).toString() : null);
            userDTO.setEmail(row.size() > 2 && row.get(2) != null ? row.get(2).toString() : null);
            userDTO.setPhone(row.size() > 3 && row.get(3) != null ? row.get(3).toString() : null);
            userDTO.setPassword(row.size() > 4 && row.get(4) != null ? row.get(4).toString() : "123456");
            if (row.size() > 5 && row.get(5) != null) {
                userDTO.setStatus(Integer.parseInt(row.get(5).toString()));
            } else {
                userDTO.setStatus(1);
            }
            userList.add(userDTO);
        }
        return userList;
    }

    /**
     * 导出为Excel
     */
    private void exportToExcel(List<UserVO> userList, HttpServletResponse response) throws IOException {
        ExcelWriter writer = ExcelUtil.getWriter(true);

        // 设置表头
        writer.addHeaderAlias("id", "ID");
        writer.addHeaderAlias("username", "用户名");
        writer.addHeaderAlias("nickname", "昵称");
        writer.addHeaderAlias("email", "邮箱");
        writer.addHeaderAlias("phone", "手机号");
        writer.addHeaderAlias("status", "状态");
        writer.addHeaderAlias("roleName", "角色");
        writer.addHeaderAlias("createTime", "创建时间");

        // 写入数据
        writer.write(userList, true);

        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("用户列表", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        // 输出流
        ServletOutputStream out = response.getOutputStream();
        writer.flush(out, true);
        writer.close();
        out.close();
    }
}
