package com.dms.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.dms.core.domain.R;
import com.dms.utils.PageUtils;
import com.dms.dto.DocumentDTO;
import com.dms.service.DocumentService;
import com.dms.vo.DocumentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 资料管理控制�?
 */
@Tag(name = "资料管理")
@RestController
@RequestMapping("/document")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @Value("${file.upload.local-path:D:/dms/upload}")
    private String localPath;

    @Operation(summary = "分页查询资料")
    @GetMapping("/page")
    public R<PageUtils<DocumentVO>> pageDocument(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer isPublic) {
        PageUtils<DocumentVO> page = documentService.pageDocument(current, size, keyword, category, isPublic);
        return R.success(page);
    }

    @Operation(summary = "根据ID获取资料")
    @GetMapping("/{id}")
    public R<DocumentVO> getDocumentById(@PathVariable Long id) {
        DocumentVO document = documentService.getDocumentById(id);
        return R.success(document);
    }

    @Operation(summary = "上传资料")
    @PostMapping("/upload")
    public R<?> uploadDocument(@Valid @ModelAttribute DocumentDTO documentDTO,
                               @RequestParam(value = "file", required = false) MultipartFile file) {
        documentService.uploadDocument(documentDTO, file);
        return R.success("保存成功");
    }

    @Operation(summary = "更新资料")
    @PutMapping
    public R<?> updateDocument(@Valid @RequestBody DocumentDTO documentDTO) {
        documentService.updateDocument(documentDTO);
        return R.success("更新成功");
    }

    @Operation(summary = "删除资料")
    @DeleteMapping("/{id}")
    public R<?> deleteDocument(@PathVariable Long id) {
        documentService.deleteDocument(id);
        return R.success("删除成功");
    }

    @Operation(summary = "下载文件")
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        DocumentVO document = documentService.getDocumentById(id);
        if (document == null || document.getFileUrl() == null) {
            throw new RuntimeException("文件不存在");
        }

        try {
            String filePath = localPath + "/" + document.getFileUrl();
            Path path = Paths.get(filePath);
            Resource resource = new FileSystemResource(path);

            if (!resource.exists()) {
                throw new RuntimeException("文件不存在");
            }

            String fileName = document.getFileName() != null ? document.getFileName() : "file";
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                    .body(resource);
        } catch (Exception e) {
            throw new RuntimeException("文件下载失败：" + e.getMessage());
        }
    }

    @Operation(summary = "预览文件")
    @GetMapping("/preview/{id}")
    public ResponseEntity<Resource> previewFile(@PathVariable Long id) {
        DocumentVO document = documentService.getDocumentById(id);
        if (document == null || document.getFileUrl() == null) {
            throw new RuntimeException("文件不存在");
        }

        try {
            String filePath = localPath + "/" + document.getFileUrl();
            Path path = Paths.get(filePath);
            Resource resource = new FileSystemResource(path);

            if (!resource.exists()) {
                throw new RuntimeException("文件不存在");
            }

            // 根据文件类型设置Content-Type
            String contentType = Files.probeContentType(path);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                    .body(resource);
        } catch (Exception e) {
            throw new RuntimeException("文件预览失败：" + e.getMessage());
        }
    }

    @Operation(summary = "切换资料状态")
    @PutMapping("/status/{id}")
    public R<?> changeDocumentStatus(@PathVariable Long id, @RequestParam Integer status) {
        documentService.changeDocumentStatus(id, status);
        return R.success("操作成功");
    }

    @Operation(summary = "导入资料")
    @PostMapping("/import")
    public R<?> importDocuments(@RequestParam("file") MultipartFile file) {
        try {
            // 解析Excel文件
            List<DocumentDTO> documentList = parseExcelFile(file);
            documentService.importDocuments(documentList);
            return R.success("导入成功");
        } catch (Exception e) {
            return R.error("导入失败：" + e.getMessage());
        }
    }

    @Operation(summary = "导出资料")
    @GetMapping("/export")
    public void exportDocuments(HttpServletResponse response) {
        try {
            List<DocumentVO> documentList = documentService.exportDocuments();
            // 生成Excel文件并下载
            exportToExcel(documentList, response);
        } catch (Exception e) {
            throw new RuntimeException("导出失败：" + e.getMessage());
        }
    }

    /**
     * 解析Excel文件
     */
    private List<DocumentDTO> parseExcelFile(MultipartFile file) throws IOException {
        ExcelReader reader = ExcelUtil.getReader(file.getInputStream());
        List<List<Object>> readAll = reader.read();
        List<DocumentDTO> documentList = new ArrayList<>();

        // 跳过表头，从第二行开始读取
        for (int i = 1; i < readAll.size(); i++) {
            List<Object> row = readAll.get(i);
            if (CollUtil.isEmpty(row) || row.get(0) == null) {
                continue;
            }
            DocumentDTO documentDTO = new DocumentDTO();
            documentDTO.setTitle(row.get(0) != null ? row.get(0).toString() : null);
            documentDTO.setDescription(row.size() > 1 && row.get(1) != null ? row.get(1).toString() : null);
            documentDTO.setCategory(row.size() > 2 && row.get(2) != null ? row.get(2).toString() : null);
            if (row.size() > 3 && row.get(3) != null) {
                documentDTO.setIsPublic(Integer.parseInt(row.get(3).toString()));
            } else {
                documentDTO.setIsPublic(0);
            }
            if (row.size() > 4 && row.get(4) != null) {
                documentDTO.setStatus(Integer.parseInt(row.get(4).toString()));
            } else {
                documentDTO.setStatus(1);
            }
            documentList.add(documentDTO);
        }
        return documentList;
    }

    /**
     * 导出为Excel
     */
    private void exportToExcel(List<DocumentVO> documentList, HttpServletResponse response) throws IOException {
        ExcelWriter writer = ExcelUtil.getWriter(true);

        // 设置表头
        writer.addHeaderAlias("id", "ID");
        writer.addHeaderAlias("title", "标题");
        writer.addHeaderAlias("description", "描述");
        writer.addHeaderAlias("category", "分类");
        writer.addHeaderAlias("fileName", "文件名");
        writer.addHeaderAlias("fileType", "文件类型");
        writer.addHeaderAlias("fileSize", "文件大小");
        writer.addHeaderAlias("isPublic", "是否公开");
        writer.addHeaderAlias("status", "状态");
        writer.addHeaderAlias("uploadUserName", "上传人");
        writer.addHeaderAlias("createTime", "创建时间");

        // 写入数据
        writer.write(documentList, true);

        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("资料列表", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        // 输出流
        ServletOutputStream out = response.getOutputStream();
        writer.flush(out, true);
        writer.close();
        out.close();
    }
}

