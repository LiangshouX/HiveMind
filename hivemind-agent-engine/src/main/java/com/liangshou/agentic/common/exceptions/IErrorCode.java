package com.liangshou.agentic.common.exceptions;

/**
 * 错误码接口。
 * <p>定义错误码和错误消息的获取方法。</p>
 *
 * @author liangshou
 * @version 1.0
 */
public interface IErrorCode {
    /**
     * 获取错误码。
     *
     * @return 错误码
     */
    int getCode();

    /**
     * 获取错误消息。
     *
     * @return 错误消息
     */
    String getMessage();
}
