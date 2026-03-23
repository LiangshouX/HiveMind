package com.liangshou.tangdynasty.agentic.common.config;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.skill.repository.ClasspathSkillRepository;
import io.agentscope.core.tool.Toolkit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.List;

@Configuration
@SuppressWarnings("unused")
public class SkillsConfig {

    @Bean
    public ClasspathSkillRepository skillRepository() throws IOException {
        return new ClasspathSkillRepository("skills");
    }

    @Bean
    public Toolkit toolkit() {
        return new Toolkit();
    }

    @Bean
    public SkillBox skillBox(Toolkit toolkit, ClasspathSkillRepository skillRepository) {
        SkillBox skillBox = new SkillBox(toolkit);
        List<AgentSkill> skills = skillRepository.getAllSkills();
        for (AgentSkill skill : skills) {
            skillBox.registration().skill(skill).apply();
        }
        return skillBox;
    }

    @Bean("sqlAssistantAgent")
    public ReActAgent sqlAssistantAgent(Toolkit toolkit, SkillBox skillBox) {
        String key = System.getenv("AI_DASHSCOPE_API_KEY");
        return ReActAgent.builder()
                .name("sql_assistant")
                .sysPrompt("")
                .model(DashScopeChatModel.builder().apiKey(key).modelName("qwen-plus").build())
                .toolkit(toolkit)
                .skillBox(skillBox)
                .memory(new InMemoryMemory())
                .build();
    }
}
