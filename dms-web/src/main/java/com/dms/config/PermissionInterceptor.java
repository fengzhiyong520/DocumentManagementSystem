package com.dms.config;

import cn.dev33.satoken.stp.StpUtil;
import com.dms.core.annotation.RequiresPermission;
import com.dms.entity.Permission;
import com.dms.entity.User;
import com.dms.mapper.PermissionMapper;
import com.dms.mapper.UserMapper;
import com.dms.service.RolePermissionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * 权限拦截器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionInterceptor implements HandlerInterceptor {
    
    private final RolePermissionService rolePermissionService;
    private final PermissionMapper permissionMapper;
    private final UserMapper userMapper;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 只处理方法级别的请求
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RequiresPermission requiresPermission = handlerMethod.getMethodAnnotation(RequiresPermission.class);
        
        // 如果方法上没有权限注解，检查类上是否有
        if (requiresPermission == null) {
            requiresPermission = handlerMethod.getBeanType().getAnnotation(RequiresPermission.class);
        }
        
        // 如果没有权限注解，直接放行
        if (requiresPermission == null) {
            return true;
        }
        
        // 检查用户是否登录
        if (!StpUtil.isLogin()) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录或登录已过期\"}");
            return false;
        }
        
        // 获取当前用户
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId);
        if (user == null || user.getRoleId() == null) {
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"message\":\"用户角色不存在\"}");
            return false;
        }
        
        // 获取用户角色的权限ID列表
        List<Long> permissionIds = rolePermissionService.getPermissionIdsByRoleId(user.getRoleId());
        if (permissionIds == null || permissionIds.isEmpty()) {
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"message\":\"权限不足\"}");
            return false;
        }
        
        // 获取权限列表
        List<Permission> permissions = permissionMapper.selectBatchIds(permissionIds);
        if (permissions == null || permissions.isEmpty()) {
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"message\":\"权限不足\"}");
            return false;
        }
        
        // 构建请求的资源路径和方法
        String requestMethod = request.getMethod();
        String requestPath = request.getRequestURI();
        // 移除 context-path
        String contextPath = request.getContextPath();
        if (StringUtils.hasText(contextPath) && requestPath.startsWith(contextPath)) {
            requestPath = requestPath.substring(contextPath.length());
        }
        
        // 从注解中获取权限信息
        String permissionCode = requiresPermission.value();
        String resource = requiresPermission.resource();
        String method = requiresPermission.method();
        
        // 如果指定了权限编码，直接匹配
        if (StringUtils.hasText(permissionCode)) {
            boolean hasPermission = permissions.stream()
                    .anyMatch(p -> permissionCode.equals(p.getPermissionCode()));
            if (!hasPermission) {
                log.warn("用户 {} 尝试访问需要权限 {} 的接口 {}", userId, permissionCode, requestPath);
                response.setStatus(403);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":403,\"message\":\"权限不足：需要权限 " + permissionCode + "\"}");
                return false;
            }
            return true;
        }
        
        // 如果没有指定权限编码，使用 resource + method 匹配
        final String finalResource;
        if (!StringUtils.hasText(resource)) {
            // 从请求路径推断资源路径（移除 /api 前缀）
            String tempResource = requestPath;
            if (tempResource.startsWith("/api/")) {
                tempResource = tempResource.substring(5);
            }
            finalResource = tempResource;
        } else {
            finalResource = resource;
        }
        
        final String finalMethod = !StringUtils.hasText(method) ? requestMethod : method;
        
        // 匹配权限：resource 和 method 都要匹配
        boolean hasPermission = permissions.stream()
                .anyMatch(p -> {
                    boolean resourceMatch = !StringUtils.hasText(p.getResource()) || 
                            finalResource.equals(p.getResource()) || 
                            finalResource.matches(p.getResource().replace("*", ".*"));
                    boolean methodMatch = !StringUtils.hasText(p.getMethod()) || 
                            finalMethod.equalsIgnoreCase(p.getMethod()) || 
                            "*".equals(p.getMethod());
                    return resourceMatch && methodMatch;
                });
        
        if (!hasPermission) {
            log.warn("用户 {} 尝试访问需要权限 {} {} 的接口 {}", userId, method, resource, requestPath);
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"message\":\"权限不足\"}");
            return false;
        }
        
        return true;
    }
}

