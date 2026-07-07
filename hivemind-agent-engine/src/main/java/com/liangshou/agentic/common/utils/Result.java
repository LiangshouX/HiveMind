package com.liangshou.agentic.common.utils;

import lombok.Data;
import java.io.Serializable;

/**
 * 统一响应结果封装类。
 * <p>用于包装 API 接口的返回数据，包含状态码、消息和数据体。</p>
 *
 * @param <T> 数据类型
 * @author liangshou
 */
@Data
public final class Result<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    /** HTTP 状态码 */
    private Integer code;
    /** 响应消息 */
    private String message;
    /** 响应数据体 */
    private T data;

    private Result() {}

    /**
     * 创建成功的响应（不带数据）。
     *
     * @param <T> 数据类型
     * @return 成功的响应对象
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 创建成功的响应（带数据）。
     *
     * @param data 响应数据
     * @param <T> 数据类型
     * @return 包含数据的成功响应对象
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("Success");
        result.setData(data);
        return result;
    }

    /**
     * 创建失败的响应。
     *
     * @param code 错误状态码
     * @param message 错误消息
     * @param <T> 数据类型
     * @return 包含错误信息的失败响应对象
     */
    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setData(null);
        return result;
    }
}
