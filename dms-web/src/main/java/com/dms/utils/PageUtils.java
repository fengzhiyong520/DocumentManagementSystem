package com.dms.utils;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import java.util.List;

/**
 * 分页工具�?
 */
@Data
public class PageUtils<T> {
    private Long current;
    private Long size;
    private Long total;
    private List<T> records;

    public PageUtils() {
    }

    public PageUtils(IPage<T> page) {
        this.current = page.getCurrent();
        this.size = page.getSize();
        this.total = page.getTotal();
        this.records = page.getRecords();
    }

    public static <T> Page<T> getPage(Long current, Long size) {
        if (current == null || current < 1) {
            current = 1L;
        }
        if (size == null || size < 1) {
            size = 10L;
        }
        return new Page<>(current, size);
    }
}

