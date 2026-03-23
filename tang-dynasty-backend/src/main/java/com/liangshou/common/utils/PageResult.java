package com.liangshou.common.utils;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 分页结果封装类。
 * <p>用于包装分页查询的结果数据，包含总记录数、当前页数据列表、当前页码和每页大小。</p>
 * <p>实现了 {@link Serializable} 接口，支持序列化传输。</p>
 *
 * @param <T> 数据类型
 * @author liangshou
 * @version 1.0
 */
@Data
public final class PageResult<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /** 总记录数 */
    private long total;
    /** 当前页的数据列表 */
    private List<T> records;
    /** 当前页码（从 1 开始） */
    private long current;
    /** 每页显示的大小 */
    private long size;

    /**
     * 私有构造函数，防止外部直接实例化。
     * <p>请使用静态方法 {@link #of(long, List, long, long)} 创建实例。</p>
     */
    private PageResult() {}

    /**
     * 创建分页结果对象。
     * <p>根据提供的参数构建并返回一个完整的分页结果对象。</p>
     *
     * @param total 总记录数
     * @param records 当前页的数据列表
     * @param current 当前页码（从 1 开始）
     * @param size 每页显示的大小
     * @param <T> 数据类型
     * @return 包含完整信息的分页结果对象
     */
    public static <T> PageResult<T> of(long total, List<T> records, long current, long size) {
        PageResult<T> pageResult = new PageResult<>();
        pageResult.setTotal(total);
        pageResult.setRecords(records);
        pageResult.setCurrent(current);
        pageResult.setSize(size);
        return pageResult;
    }
}
