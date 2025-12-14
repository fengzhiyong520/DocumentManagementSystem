package com.dms.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.dms.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 本地文件存储服务实现
 */
@Slf4j
@Service
public class LocalFileStorageServiceImpl implements FileStorageService {

    @Value("${file.upload.local-path:D:/dms/upload}")
    private String localPath;

    @Override
    public String uploadFile(MultipartFile file, String username) {
        try {
            if (username == null || username.trim().isEmpty()) {
                throw new RuntimeException("用户名不能为空");
            }
            // 生成文件路径：{用户名}/uuid.扩展名（不带日期）
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".") ?
                    originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
            String fileName = IdUtil.simpleUUID() + extension;
            String relativePath = username + "/" + fileName;
            String fullPath = localPath + "/" + relativePath;
            
            // 创建目录
            FileUtil.mkdir(new File(fullPath).getParent());
            
            // 保存文件
            file.transferTo(new File(fullPath));
            
            log.info("文件上传成功：{}", fullPath);
            return relativePath;
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败：" + e.getMessage());
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        try {
            String fullPath = localPath + "/" + fileUrl;
            File file = new File(fullPath);
            if (file.exists()) {
                FileUtil.del(file);
                log.info("文件删除成功：{}", fullPath);
            }
        } catch (Exception e) {
            log.error("文件删除失败：{}", fileUrl, e);
        }
    }
}
