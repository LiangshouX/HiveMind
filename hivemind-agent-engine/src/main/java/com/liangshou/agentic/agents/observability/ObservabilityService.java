package com.liangshou.agentic.agents.observability;

import com.liangshou.agentic.common.config.TdAgentProperties;
import io.agentscope.core.studio.StudioClient;
import io.agentscope.core.studio.StudioManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.concurrent.TimeUnit;

/**
 * AgentScope Studio 可观测性管理服务。
 *
 * <p>在应用启动时自动执行以下流程：</p>
 * <ol>
 *     <li>检查 Studio 服务是否已可用（HTTP 健康探测）</li>
 *     <li>如果不可用，尝试通过配置的 CLI 命令启动 Studio</li>
 *     <li>启动失败时记录 ERROR 日志，标记服务不可用，系统继续正常运行</li>
 * </ol>
 *
 * <p>该服务仅在 {@code tdagent.observability.enabled=true} 时激活。</p>
 *
 * @author LiangshouX
 */
@Service
@ConditionalOnProperty(prefix = "tdagent.observability", name = "enabled", havingValue = "true")
public class ObservabilityService {

    private static final Logger log = LoggerFactory.getLogger(ObservabilityService.class);

    private static final int HEALTH_CHECK_INTERVAL_MS = 2000;

    private final TdAgentProperties properties;

    private volatile StudioClient studioClient;

    private volatile boolean available = false;

    public ObservabilityService(TdAgentProperties properties) {
        this.properties = properties;
    }

    /**
     * 应用启动完成后执行 Studio 可用性检查和初始化。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        TdAgentProperties.Observability obs = properties.getObservability();
        String url = obs.getUrl() != null ? obs.getUrl() : "http://localhost:5173";
        int timeoutSeconds = obs.getStartupTimeoutSeconds();

        log.info("[Observability] 开始检查 Studio 服务可用性 - url: {}", url);

        // Step 1: 健康检查 — 服务是否已经在运行
        if (checkHealth(url)) {
            initializeStudioClient(url);
            return;
        }

        // Step 2: 尝试通过 CLI 启动 Studio
        String startupCommand = obs.getStartupCommand();
        if (startupCommand != null && !startupCommand.isBlank()) {
            log.warn("[Observability] Studio 服务不可用，尝试启动: {}", startupCommand);
            if (tryStartStudio(url, startupCommand, timeoutSeconds)) {
                initializeStudioClient(url);
                return;
            }
        }

        // Step 3: 优雅降级 — 记录错误，系统继续运行
        log.error("[Observability] Studio 服务不可用，可观测功能已禁用。"
                + "系统将正常运行，但无法追踪 Agent 行为。"
                + "请检查 Studio 是否已安装并可访问: {}", url);
        available = false;
    }

    /**
     * 获取 Studio 客户端实例。
     *
     * @return StudioClient 实例；如果服务不可用则返回 null
     */
    public StudioClient getClient() {
        return studioClient;
    }

    /**
     * 判断 Studio 服务是否可用。
     *
     * @return true 如果服务已连接且可用
     */
    public boolean isAvailable() {
        return available;
    }

    /**
     * 执行 HTTP 健康探测，检查 Studio 服务是否可达。
     *
     * @param url Studio 服务 URL
     * @return true 如果服务响应正常
     */
    private boolean checkHealth(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            int responseCode = connection.getResponseCode();
            connection.disconnect();
            boolean healthy = responseCode >= 200 && responseCode < 400;
            if (healthy) {
                log.info("[Observability] Studio 服务健康检查通过 - url: {}, status: {}", url, responseCode);
            }
            return healthy;
        } catch (IOException e) {
            log.debug("[Observability] Studio 服务健康检查失败 - url: {}, error: {}", url, e.getMessage());
            return false;
        }
    }

    /**
     * 尝试通过 CLI 命令启动 Studio，并等待其变为可用。
     *
     * @param url             Studio 服务 URL
     * @param startupCommand  启动命令
     * @param timeoutSeconds  最大等待秒数
     * @return true 如果 Studio 在超时前变为可用
     */
    private boolean tryStartStudio(String url, String startupCommand, int timeoutSeconds) {
        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", startupCommand);
            pb.redirectErrorStream(true);
            process = pb.start();
            log.info("[Observability] 已启动 Studio 进程，等待服务就绪... (超时: {}s)", timeoutSeconds);

            boolean healthy = waitForHealth(url, timeoutSeconds);
            if (healthy) {
                log.info("[Observability] Studio 服务已成功启动");
                return true;
            }

            log.warn("[Observability] 等待 Studio 启动超时 ({}s)", timeoutSeconds);
            return false;
        } catch (IOException e) {
            log.error("[Observability] 启动 Studio 失败: {}", e.getMessage());
            return false;
        } finally {
            // 如果启动失败，清理进程
            if (process != null && !available) {
                process.destroyForcibly();
            }
        }
    }

    /**
     * 轮询等待 Studio 服务变为可用。
     *
     * @param url            Studio 服务 URL
     * @param timeoutSeconds 最大等待秒数
     * @return true 如果服务在超时前变为可用
     */
    private boolean waitForHealth(String url, int timeoutSeconds) {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutSeconds);
        while (System.currentTimeMillis() < deadline) {
            if (checkHealth(url)) {
                return true;
            }
            try {
                Thread.sleep(HEALTH_CHECK_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("[Observability] 等待 Studio 启动被中断");
                return false;
            }
        }
        return false;
    }

    /**
     * 初始化 StudioManager 并获取 StudioClient 实例。
     *
     * @param url Studio 服务 URL
     */
    private void initializeStudioClient(String url) {
        try {
            StudioManager.init()
                    .studioUrl(url)
                    .project(properties.getSystemPrompt().getProductName())
                    .runName("%s-%d".formatted(
                            properties.getSystemPrompt().getProductName(),
                            System.currentTimeMillis()))
                    .initialize()
                    .block();

            studioClient = StudioManager.getClient();
            available = true;
            log.info("[Observability] Studio 已连接 - url: {}, client: {}", url, studioClient);
        } catch (Exception e) {
            log.error("[Observability] 初始化 StudioClient 失败: {}", e.getMessage(), e);
            available = false;
        }
    }
}
