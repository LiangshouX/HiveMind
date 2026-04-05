package com.liangshou.tangdynasty.agentic.common.exceptions;

/**
 * 错误码枚举类 - 统一管理 Agent Engine 模块的错误码和错误消息。
 *
 * <p>定义的错误码包括：</p>
 * <ul>
 *     <li><b>SUCCESS (200)</b> - 操作成功</li>
 *     <li><b>SYSTEM_ERROR (500)</b> - 系统内部错误</li>
 *     <li><b>PARAM_ERROR (400)</b> - 请求参数错误</li>
 *     <li><b>NOT_FOUND (404)</b> - 资源未找到</li>
 *     <li><b>UNAUTHORIZED (401)</b> - 未授权访问</li>
 *     <li><b>SOUL_LOADER_ERROR (501)</b> - SOUL 提示词加载失败</li>
 *     <li><b>LOAD_PROVIDER_ERROR (502)</b> - 模型供应商配置加载失败</li>
 *     <li><b>SESSION_HISTORY_ERROR (503)</b> - 会话历史存储错误</li>
 * </ul>
 *
 * <p>所有错误码实现了 {@link IErrorCode} 接口，可通过 {@link BizException} 抛出。</p>
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
