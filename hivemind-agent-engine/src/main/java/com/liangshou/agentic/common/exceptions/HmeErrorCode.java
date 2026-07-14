package com.liangshou.agentic.common.exceptions;

import lombok.Getter;

/**
 * HiveMind 统一错误码枚举 — Agent Engine 模块。
 *
 * <p>错误码格式：{@code HME_{业务域}_{编号}}</p>
 * <ul>
 *     <li><b>HME</b> — HiveMind Error 统一前缀</li>
 *     <li><b>业务域</b> — 2~4 个大写字母，表示错误所属的业务领域</li>
 *     <li><b>编号</b> — 3 位数字，001 起</li>
 * </ul>
 *
 * <p>业务域划分：</p>
 * <ul>
 *     <li><b>SYSTEM</b> — 通用系统错误</li>
 *     <li><b>AGENT</b> — Agent 引擎核心</li>
 *     <li><b>SKILL</b> — 技能管理</li>
 *     <li><b>PROVIDER</b> — 模型供应商</li>
 *     <li><b>MCP</b> — MCP 配置与工具</li>
 *     <li><b>MODEL</b> — 模型管理</li>
 *     <li><b>PROFILE</b> — Agent Profile</li>
 *     <li><b>MEMORY</b> — 记忆管理</li>
 *     <li><b>STORAGE</b> — 文件存储</li>
 * </ul>
 *
 * @author liangshou
 * @version 2.0
 * @see IErrorCode
 * @see BizException
 */
@Getter
public enum HmeErrorCode implements IErrorCode {

    // ==================== SYSTEM — 通用系统错误 ====================

    /**
     * 操作成功
     */
    SUCCESS(200, "HME_SYSTEM_000", "操作成功"),
    /**
     * 系统内部错误
     */
    SYSTEM_ERROR(500, "HME_SYSTEM_001", "系统内部错误"),
    /**
     * 请求参数错误
     */
    PARAM_ERROR(400, "HME_SYSTEM_002", "请求参数错误"),
    /**
     * 资源未找到
     */
    NOT_FOUND(404, "HME_SYSTEM_003", "资源未找到"),
    /**
     * 请求方法不支持
     */
    METHOD_NOT_ALLOWED(405, "HME_SYSTEM_004", "请求方法不支持"),
    /**
     * 请求频率过高
     */
    TOO_MANY_REQUESTS(429, "HME_SYSTEM_005", "请求频率过高"),

    // ==================== AGENT — Agent 引擎核心 ====================

    /**
     * SOUL 提示词加载失败
     */
    AGENT_SOUL_LOAD_ERROR(500, "HME_AGENT_001", "SOUL 提示词加载失败"),
    /**
     * 模型供应商配置加载失败
     */
    AGENT_PROVIDER_LOAD_ERROR(500, "HME_AGENT_002", "模型供应商配置加载失败"),
    /**
     * 会话历史存储错误
     */
    AGENT_SESSION_HISTORY_ERROR(500, "HME_AGENT_003", "会话历史存储错误"),
    /**
     * 标题生成失败
     */
    AGENT_TITLE_GENERATE_ERROR(500, "HME_AGENT_004", "标题生成失败"),
    /**
     * SSE 事件发送失败
     */
    AGENT_SSE_EMIT_ERROR(500, "HME_AGENT_005", "SSE 事件发送失败"),
    /**
     * 未获取到当前登录用户
     */
    AGENT_USER_NOT_FOUND(401, "HME_AGENT_006", "未获取到当前登录用户"),
    /**
     * Agent 会话状态序列化失败
     */
    AGENT_SESSION_SERIALIZE_ERROR(500, "HME_AGENT_007", "Agent 会话状态序列化失败"),
    /**
     * Agent 会话状态反序列化失败
     */
    AGENT_SESSION_DESERIALIZE_ERROR(500, "HME_AGENT_008", "Agent 会话状态反序列化失败"),
    /**
     * Sandbox 工具初始化失败
     */
    AGENT_SANDBOX_INIT_ERROR(500, "HME_AGENT_009", "Sandbox 工具初始化失败"),
    /**
     * 未知系统工具
     */
    AGENT_UNKNOWN_TOOL(400, "HME_AGENT_010", "未知系统工具"),

    // ==================== SKILL — 技能管理 ====================

    /**
     * Skill 不存在
     */
    SKILL_NOT_FOUND(404, "HME_SKILL_001", "Skill 不存在"),
    /**
     * 同名 Skill 已存在
     */
    SKILL_ALREADY_EXISTS(409, "HME_SKILL_002", "同名 Skill 已存在"),
    /**
     * Skill 版本已存在
     */
    SKILL_VERSION_ALREADY_EXISTS(409, "HME_SKILL_003", "Skill 版本已存在"),
    /**
     * Skill 不存在或无权限
     */
    SKILL_NOT_FOUND_OR_NO_PERMISSION(404, "HME_SKILL_004", "Skill 不存在或无权限"),
    /**
     * Skill 创建失败
     */
    SKILL_CREATE_ERROR(500, "HME_SKILL_005", "Skill 创建失败"),
    /**
     * Skill 更新失败
     */
    SKILL_UPDATE_ERROR(500, "HME_SKILL_006", "Skill 更新失败"),
    /**
     * Skill 发布失败
     */
    SKILL_PUBLISH_ERROR(500, "HME_SKILL_007", "Skill 发布失败"),
    /**
     * Skill 版本上传失败
     */
    SKILL_VERSION_UPLOAD_ERROR(500, "HME_SKILL_008", "Skill 版本上传失败"),
    /**
     * Skill 版本不存在
     */
    SKILL_VERSION_NOT_FOUND(404, "HME_SKILL_009", "Skill 版本不存在"),
    /**
     * Skill 版本下载/解压失败
     */
    SKILL_VERSION_DOWNLOAD_ERROR(500, "HME_SKILL_010", "Skill 版本下载或解压失败"),
    /**
     * Skill 包缺少 SKILL.md 文件
     */
    SKILL_MISSING_MANIFEST(400, "HME_SKILL_011", "Skill 包缺少 SKILL.md 文件"),
    /**
     * 加载内置 Skill 失败
     */
    SKILL_BUILTIN_LOAD_ERROR(500, "HME_SKILL_012", "加载内置 Skill 失败"),
    /**
     * Skill 目录不存在
     */
    SKILL_DIRECTORY_NOT_FOUND(500, "HME_SKILL_013", "Skill 目录不存在"),
    /**
     * Skill 资源路径不能为空
     */
    SKILL_RESOURCE_PATH_EMPTY(400, "HME_SKILL_014", "Skill 资源路径不能为空"),
    /**
     * Skill 资源路径不允许越级或绝对路径
     */
    SKILL_RESOURCE_PATH_INVALID(400, "HME_SKILL_015", "Skill 资源路径不允许越级或绝对路径"),

    // ==================== PROVIDER — 模型供应商 ====================

    /**
     * Provider 不存在或无权限访问
     */
    PROVIDER_NOT_FOUND(404, "HME_PROVIDER_001", "Provider 不存在或无权限访问"),
    /**
     * 至少需要保留一个激活的 Provider
     */
    PROVIDER_LAST_ACTIVE(403, "HME_PROVIDER_002", "至少需要保留一个激活的 Provider，无法停用最后一个"),
    /**
     * 内置 Provider 初始化失败
     */
    PROVIDER_BUILTIN_INIT_ERROR(500, "HME_PROVIDER_003", "内置 Provider 初始化失败"),
    /**
     * 未找到内置模型供应商
     */
    PROVIDER_BUILTIN_NOT_FOUND(500, "HME_PROVIDER_004", "未找到内置模型供应商"),
    /**
     * 模型 API Key 未配置
     */
    PROVIDER_API_KEY_MISSING(500, "HME_PROVIDER_005", "模型 API Key 未配置"),
    /**
     * 用户未配置任何激活的模型供应商
     */
    PROVIDER_NO_ACTIVE(500, "HME_PROVIDER_006", "用户未配置任何激活的模型供应商，请先在模型配置页面配置"),
    /**
     * 供应商 API Key 未配置
     */
    PROVIDER_API_KEY_NOT_CONFIGURED(500, "HME_PROVIDER_007", "供应商 API Key 未配置"),
    /**
     * 内置模型目录中未配置任何供应商
     */
    PROVIDER_BUILTIN_EMPTY(500, "HME_PROVIDER_008", "内置模型目录中未配置任何供应商"),
    /**
     * 内置模型目录中存在重复的供应商 ID
     */
    PROVIDER_BUILTIN_DUPLICATE(500, "HME_PROVIDER_009", "内置模型目录中存在重复的供应商 ID"),
    /**
     * 加载内置模型目录失败
     */
    PROVIDER_BUILTIN_LOAD_ERROR(500, "HME_PROVIDER_010", "加载内置模型目录失败"),
    /**
     * 供应商未配置任何模型
     */
    PROVIDER_NO_MODELS(500, "HME_PROVIDER_011", "供应商未配置任何模型"),
    /**
     * 内置模型目录存在缺少 ID 的供应商配置
     */
    PROVIDER_BUILTIN_MISSING_ID(500, "HME_PROVIDER_012", "内置模型目录存在缺少 ID 的供应商配置"),
    /**
     * 供应商未配置 providerType
     */
    PROVIDER_BUILTIN_MISSING_TYPE(500, "HME_PROVIDER_013", "供应商未配置 providerType"),
    /**
     * 不支持的模型供应商类型
     */
    PROVIDER_UNSUPPORTED_TYPE(400, "HME_PROVIDER_014", "不支持的模型供应商类型"),

    // ==================== MCP — MCP 配置与工具 ====================

    /**
     * ReMe MCP 客户端未初始化
     */
    MCP_CLIENT_NOT_INITIALIZED(500, "HME_MCP_001", "ReMe MCP 客户端未初始化"),
    /**
     * MCP 模式未启用
     */
    MCP_MODE_NOT_ENABLED(500, "HME_MCP_002", "MCP 模式未启用，无法调用 MCP 工具"),
    /**
     * MCP 工具调用失败
     */
    MCP_TOOL_CALL_ERROR(500, "HME_MCP_003", "MCP 工具调用失败"),

    // ==================== PROFILE — Agent Profile ====================

    /**
     * Profile 文件列表大小不一致
     */
    PROFILE_LIST_SIZE_MISMATCH(400, "HME_PROFILE_001", "filenames、contents、enabled 列表大小不一致"),
    /**
     * 默认 Profile 文件不存在或为空
     */
    PROFILE_DEFAULT_NOT_FOUND(404, "HME_PROFILE_002", "默认 Profile 文件不存在或为空"),
    /**
     * filename 不能为空
     */
    PROFILE_FILENAME_EMPTY(400, "HME_PROFILE_003", "filename 不能为空"),
    /**
     * 不合法的文件名
     */
    PROFILE_FILENAME_INVALID(400, "HME_PROFILE_004", "不合法的文件名"),

    // ==================== MEMORY — 记忆管理 ====================

    /**
     * 记忆文件列表获取失败
     */
    MEMORY_LIST_ERROR(500, "HME_MEMORY_001", "记忆文件列表获取失败"),
    /**
     * 记忆文件读取失败
     */
    MEMORY_READ_ERROR(500, "HME_MEMORY_002", "记忆文件读取失败"),
    /**
     * 记忆文件编辑失败
     */
    MEMORY_EDIT_ERROR(500, "HME_MEMORY_003", "记忆文件编辑失败"),
    /**
     * 记忆搜索失败
     */
    MEMORY_SEARCH_ERROR(500, "HME_MEMORY_004", "记忆搜索失败"),

    // ==================== STORAGE — 文件存储 ====================

    /**
     * OSS AccessKey 未配置
     */
    STORAGE_OSS_CONFIG_MISSING(500, "HME_STORAGE_001", "OSS AccessKey ID 和 Secret 必须配置"),
    /**
     * 文件上传失败
     */
    STORAGE_UPLOAD_ERROR(500, "HME_STORAGE_002", "文件上传失败"),
    /**
     * 文件下载失败
     */
    STORAGE_DOWNLOAD_ERROR(500, "HME_STORAGE_003", "文件下载失败"),
    /**
     * 文件删除失败
     */
    STORAGE_DELETE_ERROR(500, "HME_STORAGE_004", "文件删除失败"),
    /**
     * 检查文件存在失败
     */
    STORAGE_EXISTENCE_CHECK_ERROR(500, "HME_STORAGE_005", "检查文件存在失败"),
    /**
     * 生成预签名下载 URL 失败
     */
    STORAGE_PRESIGN_DOWNLOAD_ERROR(500, "HME_STORAGE_006", "生成预签名下载 URL 失败"),
    /**
     * 生成预签名上传 URL 失败
     */
    STORAGE_PRESIGN_UPLOAD_ERROR(500, "HME_STORAGE_007", "生成预签名上传 URL 失败");

    /**
     * HTTP 状态码。
     */
    private final int httpStatus;

    /**
     * 业务错误码（如 HME_AGENT_001）。
     */
    private final String code;

    /**
     * 错误消息。
     */
    private final String message;

    HmeErrorCode(int httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
