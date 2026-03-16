package com.liangshou.common.config;

import com.liangshou.infrastructure.agentsupport.mcp.McpClientManager;
import com.liangshou.infrastructure.agentsupport.tools.SkillExecutor;
import com.liangshou.infrastructure.agentsupport.tools.ToolExecutor;
import com.liangshou.infrastructure.datasource.po.McpPO;
import com.liangshou.infrastructure.datasource.po.SkillPO;
import com.liangshou.infrastructure.datasource.po.ToolPO;
import com.liangshou.infrastructure.datasource.support.McpRepository;
import com.liangshou.infrastructure.datasource.support.SkillRepository;
import com.liangshou.infrastructure.datasource.support.ToolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Agent 工具自动配置
 * 
 * 在应用启动时自动加载已启用的 MCP、Skill 和 Tool 配置
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class AgentToolsAutoConfig {
    
    private final McpClientManager mcpClientManager;
    private final SkillExecutor skillExecutor;
    private final ToolExecutor toolExecutor;
    private final McpRepository mcpRepository;
    private final SkillRepository skillRepository;
    private final ToolRepository toolRepository;
    
    /**
     * 应用启动时初始化所有启用的工具
     */
    @Bean
    public CommandLineRunner initAgentTools() {
        return args -> {
            log.info("Initializing agent tools...");
            
            // 加载所有启用的 MCP（从 Repository 层获取）
            List<McpPO> enabledMcps = mcpRepository.findAllEnabled();
            for (McpPO mcp : enabledMcps) {
                try {
                    mcpClientManager.startClient(mcp);
                    log.info("Loaded MCP: {}", mcp.getName());
                } catch (Exception e) {
                    log.error("Failed to load MCP {}: {}", mcp.getName(), e.getMessage(), e);
                }
            }
            
            // 加载所有启用的 Skill（从 Repository 层获取）
            List<SkillPO> enabledSkills = skillRepository.findAllEnabled();
            for (SkillPO skill : enabledSkills) {
                try {
                    skillExecutor.loadSkill(skill);
                    log.info("Loaded Skill: {}", skill.getName());
                } catch (Exception e) {
                    log.error("Failed to load skill {}: {}", skill.getName(), e.getMessage(), e);
                }
            }
            
            // 加载所有启用的 Tool（从 Repository 层获取）
            List<ToolPO> enabledTools = toolRepository.findAllEnabled();
            for (ToolPO tool : enabledTools) {
                try {
                    toolExecutor.loadToolFromConfig(tool);
                    log.info("Loaded Tool: {}", tool.getName());
                } catch (Exception e) {
                    log.error("Failed to load tool {}: {}", tool.getName(), e.getMessage(), e);
                }
            }
            
            log.info("Agent tools initialization completed. Loaded {} MCPs, {} Skills, {} Tools", 
                    enabledMcps.size(), enabledSkills.size(), enabledTools.size());
        };
    }
}
