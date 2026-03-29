package com.liangshou.tangdynasty.agentic.agents.tools;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

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
