package com.dms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dms.entity.Borrow;
import org.apache.ibatis.annotations.Mapper;

/**
 * 借阅Mapper
 */
@Mapper
public interface BorrowMapper extends BaseMapper<Borrow> {
}

