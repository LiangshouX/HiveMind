package com.liangshou.tangdynasty.agentic.common.exceptions;

/**
 * 业务异常类。
 * <p>用于封装业务逻辑中的错误信息。</p>
 *
 * @author liangshou
 * @version 1.0
 */
public class BizException extends RuntimeException {

    /** 错误码 */
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

    /**
     * 获取错误码。
     *
     * @return 错误码
     */
    public int getCode() {
        return code;
    }
}
