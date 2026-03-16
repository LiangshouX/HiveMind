package com.liangshou.adapter.controller;

import com.liangshou.common.Result;
import com.liangshou.infrastructure.datasource.po.OfficialPO;
import com.liangshou.infrastructure.datasource.po.ModelPO;
import com.liangshou.service.OfficialService;
import com.liangshou.service.ModelService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AgentController {

    private final OfficialService officialService;
    private final ModelService modelService;

    // --- Agent Config ---
    @GetMapping("/agent-config")
    public Result<AgentConfigDTO> getAgentConfig() {
        List<OfficialPO> officials = officialService.list();
        List<ModelPO> models = modelService.list();

        List<Map<String, Object>> agentList = officials.stream().map(o -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", o.getName()); // Use name as ID for now as per frontend
            map.put("name", o.getName());
            map.put("role", o.getTitle());
            map.put("description", o.getBio());
            // Get model from modelConfig or default
            String model = "qwen-turbo"; // Default
            if (o.getModelConfig() != null && o.getModelConfig().containsKey("model")) {
                model = (String) o.getModelConfig().get("model");
            }
            map.put("model", model);
            map.put("status", "idle"); // Dummy status
            map.put("skills", o.getSkills() != null ? o.getSkills() : new ArrayList<>());
            return map;
        }).collect(Collectors.toList());

        List<String> modelNames = models.stream().map(ModelPO::getName).collect(Collectors.toList());
        if (modelNames.isEmpty()) {
            modelNames.add("qwen-turbo");
            modelNames.add("qwen-plus");
        }

        return Result.success(new AgentConfigDTO(agentList, modelNames));
    }

    @Data
    public static class AgentConfigDTO {
        private List<Map<String, Object>> agents;
        private List<String> knownModels;

        public AgentConfigDTO(List<Map<String, Object>> agents, List<String> knownModels) {
            this.agents = agents;
            this.knownModels = knownModels;
        }
    }

    // --- Agents Status ---
    @GetMapping("/agents-status")
    public Result<Map<String, Object>> getAgentsStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("ok", true);
        
        Map<String, Object> gateway = new HashMap<>();
        gateway.put("alive", true);
        gateway.put("probe", true);
        gateway.put("status", "running");
        status.put("gateway", gateway);
        
        status.put("checkedAt", LocalDateTime.now().toString());
        
        // Dummy agent status
        List<OfficialPO> officials = officialService.list();
        List<Map<String, Object>> agents = officials.stream().map(o -> {
             Map<String, Object> map = new HashMap<>();
             map.put("id", o.getName());
             map.put("status", "active");
             return map;
        }).collect(Collectors.toList());
        status.put("agents", agents);
        
        return Result.success(status);
    }

    // --- Set Model ---
    @PostMapping("/set-model")
    public Result<Boolean> setModel(@RequestBody Map<String, String> payload) {
        String agentId = payload.get("agentId");
        String model = payload.get("model");
        
        OfficialPO official = officialService.lambdaQuery().eq(OfficialPO::getName, agentId).one();
        if (official != null) {
            Map<String, Object> config = official.getModelConfig();
            if (config == null) {
                config = new HashMap<>();
            }
            config.put("model", model);
            official.setModelConfig(config);
            officialService.updateById(official);
            // TODO: Log model change
            return Result.success(true);
        }
        return Result.error("Agent not found");
    }

    // --- Wake Agent ---
    @PostMapping("/agent-wake")
    public Result<Boolean> wakeAgent(@RequestBody Map<String, String> payload) {
        String agentId = payload.get("agentId");
        // Logic to wake up agent (e.g. check message queue)
        return Result.success(true);
    }

    // --- Add Skill ---
    @PostMapping("/add-skill")
    public Result<Boolean> addSkill(@RequestBody Map<String, String> payload) {
        String agentId = payload.get("agentId");
        String skillName = payload.get("skillName");
        
        OfficialPO official = officialService.lambdaQuery().eq(OfficialPO::getName, agentId).one();
        if (official != null) {
            List<String> skills = official.getSkills();
            if (skills == null) {
                skills = new ArrayList<>();
            }
            if (!skills.contains(skillName)) {
                skills.add(skillName);
                official.setSkills(skills);
                officialService.updateById(official);
            }
            return Result.success(true);
        }
        return Result.error("Agent not found");
    }

    // --- Model Change Log ---
    @GetMapping("/model-change-log")
    public Result<List<Object>> getModelChangeLog() {
        // Return empty list for now
        return Result.success(new ArrayList<>());
    }
    
    // --- Officials Stats (stub) ---
    @GetMapping("/officials-stats")
    public Result<Map<String, Object>> getOfficialsStats() {
        Map<String, Object> data = new HashMap<>();
        data.put("officials", new ArrayList<>());
        Map<String, Object> totals = new HashMap<>();
        totals.put("tasks_done", 0);
        totals.put("cost_cny", 0.0);
        data.put("totals", totals);
        data.put("top_official", "None");
        return Result.success(data);
    }
}
