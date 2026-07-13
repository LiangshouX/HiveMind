package com.liangshou.agentic.common.exceptions;

/**
 * 错误码接口。
 * <p>定义错误码、HTTP 状态码和错误消息的获取方法。</p>
 * <p>错误码格式为 {@code HME_{业务域}_{编号}}，例如 {@code HME_AUTH_001}。</p>
 *
 * @author liangshou
 * @version 2.0
 */
public interface IErrorCode {
    /**
     * 获取业务错误码。
     * <p>格式示例：{@code HME_AUTH_001}、{@code HME_SYSTEM_001}</p>
     *
     * @return 业务错误码字符串
     */
    String getCode();

    /**
     * 获取对应的 HTTP 状态码。
     *
     * @return HTTP 状态码
     */
    int getHttpStatus();

    /**
     * 获取错误消息。
     *
     * @return 错误消息
     */
    String getMessage();
}
