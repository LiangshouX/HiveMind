package com.liangshou.tangdynasty.agentic.common.exceptions;

/**
 * 错误码枚举类。
 * <p>用于统一管理 Agent Engine 模块中的各种错误码和对应的错误消息。</p>
 *
 * @author liangshou
 * @version 1.0
 */
public enum ErrorCodeEnum implements IErrorCode {
    /**
     * 成功状态码
     */
    SUCCESS(200, "Success"),

    /**
     * 系统错误
     */
    SYSTEM_ERROR(500, "System Error"),

    /**
     * 参数错误
     */
    PARAM_ERROR(400, "Parameter Error"),

    /**
     * 资源未找到
     */
    NOT_FOUND(404, "Not Found"),

    /**
     * 未授权
     */
    UNAUTHORIZED(401, "Unauthorized"),

    /**
     * SOUL 加载错误
     */
    SOUL_LOADER_ERROR(501, "Soul Loader Error"),

    /**
     * 加载供应商错误
     */
    LOAD_PROVIDER_ERROR(502, "Load Provider Error"),

    /**
     * Session 历史存储错误
     */
    SESSION_HISTORY_ERROR(503, "Session History Error");

    /**
     * 错误码
     */
    private final int code;

    /**
     * 错误消息
     */
    private final String message;

    /**
     * 构造错误码枚举。
     *
     * @param code    错误码
     * @param message 错误消息
     */
    ErrorCodeEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
