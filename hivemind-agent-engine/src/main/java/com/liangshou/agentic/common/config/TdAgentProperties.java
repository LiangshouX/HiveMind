package com.liangshou.agentic.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Agent 引擎配置属性 - 封装所有与 HiveMind Agent 相关的配置项。
 *
 * <p>该配置类通过 Spring Boot 的 {@code @ConfigurationProperties} 机制，
 * 从 application.yaml 中读取前缀为 "tdagent" 的配置项，包括：</p>
 *
 * <ul>
 *     <li><b>SystemPrompt</b>：系统提示词相关配置（产品名称、所有者名称、历史预览长度）</li>
 *     <li><b>Model</b>：LLM 模型配置（供应商 ID、模型 ID、API Key、Base URL、流式选项等）</li>
 *     <li><b>Sandbox</b>：沙箱环境配置（是否启用、浏览器/文件系统支持、严格启动模式）</li>
 *     <li><b>ToolGuard</b>：工具防护配置（是否启用、严格模式、审批过期时间）</li>
 *     <li><b>ReMe</b>：长期记忆服务配置（Base URL、超时时间、Top-K 检索数量）</li>
 *     <li><b>Compaction</b>：对话压缩配置（触发阈值、保留消息数、最大摘要长度）</li>
 *     <li><b>Streaming</b>：流式响应配置（是否启用、增量模式）</li>
 *     <li><b>Observability</b>：可观测性配置（Studio URL、自动启动命令、启动超时）</li>
 * </ul>
 *
 * <p>配置示例（application.yaml）：</p>
 * <pre>{@code
 * tdagent:
 *   model:
 *     provider-id: dashscope
 *     model-name: qwen-max
 *     api-key: ${DASHSCOPE_API_KEY}
 *   sandbox:
 *     enabled: true
 *   tool-guard:
 *     enabled: true
 *     strict-mode: true
 * }</pre>
 *
 * @author LiangshouX
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "tdagent")
public class TdAgentProperties {

    private Observability observability = new Observability();

    private SystemPrompt systemPrompt = new SystemPrompt();

    private Model model = new Model();

    private Sandbox sandbox = new Sandbox();

    private ToolGuard toolGuard = new ToolGuard();

    private ReMe reme = new ReMe();

    private Compaction compaction = new Compaction();

    private Streaming streaming = new Streaming();

    private Skill skill = new Skill();

    /**
     * 可观测性相关配置
     */
    @Getter
    @Setter
    public static class Observability {

        private boolean enabled = true;

        private String url;

        /**
         * Studio 服务不可用时，尝试通过此命令启动。
         * 设为 null 或空字符串则跳过自动启动。
         */
        private String startupCommand = "agentscope studio";

        /**
         * 等待 Studio 服务启动的最大秒数。
         */
        private int startupTimeoutSeconds = 30;
    }

    /**
     * System prompt 相关配置。
     */
    @Getter
    @Setter
    public static class SystemPrompt {

        private String productName = "HiveMindAgent";

        private String ownerName = "HiveMind";

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

        private String apiKey;

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

        /**
         * 压缩触发模式：TOKEN（基于token计量）| LEGACY（字符数/消息数）
         */
        private String triggerMode = "TOKEN";

        /**
         * 模型上下文窗口大小（tokens），0 表示自动检测
         */
        private int contextWindowSize = 0;

        /**
         * 压缩触发阈值比例（上下文窗口的百分比），默认 0.85
         */
        private double thresholdRatio = 0.85;

        /**
         * 输出预留 token 数，0 表示自动计算
         */
        private int outputReserveTokens = 0;

        /**
         * 前区保留比例（消息列表前 N% 的消息不压缩），默认 0.10
         */
        private double headRatio = 0.10;

        /**
         * 最小压缩间隔（压缩后至少再积累 N 条新消息才允许再次压缩）
         */
        private int minMessagesSinceCompaction = 3;

        private int triggerMessageCount = 20;

        private int triggerCharacterCount = 24000;

        private int keepRecentMessages = 6;

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

    /**
     * Agent Skill 相关配置。
     */
    @Getter
    @Setter
    public static class Skill {

        private boolean enabled = true;

        private String builtinLocation = "classpath:skills";

        private boolean builtinEnabledByDefault = true;

        private boolean customEnabledByDefault = true;
    }
}
