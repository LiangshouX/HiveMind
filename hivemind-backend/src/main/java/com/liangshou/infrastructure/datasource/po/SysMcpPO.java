package com.liangshou.infrastructure.datasource.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * MCP配置表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_mcp")
public class SysMcpPO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**

     * 主键ID

     */

    @TableId(type = IdType.AUTO)
    private Long id;

    /**

     * 任务所属的用户ID

     */

    @TableField("user_id")

    private Long userId;

    /**

     * MCP Server

     */

    @TableField("mcp_server")

    private String mcpServer;

    /**

     * MCP类型: SYSTEM/CUSTOM

     */

    @TableField("mcp_server_type")

    private String mcpServerType;

    /**

     * MCP Server下对应的具体工具

     */

    @TableField("mcp_tool")

    private String mcpTool;

    /**

     * 是否启用Server: 0-否, 1-是

     */

    @TableField("is_server_activated")

    private Integer isServerActivated;

    /**

     * 是否启用Tool: 0-否, 1-是

     */

    @TableField("is_tool_activated")

    private Integer isToolActivated;

    /**

     * 工具参数配置

     */

    @TableField("tool_param_config")

    private String toolParamConfig;

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
