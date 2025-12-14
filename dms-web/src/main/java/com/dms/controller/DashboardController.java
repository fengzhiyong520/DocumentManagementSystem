package com.dms.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dms.core.domain.R;
import com.dms.entity.Borrow;
import com.dms.entity.Document;
import com.dms.service.BorrowService;
import com.dms.service.DocumentService;
import com.dms.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 首页统计仪表盘控制器
 */
@Tag(name = "首页统计")
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final UserService userService;
    private final DocumentService documentService;
    private final BorrowService borrowService;

    @Operation(summary = "获取统计数据")
    @GetMapping("/statistics")
    public R<Map<String, Object>> getStatistics() {
        Map<String, Object> data = new HashMap<>();
        // 用户总数
        data.put("userCount", userService.count());
        // 资料总数
        data.put("documentCount", documentService.count());
        // 待审批借阅数
        data.put("pendingBorrowCount", borrowService.count(
            new QueryWrapper<Borrow>()
                .eq("status", 0)
        ));
        // 今日新增资料数
        data.put("todayDocumentCount", documentService.count(
            new QueryWrapper<Document>()
                .ge("create_time", 
                    LocalDateTime.now().withHour(0).withMinute(0).withSecond(0))
        ));
        return R.success(data);
    }

    @Operation(summary = "获取按文件类型统计的资料数量")
    @GetMapping("/statistics/by-category")
    public R<Map<String, Object>> getStatisticsByCategory() {
        List<Document> documents = documentService.list();
        // 按文件类型（fileType）统计，如果没有fileType则按category统计
        Map<String, Long> typeCount = documents.stream()
            .filter(doc -> {
                // 优先使用fileType，如果没有则使用category
                return (doc.getFileType() != null && !doc.getFileType().isEmpty()) 
                    || (doc.getCategory() != null && !doc.getCategory().isEmpty());
            })
            .collect(Collectors.groupingBy(
                doc -> {
                    // 优先使用fileType，如果没有则使用category
                    String type = doc.getFileType();
                    if (type == null || type.isEmpty()) {
                        type = doc.getCategory();
                    }
                    return type != null ? type : "unknown";
                },
                Collectors.counting()
            ));
        
        // 将英文类型转换为中文显示名称
        Map<String, String> typeNameMap = new HashMap<>();
        typeNameMap.put("image", "图片");
        typeNameMap.put("text", "文本");
        typeNameMap.put("file", "文件");
        
        // 确保按固定顺序返回：图片、文本、文件
        List<String> typeNames = new ArrayList<>();
        List<Long> typeCounts = new ArrayList<>();
        
        String[] fixedTypes = {"image", "text", "file"};
        for (String type : fixedTypes) {
            typeNames.add(typeNameMap.getOrDefault(type, type));
            typeCounts.add(typeCount.getOrDefault(type, 0L));
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("categories", typeNames);
        result.put("counts", typeCounts);
        return R.success(result);
    }

    @Operation(summary = "获取最近7天的资料新增统计")
    @GetMapping("/statistics/by-date")
    public R<Map<String, Object>> getStatisticsByDate() {
        LocalDate today = LocalDate.now();
        List<String> dates = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(23, 59, 59);
            
            long count = documentService.count(
                new QueryWrapper<Document>()
                    .ge("create_time", startOfDay)
                    .le("create_time", endOfDay)
            );
            
            dates.add(date.toString());
            counts.add(count);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("dates", dates);
        result.put("counts", counts);
        return R.success(result);
    }

    @Operation(summary = "获取借阅状态统计")
    @GetMapping("/statistics/by-borrow-status")
    public R<Map<String, Object>> getStatisticsByBorrowStatus() {
        List<Borrow> borrows = borrowService.list();
        Map<Integer, Long> statusCount = borrows.stream()
            .collect(Collectors.groupingBy(
                Borrow::getStatus,
                Collectors.counting()
            ));
        
        // 状态映射：0-待审批，1-已批准，2-已拒绝，3-已归还，4-已逾期
        Map<Integer, String> statusMap = new HashMap<>();
        statusMap.put(0, "待审批");
        statusMap.put(1, "已批准");
        statusMap.put(2, "已拒绝");
        statusMap.put(3, "已归还");
        statusMap.put(4, "已逾期");
        
        List<String> statusNames = new ArrayList<>();
        List<Long> statusCounts = new ArrayList<>();
        
        for (int i = 0; i <= 4; i++) {
            statusNames.add(statusMap.get(i));
            statusCounts.add(statusCount.getOrDefault(i, 0L));
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("statusNames", statusNames);
        result.put("counts", statusCounts);
        return R.success(result);
    }
}
