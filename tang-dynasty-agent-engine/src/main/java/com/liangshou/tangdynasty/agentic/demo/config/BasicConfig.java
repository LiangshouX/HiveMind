package com.liangshou.tangdynasty.agentic.demo.config;

import com.liangshou.tangdynasty.agentic.agents.memory.session.MongoDBSessionHistoryService;
import com.liangshou.tangdynasty.agentic.demo.agent.AgentscopeBrowserUseAgent;
import io.agentscope.runtime.engine.services.agent_state.InMemoryStateService;
import io.agentscope.runtime.engine.services.memory.persistence.memory.service.InMemoryMemoryService;
import io.agentscope.runtime.sandbox.manager.ManagerConfig;
import io.agentscope.runtime.sandbox.manager.SandboxService;
import io.agentscope.runtime.sandbox.manager.client.container.BaseClientStarter;
import io.agentscope.runtime.sandbox.manager.client.container.docker.DockerClientStarter;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class BasicConfig {

    private static final Logger logger = LoggerFactory.getLogger(BasicConfig.class);

    private static AgentscopeBrowserUseAgent agentInstance;

    @Resource
    private MongoTemplate mongoTemplate;

    @Bean
    public InMemoryStateService stateService() {
        logger.info("Creating InMemoryStateService bean");
        return new InMemoryStateService();
    }

    @Bean
    public MongoDBSessionHistoryService sessionHistoryService() {
        logger.info("Creating MongoDBSessionHistoryService bean");
        return new MongoDBSessionHistoryService(mongoTemplate);
    }

    @Bean
    public InMemoryMemoryService memoryService() {
        logger.info("Creating InMemoryMemoryService bean");
        return new InMemoryMemoryService();
    }

    @Bean
    public SandboxService sandboxService() {
        logger.info("Creating SandboxService bean");
        BaseClientStarter clientConfig = DockerClientStarter.builder().build();
        ManagerConfig managerConfig = ManagerConfig.builder()
                .clientStarter(clientConfig)
                .build();
        SandboxService sandboxService = new SandboxService(managerConfig);
        sandboxService.start();
        return sandboxService;
    }

    @Bean
    public AgentscopeBrowserUseAgent agentscopeBrowserUseAgent(
            InMemoryStateService stateService,
            MongoDBSessionHistoryService sessionHistoryService,
            InMemoryMemoryService memoryService,
            SandboxService sandboxService) {
        logger.info("Creating AgentscopeBrowserUseAgent bean...");
        agentInstance = new AgentscopeBrowserUseAgent();

        // Set services as in AgentScopeDeployExample
        agentInstance.setStateService(stateService);
        agentInstance.setSessionHistoryService(sessionHistoryService);
        agentInstance.setMemoryService(memoryService);
        agentInstance.setSandboxService(sandboxService);

        // Note: start() will be called by Runner.start(), not here
        logger.info("AgentscopeBrowserUseAgent bean created");
        return agentInstance;
    }
}
