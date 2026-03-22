package com.liangshou.infrastructure.datasource.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI Agent通讯渠道配置表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_channels")
public class SysChannelsPO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**

     * 主键ID

     */

    @TableId(type = IdType.AUTO)
    private Long id;

    /**

     * 登录的用户ID

     */

    @TableField("user_id")

    private Long userId;

    /**

     * 渠道名

     */

    @TableField("channel_name")

    private String channelName;

    /**

     * 是否启用: 0-否, 1-是

     */

    @TableField("is_active")

    private Integer isActive;

    /**

     * 机器人前缀

     */

    @TableField("bot_prefix")

    private String botPrefix;

    /**

     * 是否显示工具信息: 0-否, 1-是

     */

    @TableField("show_tool_message")

    private Integer showToolMessage;

    /**

     * 是否显示思考过程: 0-否, 1-是

     */

    @TableField("show_thinking")

    private Integer showThinking;

    /**

     * 说明文档地址

     */

    @TableField("documentation_address")

    private String documentationAddress;

    /**

     * 渠道对应的针对性配置

     */

    @TableField("specific_config")

    private String specificConfig;

    /**

     * 创建时间

     */

    @TableField(fill = FieldFill.INSERT, value = "create_time")

    private LocalDateTime createTime;

    /**

     * 更新时间

     */

    @TableField(fill = FieldFill.INSERT_UPDATE, value = "update_time")

    private LocalDateTime updateTime;

}
