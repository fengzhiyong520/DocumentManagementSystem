package com.dms.controller;

import com.dms.core.domain.R;
import com.dms.dto.BorrowDTO;
import com.dms.service.BorrowService;
import com.dms.vo.BorrowVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.dms.utils.PageUtils;

/**
 * 借阅管理控制器
 */
@Tag(name = "借阅管理")
@RestController
@RequestMapping("/borrow")
@RequiredArgsConstructor
public class BorrowController {

    private final BorrowService borrowService;

    @Operation(summary = "申请借阅")
    @PostMapping("/apply")
    public R<?> applyBorrow(@Valid @RequestBody BorrowDTO borrowDTO) {
        borrowService.applyBorrow(borrowDTO);
        return R.success("申请成功");
    }

    @Operation(summary = "审批借阅")
    @PutMapping("/approve/{id}")
    public R<?> approveBorrow(@PathVariable Long id, @RequestParam Integer status, @RequestParam(required = false) String remark) {
        borrowService.approveBorrow(id, status, remark);
        return R.success("审批成功");
    }

    @Operation(summary = "归还资料")
    @PutMapping("/return/{id}")
    public R<?> returnDocument(@PathVariable Long id) {
        borrowService.returnDocument(id);
        return R.success("归还成功");
    }

    @Operation(summary = "分页查询借阅记录")
    @GetMapping("/page")
    public R<PageUtils<BorrowVO>> pageBorrow(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Integer status) {
        PageUtils<BorrowVO> page = borrowService.pageBorrow(current, size, status);
        return R.success(page);
    }

    @Operation(summary = "检查用户是否已申请过某个资料")
    @GetMapping("/check/{documentId}")
    public R<Integer> checkApplied(@PathVariable Long documentId) {
        Long userId = cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
        Integer status = borrowService.getBorrowStatus(documentId, userId);
        // 如果未申请返回-1，已申请返回状态值（0-待审批，1-已批准，2-已拒绝，3-已归还，4-已逾期）
        return R.success(status != null ? status : -1);
    }

    @Operation(summary = "分页查询当前用户审批过的记录")
    @GetMapping("/approved-records")
    public R<PageUtils<BorrowVO>> pageApprovedRecords(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Integer status) {
        PageUtils<BorrowVO> page = borrowService.pageApprovedRecords(current, size, status);
        return R.success(page);
    }
}
