package com.liangshou.common.utils;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public final class PageResult<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private long total;
    private List<T> records;
    private long current;
    private long size;

    private PageResult() {}

    public static <T> PageResult<T> of(long total, List<T> records, long current, long size) {
        PageResult<T> pageResult = new PageResult<>();
        pageResult.setTotal(total);
        pageResult.setRecords(records);
        pageResult.setCurrent(current);
        pageResult.setSize(size);
        return pageResult;
    }
}
