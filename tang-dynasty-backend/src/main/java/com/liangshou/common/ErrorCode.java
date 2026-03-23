package com.liangshou.common;

import lombok.Getter;

/**
 * 系统错误码枚举类。
 * <p>用于统一管理系统中的各种错误码和对应的错误消息。</p>
 *
 * @author liangshou
 * @version 1.0
 */
@Getter
public enum ErrorCode {
    /** 成功状态码 */
    SUCCESS(200, "Success"),
    /** 系统错误 */
    SYSTEM_ERROR(500, "System Error"),
    /** 参数错误 */
    PARAM_ERROR(400, "Parameter Error"),
    /** 资源未找到 */
    NOT_FOUND(404, "Not Found"),
    /** 未授权 */
    UNAUTHORIZED(401, "Unauthorized");

    /** 错误码 */
    private final int code;
    /** 错误消息 */
    private final String message;

    /**
     * 构造错误码枚举。
     *
     * @param code 错误码
     * @param message 错误消息
     */
    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
