package com.liangshou.common.utils;

import lombok.Data;
import java.io.Serializable;

/**
 * 统一响应结果封装类。
 * <p>用于包装 API 接口的返回数据，包含状态码、消息和数据体。</p>
 * <p>实现了 {@link Serializable} 接口，支持序列化传输。</p>
 * <p>提供便捷的静态方法创建成功或失败的响应：</p>
 * <ul>
 *     <li>{@link #success()} - 创建不带数据的成功响应</li>
 *     <li>{@link #success(Object)} - 创建带数据的成功响应</li>
 *     <li>{@link #error(Integer, String)} - 创建指定错误码和消息的失败响应</li>
 * </ul>
 *
 * @param <T> 数据类型
 * @author liangshou
 * @version 1.0
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

    /**
     * 私有构造函数，防止外部直接实例化。
     * <p>请使用静态方法 {@link #success()}、{@link #success(Object)} 或 {@link #error(Integer, String)} 创建实例。</p>
     */
    private Result() {}

    /**
     * 创建成功的响应（不带数据）。
     * <p>默认状态码为 200，消息为 "Success"。</p>
     *
     * @param <T> 数据类型
     * @return 成功的响应对象
     * @see #success(Object)
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 创建成功的响应（带数据）。
     * <p>设置状态码为 200，消息为 "Success"，并包含提供的数据。</p>
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
     * <p>根据提供的错误码和消息创建错误响应，数据体为 null。</p>
     *
     * @param code 错误状态码
     * @param message 错误消息
     * @param <T> 数据类型
     * @return 包含错误信息的响应对象
     */
    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setData(null);
        return result;
    }
}
