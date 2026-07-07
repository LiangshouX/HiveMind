package com.liangshou.agentic.common.exceptions;

import lombok.Getter;

/**
 * 业务异常类 - 封装 Agent 引擎模块的业务逻辑错误。
 *
 * <p>该异常用于统一处理业务层面的错误，包含：</p>
 * <ul>
 *     <li>错误码（code）- 用于前端识别错误类型</li>
 *     <li>错误消息（message）- 用于向用户展示友好的错误提示</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * throw new BizException(ErrorCodeEnum.SESSION_HISTORY_ERROR);
 * throw new BizException(ErrorCodeEnum.SOUL_LOADER_ERROR, cause);
 * }</pre>
 *
 * @author liangshou
 * @version 1.0
 */
@Getter
public class BizException extends RuntimeException {

    /** 错误码
     * -- GETTER --
     *  获取错误码。
     *
     */
    private final int code;

    /**
     * 构造业务异常。
     *
     * @param errorCodeEnum 错误码枚举对象
     */
    public BizException(IErrorCode errorCodeEnum) {
        super(errorCodeEnum.getMessage());
        this.code = errorCodeEnum.getCode();
    }

    /**
     * 构造业务异常。
     *
     * @param errorCodeEnum 错误码枚举对象
     * @param cause 异常原因
     */
    public BizException(IErrorCode errorCodeEnum, Throwable cause) {
        super(errorCodeEnum.getMessage(), cause);
        this.code = errorCodeEnum.getCode();
    }

}
