package com.liangshou.infrastructure.datasource.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 模型配置表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_models")
public class SysModelsPO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    @TableField("user_id")
    private String userId;

    /** 模型供应商 */
    @TableField("model_provider_id")
    private String modelProviderId;

    /** Provider 显示名称 */
    @TableField("provider_name")
    private String providerName;

    /** 供应商类型: SYSTEM/CUSTOM/LOCAL */
    @TableField("model_provider_type")
    private String modelProviderType;

    /** 模型供应商是否启用: 0-否, 1-是 */
    @TableField("is_provider_activated")
    private Integer isProviderActivated;

    /** 是否系统内置: 0-否, 1-是 */
    @TableField("is_system_built_in")
    private Integer isSystemBuiltIn;

    /** 访问地址 */
    @TableField("base_url")
    private String baseUrl;

    /** 秘钥, 加密存储 */
    @TableField("api_key")
    private String apiKey;

    /** 模型列表 JSON 数组 */
    @TableField("models_json")
    private String modelsJson;

    /** 请求的模型ID */
    @TableField("model_id")
    private String modelId;

    /** 模型名称 */
    @TableField("model_name")
    private String modelName;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT, value = "create_time")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE, value = "update_time")
    private LocalDateTime updateTime;
}
