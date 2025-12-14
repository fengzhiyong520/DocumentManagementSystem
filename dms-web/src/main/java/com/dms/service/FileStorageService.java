package com.dms.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储服务接口
 */
public interface FileStorageService {
    /**
     * 上传文件
     * @param file 文件
     * @param username 用户名，用于路径隔离
     * @return 相对路径
     */
    String uploadFile(MultipartFile file, String username);

    /**
     * 删除文件
     */
    void deleteFile(String fileUrl);
}

