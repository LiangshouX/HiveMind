package com.liangshou.common;

import com.liangshou.agentic.common.exceptions.IErrorCode;
import lombok.Getter;

/**
 * HiveMind 统一错误码枚举 — Backend 模块（认证、用户、任务域）。
 *
 * <p>错误码格式：{@code HME_{业务域}_{编号}}</p>
 * <ul>
 *     <li><b>AUTH</b> — 认证与授权</li>
 *     <li><b>USER</b> — 用户管理</li>
 *     <li><b>TASK</b> — 任务与模板</li>
 * </ul>
 *
 * @author liangshou
 * @version 2.0
 * @see com.liangshou.agentic.common.exceptions.IErrorCode
 * @see com.liangshou.agentic.common.exceptions.BizException
 */
@Getter
public enum HmeBackendErrorCode implements IErrorCode {

    // ==================== AUTH — 认证与授权 ====================

    /**
     * 未登录或登录已过期
     */
    AUTH_NOT_LOGGED_IN(401, "HME_AUTH_001", "未登录或登录已过期，请重新登录"),
    /**
     * JWT Token 无效或已过期
     */
    AUTH_TOKEN_INVALID(401, "HME_AUTH_002", "JWT Token 无效或已过期"),
    /**
     * JWT Token 解析失败
     */
    AUTH_TOKEN_PARSE_ERROR(401, "HME_AUTH_003", "JWT Token 解析失败"),
    /**
     * 用户账号已被禁用
     */
    AUTH_ACCOUNT_DISABLED(403, "HME_AUTH_004", "用户账号已被禁用"),
    /**
     * 无权访问当前资源
     */
    AUTH_ACCESS_DENIED(403, "HME_AUTH_005", "无权访问当前资源，请检查权限配置"),
    /**
     * 用户名或密码错误
     */
    AUTH_BAD_CREDENTIALS(401, "HME_AUTH_006", "用户名或密码错误"),
    /**
     * 用户不存在
     */
    AUTH_USER_NOT_FOUND(401, "HME_AUTH_007", "用户不存在"),

    // ==================== USER — 用户管理 ====================

    /**
     * 用户 ID 已存在
     */
    USER_ID_ALREADY_EXISTS(400, "HME_USER_001", "用户 ID 已存在"),
    /**
     * 用户不存在
     */
    USER_NOT_FOUND(404, "HME_USER_002", "用户不存在"),
    /**
     * 用户 ID 不能为空
     */
    USER_ID_EMPTY(400, "HME_USER_003", "用户 ID 不能为空"),
    /**
     * 用户信息更新失败
     */
    USER_UPDATE_FAILED(500, "HME_USER_004", "用户信息更新失败"),

    // ==================== TASK — 任务与模板 ====================

    /**
     * 任务模板不存在
     */
    TASK_TEMPLATE_NOT_FOUND(404, "HME_TASK_001", "任务模板不存在"),
    /**
     * 无权访问该模板
     */
    TASK_TEMPLATE_ACCESS_DENIED(403, "HME_TASK_002", "无权访问该模板"),
    /**
     * 系统内置模板不可编辑
     */
    TASK_TEMPLATE_BUILTIN_NOT_EDITABLE(403, "HME_TASK_003", "系统内置模板不可编辑"),
    /**
     * 系统内置模板不可删除
     */
    TASK_TEMPLATE_BUILTIN_NOT_DELETABLE(403, "HME_TASK_004", "系统内置模板不可删除"),
    /**
     * 无权编辑该模板
     */
    TASK_TEMPLATE_EDIT_DENIED(403, "HME_TASK_005", "无权编辑该模板"),
    /**
     * 无权删除该模板
     */
    TASK_TEMPLATE_DELETE_DENIED(403, "HME_TASK_006", "无权删除该模板"),
    /**
     * 参数替换后命令为空
     */
    TASK_TEMPLATE_PARAM_EMPTY(400, "HME_TASK_007", "参数替换后命令为空"),
    /**
     * 缺少必填参数
     */
    TASK_TEMPLATE_PARAM_MISSING(400, "HME_TASK_008", "缺少必填参数"),
    /**
     * 任务执行失败
     */
    TASK_EXECUTION_FAILED(500, "HME_TASK_009", "任务执行失败"),
    /**
     * 记录不存在或无权限修改
     */
    TASK_RECORD_NOT_FOUND_UPDATE(404, "HME_TASK_010", "记录不存在或无权限修改"),
    /**
     * 记录不存在或无权限删除
     */
    TASK_RECORD_NOT_FOUND_DELETE(404, "HME_TASK_011", "记录不存在或无权限删除"),
    /**
     * Provider 不存在或无权限访问
     */
    TASK_PROVIDER_NOT_FOUND(404, "HME_TASK_012", "Provider 不存在或无权限访问"),
    /**
     * 至少需要保留一个激活的 Provider
     */
    TASK_PROVIDER_LAST_ACTIVE(403, "HME_TASK_013", "至少需要保留一个激活的 Provider，无法停用最后一个"),
    /**
     * 内置 Provider 初始化失败
     */
    TASK_PROVIDER_BUILTIN_INIT_ERROR(500, "HME_TASK_014", "内置 Provider 初始化失败"),
    /**
     * 系统内置 Provider 不可删除
     */
    TASK_PROVIDER_BUILTIN_NOT_DELETABLE(403, "HME_TASK_015", "系统内置 Provider 不可删除"),
    /**
     * API Key 加密失败
     */
    TASK_API_KEY_ENCRYPT_ERROR(500, "HME_TASK_016", "API Key 加密失败"),
    /**
     * API Key 解密失败
     */
    TASK_API_KEY_DECRYPT_ERROR(500, "HME_TASK_017", "API Key 解密失败"),
    /**
     * 缺少 baseUrl
     */
    TASK_PROVIDER_BASE_URL_MISSING(400, "HME_TASK_018", "缺少 baseUrl");

    /**
     * HTTP 状态码。
     */
    private final int httpStatus;

    /**
     * 业务错误码（如 HME_AUTH_001）。
     */
    private final String code;

    /**
     * 错误消息。
     */
    private final String message;

    HmeBackendErrorCode(int httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
