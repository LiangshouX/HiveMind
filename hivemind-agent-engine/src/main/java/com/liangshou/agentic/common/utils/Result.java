package com.liangshou.agentic.common.utils;

import com.liangshou.agentic.common.exceptions.IErrorCode;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一响应结果封装类。
 * <p>用于包装 API 接口的返回数据，包含业务错误码、消息和数据体。</p>
 * <p>错误码格式为 {@code HME_{业务域}_{编号}}，例如 {@code HME_AUTH_001}。</p>
 *
 * @param <T> 数据类型
 * @author liangshou
 * @version 2.0
 */
@Data
public final class Result<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 业务错误码。成功时为 {@code "SUCCESS"}，错误时为 {@code HME_{域}_{编号}}。
     */
    private String code;
    /**
     * 响应消息。
     */
    private String message;
    /**
     * 响应数据体。
     */
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
        result.setCode("SUCCESS");
        result.setMessage("Success");
        result.setData(data);
        return result;
    }

    /**
     * 创建失败的响应。
     *
     * @param code    业务错误码（如 HME_AUTH_001）
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 包含错误信息的失败响应对象
     */
    public static <T> Result<T> error(String code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setData(null);
        return result;
    }

    /**
     * 创建失败的响应，使用错误码枚举的默认消息。
     *
     * @param errorCode 错误码枚举对象
     * @param <T>       数据类型
     * @return 包含错误信息的失败响应对象
     */
    public static <T> Result<T> error(IErrorCode errorCode) {
        return error(errorCode.getCode(), errorCode.getMessage());
    }

    /**
     * 创建失败的响应，使用错误码枚举但自定义消息。
     *
     * @param errorCode 错误码枚举对象
     * @param message   自定义错误消息
     * @param <T>       数据类型
     * @return 包含错误信息的失败响应对象
     */
    public static <T> Result<T> error(IErrorCode errorCode, String message) {
        return error(errorCode.getCode(), message);
    }
}
