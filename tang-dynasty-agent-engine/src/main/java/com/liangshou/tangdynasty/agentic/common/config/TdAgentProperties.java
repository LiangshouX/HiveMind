package com.liangshou.tangdynasty.agentic.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Agent 配置信息读取类
 *
 * @author LiangshouX
 */

@Getter
@Setter
@ConfigurationProperties(prefix = "tdagent")
public class TdAgentProperties {

    private SystemPrompt systemPrompt = new SystemPrompt();

    private Model model = new Model();

    private Sandbox sandbox = new Sandbox();

    private ToolGuard toolGuard = new ToolGuard();

    private ReMe reme = new ReMe();

    private Compaction compaction = new Compaction();

    private Streaming streaming = new Streaming();

    /**
     * System prompt 相关配置。
     */
    @Getter
    @Setter
    public static class SystemPrompt {

        private String productName = "TDAgent";

        private String ownerName = "TangWorkSpace";

        private int maxHistoryPreview = 12;
    }

    /**
     * 模型相关配置。
     */
    @Getter
    @Setter
    public static class Model {

        private String providerConfigLocation = "classpath:provider/builtin_provider.json";

        private boolean reloadOnChange = true;

        private String providerId = "dashscope";

        private String modelId;

        private String apiKey;

        private String baseUrl;

        private String endpointPath;

        private String modelName = "qwen-max";

        private boolean stream;

        private boolean enableThinking;

        private int maxIters = 8;
    }

    /**
     * Sandbox 相关配置。
     */
    @Getter
    @Setter
    public static class Sandbox {

        private boolean enabled = true;

        private boolean browserEnabled = true;

        private boolean filesystemEnabled = true;

        private boolean strictStartup;
    }

    /**
     * Tool guard 相关配置。
     */
    @Getter
    @Setter
    public static class ToolGuard {

        private boolean enabled = true;

        private boolean strictMode = true;

        private int pendingExpireMinutes = 60;
    }

    /**
     * ReMe 相关配置。
     */
    @Getter
    @Setter
    public static class ReMe {

        private boolean enabled = true;

        private String baseUrl = "http://localhost:8085";

        private int timeoutSeconds = 60;

        private int topK = 5;
    }

    /**
     * 对话压缩相关配置。
     */
    @Getter
    @Setter
    public static class Compaction {

        private boolean enabled = true;

        private int triggerMessageCount = 20;

        private int triggerCharacterCount = 24000;

        private int keepRecentMessages = 8;

        private int maxSummaryCharacters = 2400;
    }

    /**
     * 流式接口相关配置。
     */
    @Getter
    @Setter
    public static class Streaming {

        private boolean enabled = true;

        private boolean incremental = true;
    }
}

