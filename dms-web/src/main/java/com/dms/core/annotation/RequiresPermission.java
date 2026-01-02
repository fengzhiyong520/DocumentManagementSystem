package com.dms.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限注解
 * 用于标记需要权限验证的接口
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermission {
    /**
     * 权限编码（可选，如果不指定则使用 resource + method 匹配）
     */
    String value() default "";
    
    /**
     * 资源路径（可选，如果不指定则从请求路径推断）
     */
    String resource() default "";
    
    /**
     * 请求方法（可选，如果不指定则从请求方法推断）
     */
    String method() default "";
}

