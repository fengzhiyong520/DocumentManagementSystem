package com.dms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dms.utils.PageUtils;
import com.dms.dto.BorrowDTO;
import com.dms.entity.Borrow;
import com.dms.vo.BorrowVO;

/**
 * 借阅服务接口
 */
public interface BorrowService extends IService<Borrow> {
    /**
     * 申请借阅
     */
    void applyBorrow(BorrowDTO borrowDTO);

    /**
     * 审批借阅
     */
    void approveBorrow(Long id, Integer status, String remark);

    /**
     * 归还资料
     */
    void returnDocument(Long id);

    /**
     * 分页查询借阅记录
     */
    PageUtils<BorrowVO> pageBorrow(Long current, Long size, Integer status);

    /**
     * 检查用户是否已申请过某个资�?
     */
    boolean hasApplied(Long documentId, Long userId);

    /**
     * 获取用户对某个资料的借阅状�?
     * @return 借阅记录的状态，如果未申请返回null
     */
    Integer getBorrowStatus(Long documentId, Long userId);

    /**
     * 分页查询当前用户审批过的记录
     */
    PageUtils<BorrowVO> pageApprovedRecords(Long current, Long size, Integer status);
}

