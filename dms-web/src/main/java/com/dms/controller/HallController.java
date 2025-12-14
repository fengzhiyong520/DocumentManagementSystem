package com.dms.controller;

import com.dms.core.domain.R;
import com.dms.utils.PageUtils;
import com.dms.service.DocumentService;
import com.dms.vo.DocumentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 大厅管理控制�?
 */
@Tag(name = "大厅管理")
@RestController
@RequestMapping("/hall")
@RequiredArgsConstructor
public class HallController {

    private final DocumentService documentService;

    @Operation(summary = "获取公开资料列表")
    @GetMapping("/documents")
    public R<PageUtils<DocumentVO>> listPublicDocuments(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) String uploadUserName) {
        PageUtils<DocumentVO> page = documentService.listPublicDocuments(current, size, keyword, category, fileName, uploadUserName);
        return R.success(page);
    }
}

