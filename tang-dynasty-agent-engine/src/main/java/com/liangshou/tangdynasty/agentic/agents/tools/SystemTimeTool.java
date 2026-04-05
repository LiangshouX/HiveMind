package com.liangshou.tangdynasty.agentic.agents.tools;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 系统时间工具 - 为 Agent 提供获取当前系统时间的能力。
 *
 * <p>该工具允许 Agent 查询指定时区的当前时间，支持：</p>
 * <ul>
 *     <li>通过时区 ID（如 Asia/Shanghai、UTC）获取对应时区的时间</li>
 *     <li>返回格式化的时间字符串（yyyy-MM-dd HH:mm:ss）</li>
 *     <li>当时区无效时，自动降级为系统默认时区</li>
 * </ul>
 *
 * <p>使用场景：</p>
 * <ul>
 *     <li>Agent 需要知道当前时间来安排任务或回答时间相关问题</li>
 *     <li>跨时区协作时需要转换时间</li>
 *     <li>记录事件发生的时间戳</li>
 * </ul>
 *
 * @author LiangshouX
 */
@Component
@SuppressWarnings("unused")
public class SystemTimeTool {

    @Tool(name = "get_current_time", description = "Get the current system time in a specific timezone")
    public String getCurrentTime(
            @ToolParam(name = "timezone", description = "Timezone ID, e.g., Asia/Shanghai or UTC") String timezone) {
        try {
            ZoneId zoneId = ZoneId.of(timezone);
            return LocalDateTime.now(zoneId)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            return "Invalid timezone provided. Defaulting to system time: "
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
    }
}
