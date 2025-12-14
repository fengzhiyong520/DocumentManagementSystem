package com.dms;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 文件资料管理系统启动�?
 */
@SpringBootApplication(scanBasePackages = "com.dms")
@MapperScan(basePackages = "com.dms.mapper")
public class DmsApplication {
    public static void main(String[] args) {
        SpringApplication.run(DmsApplication.class, args);
    }
}

