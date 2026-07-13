package com.liangshou.agentic.common.exceptions;

import lombok.Getter;

/**
 * 业务异常类 - 封装 HiveMind 系统的业务逻辑错误。
 *
 * <p>该异常用于统一处理业务层面的错误，包含：</p>
 * <ul>
 *     <li>错误码（code）- 格式为 {@code HME_{业务域}_{编号}}，例如 {@code HME_AUTH_001}</li>
 *     <li>HTTP 状态码（httpStatus）- 对应的 HTTP 响应状态码</li>
 *     <li>错误消息（message）- 用于向用户展示友好的错误提示</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * throw new BizException(HmeErrorCode.AGENT_001);
 * throw new BizException(HmeErrorCode.AGENT_001, cause);
 * throw new BizException(HmeErrorCode.AGENT_001, "自定义消息");
 * }</pre>
 *
 * @author liangshou
 * @version 2.0
 */
@Getter
public class BizException extends RuntimeException {

    /**
     * 业务错误码（如 HME_AUTH_001）。
     */
    private final String code;

    /**
     * HTTP 状态码。
     */
    private final int httpStatus;

    /**
     * 构造业务异常，使用错误码枚举的默认消息。
     *
     * @param errorCode 错误码枚举对象
     */
    public BizException(IErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.httpStatus = errorCode.getHttpStatus();
    }

    /**
     * 构造业务异常，附带原因链。
     *
     * @param errorCode 错误码枚举对象
     * @param cause     异常原因
     */
    public BizException(IErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.code = errorCode.getCode();
        this.httpStatus = errorCode.getHttpStatus();
    }

    /**
     * 构造业务异常，使用自定义消息覆盖默认消息。
     *
     * @param errorCode 错误码枚举对象
     * @param message   自定义错误消息
     */
    public BizException(IErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
        this.httpStatus = errorCode.getHttpStatus();
    }
}
