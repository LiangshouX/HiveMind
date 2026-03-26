package com.liangshou.tangdynasty.agentic.quickstart;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.runtime.sandbox.box.BaseSandbox;
import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.manager.ManagerConfig;
import io.agentscope.runtime.sandbox.manager.SandboxService;
import io.agentscope.runtime.sandbox.manager.client.container.BaseClientStarter;
import io.agentscope.runtime.sandbox.manager.client.container.docker.DockerClientStarter;
import lombok.extern.slf4j.Slf4j;

/**
 * 测试 AgentScope 沙箱使用 Demo
 */
@Slf4j
@SuppressWarnings("unused")
public class ASSandboxQS {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        Sandbox baseSandbox = connectToBaseSandbox();
    }

    /**
     * 连接到 BaseSandbox
     */
    private static Sandbox connectToBaseSandbox() {
        log.info("Connecting to BaseSandbox...");

        //        创建并启动沙箱服务
        BaseClientStarter clientConfig = DockerClientStarter.builder().build();
        ManagerConfig managerConfig = ManagerConfig.builder().clientStarter(clientConfig).build();
        SandboxService sandboxService = new SandboxService(managerConfig);
        sandboxService.start();

        // 连接沙箱（沙箱会在执行后自动删除）
        try (BaseSandbox baseSandbox = new BaseSandbox(sandboxService, "userId", "sessionId")) {
            // 格式化输出沙箱工具列表
            log.info("\n========== 沙箱工具列表 ==========\n");
            Object tools = baseSandbox.listTools("");
            log.info(formatJson(tools));
            log.info("\n==================================\\n");
        
            // 执行 Python 代码并格式化输出结果
            log.info("\n\n========== Python 执行结果 ==========\n\n");
            String pythonResult = baseSandbox.runIpythonCell("print('Hello from the sandbox!')");
            log.info(formatJson(pythonResult));
            log.info("\n\n====================================\n\n");
        
            // 执行 Shell 命令并格式化输出结果
            log.info("\n\n========== Shell 命令执行结果 ==========\n\n");
            String shellResult = baseSandbox.runShellCommand("echo Hello, World!");
            log.info(formatJson(shellResult));
            log.info("\n======================================\n\n");
        
            return baseSandbox;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 格式化 JSON 输出，使其更易读
     */
    private static String formatJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        try {
            // 如果已经是字符串，尝试解析为 JSON 并格式化
            if (obj instanceof String jsonStr) {
                if (jsonStr.trim().startsWith("{")) {
                    Object jsonObj = objectMapper.readValue(jsonStr, Object.class);
                    return objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(jsonObj);
                }
                return jsonStr;
            }
            // 如果是对象，直接格式化为 JSON
            return objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(obj);
        } catch (Exception e) {
            // 如果格式化失败，返回原始 toString
            return obj.toString();
        }
    }
}

