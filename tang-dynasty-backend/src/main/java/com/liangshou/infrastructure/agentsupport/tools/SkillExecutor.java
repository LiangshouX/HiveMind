package com.liangshou.infrastructure.agentsupport.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liangshou.infrastructure.datasource.po.SkillPO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.execution.DefaultToolExecutionEngine;
import org.springframework.ai.tool.execution.ToolExecutionEngine;
import org.springframework.ai.tool.registry.ToolRegistry;
import org.springframework.stereotype.Component;

import javax.script.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Skill 执行器
 * 
 * 负责加载和执行技能脚本，支持 JavaScript/Python 等脚本语言
 */
@Component
@Slf4j
public class SkillExecutor {
    
    private final Map<String, CompiledScript> compiledScripts = new ConcurrentHashMap<>();
    private final ScriptEngineManager engineManager = new ScriptEngineManager();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ToolExecutionEngine toolExecutionEngine = new DefaultToolExecutionEngine();
    
    /**
     * 加载技能脚本
     */
    public synchronized void loadSkill(SkillPO skill) {
        String name = skill.getName();
        String scriptContent = skill.getScript();
        
        try {
            // 使用 Nashorn JavaScript 引擎
            ScriptEngine engine = engineManager.getEngineByName("JavaScript");
            
            if (engine == null) {
                throw new RuntimeException("JavaScript engine not available");
            }
            
            // 编译脚本
            Compilable compilable = (Compilable) engine;
            CompiledScript compiledScript = compilable.compile(scriptContent);
            
            compiledScripts.put(name, compiledScript);
            log.info("Skill {} loaded successfully", name);
            
        } catch (Exception e) {
            log.error("Failed to load skill {}: {}", name, e.getMessage(), e);
            throw new RuntimeException("Failed to load skill: " + e.getMessage(), e);
        }
    }
    
    /**
     * 卸载技能
     */
    public synchronized void unloadSkill(String skillName) {
        compiledScripts.remove(skillName);
        log.info("Skill {} unloaded", skillName);
    }
    
    /**
     * 执行技能
     */
    public Object executeSkill(String skillName, Map<String, Object> context) throws Exception {
        CompiledScript script = compiledScripts.get(skillName);
        if (script == null) {
            throw new IllegalStateException("Skill not found: " + skillName);
        }
        
        // 创建绑定上下文
        Bindings bindings = script.getEngine().createBindings();
        bindings.put("context", context);
        bindings.put("logger", log);
        
        // 执行脚本
        Object result = script.eval(bindings);
        
        log.debug("Skill {} executed with result: {}", skillName, result);
        return result;
    }
    
    /**
     * 重新加载技能
     */
    public synchronized void reloadSkill(SkillPO skill) {
        String name = skill.getName();
        if (compiledScripts.containsKey(name)) {
            unloadSkill(name);
        }
        if (skill.getEnabled()) {
            loadSkill(skill);
        }
    }
    
    /**
     * 检查技能是否存在
     */
    public boolean hasSkill(String skillName) {
        return compiledScripts.containsKey(skillName);
    }
    
    /**
     * 获取所有已加载的技能
     */
    public java.util.List<String> getLoadedSkills() {
        return new java.util.ArrayList<>(compiledScripts.keySet());
    }
}
