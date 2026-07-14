package com.liangshou.agentic.agents.memory.reme;

import com.liangshou.agentic.common.exceptions.BizException;
import com.liangshou.agentic.common.exceptions.HmeErrorCode;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.ToolCallParam;
import io.agentscope.core.tool.mcp.McpClientWrapper;
import io.agentscope.core.tool.mcp.McpTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * 带用户隔离的 MCP 工具包装器。
 *
 * <p>在调用 MCP 工具时，自动为文件操作类工具的 path 参数添加用户 ID 前缀，
 * 实现用户间的数据隔离。</p>
 *
 * <p>对于 search 工具，会在搜索结果中过滤掉不属于当前用户目录的内容。</p>
 *
 * @author LiangshouX
 */
public class UserScopedMcpTool extends McpTool {

    private static final Logger log = LoggerFactory.getLogger(UserScopedMcpTool.class);

    /**
     * 需要添加用户路径前缀的文件操作工具
     */
    private static final Set<String> FILE_PATH_TOOLS = Set.of(
            "read", "write", "edit", "list", "move", "delete",
            "read_image", "daily_write", "daily_list",
            "frontmatter_read", "frontmatter_update", "frontmatter_delete"
    );

    /**
     * 需要过滤搜索结果的工具
     */
    private static final Set<String> SEARCH_TOOLS = Set.of("search", "node_search");

    private final String userId;

    /**
     * 构造用户隔离的 MCP 工具。
     *
     * @param original 原始 MCP 工具
     * @param userId   用户 ID，用作 workspace 隔离前缀
     */
    public UserScopedMcpTool(McpTool original, String userId) {
        super(
                original.getName(),
                original.getDescription(),
                original.getParameters(),
                getClientWrapper(original),
                original.getPresetArguments()
        );
        this.userId = userId;
    }

    @Override
    public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
        if (userId == null || userId.isBlank()) {
            return super.callAsync(param);
        }

        // 对文件操作类工具添加用户路径前缀
        if (FILE_PATH_TOOLS.contains(getName())) {
            param = applyFilePathScope(param);
        }

        // 对 list 工具，确保 recursive=true
        if ("list".equals(getName())) {
            param = ensureRecursive(param);
        }

        // 对搜索工具，调用后过滤结果
        if (SEARCH_TOOLS.contains(getName())) {
            return super.callAsync(param)
                    .map(this::filterSearchResults);
        }

        return super.callAsync(param);
    }

    /**
     * 确保 list 工具的 recursive 参数为 true。
     */
    private ToolCallParam ensureRecursive(ToolCallParam param) {
        Map<String, Object> input = param.getInput();
        if (input == null) {
            input = new HashMap<>();
        }

        // 如果没有设置 recursive 或为 false，设置为 true
        if (!input.containsKey("recursive") || !Boolean.TRUE.equals(input.get("recursive"))) {
            Map<String, Object> newInput = new HashMap<>(input);
            newInput.put("recursive", true);
            log.info("[UserScoped] Set recursive=true for list tool");

            return ToolCallParam.builder()
                    .toolUseBlock(param.getToolUseBlock())
                    .input(newInput)
                    .agent(param.getAgent())
                    .context(param.getContext())
                    .emitter(param.getEmitter())
                    .build();
        }

        return param;
    }

    /**
     * 对文件操作类工具的参数应用用户路径前缀。
     *
     * <p>当 path 为空时，设置为 userId/（用户的根目录）。
     * 当 path 不为空且不包含用户前缀时，添加 userId/ 前缀。</p>
     */
    private ToolCallParam applyFilePathScope(ToolCallParam param) {
        Map<String, Object> input = param.getInput();
        if (input == null) {
            return param;
        }

        // 获取 path，如果不存在则默认为空字符串
        String path = input.containsKey("path") ? (String) input.get("path") : "";
        if (path == null) {
            path = "";
        }

        // 统一路径分隔符为 Unix 格式
        String normalizedPath = path.replace("\\", "/");

        // 如果 path 已经包含用户前缀，直接返回
        if (normalizedPath.startsWith(userId + "/") || normalizedPath.startsWith(userId + "\\")) {
            log.debug("[UserScoped] path already has user prefix: {}", path);
            return param;
        }

        // 构建用户隔离的路径
        String scopedPath;
        if (normalizedPath.isBlank()) {
            // 空路径 → 用户根目录
            scopedPath = userId + "/";
        } else {
            // 非空路径 → 添加用户前缀
            scopedPath = userId + "/" + normalizedPath;
        }

        log.info("[UserScoped] Tool: {}, userId: {}, originalPath: '{}', scopedPath: '{}'",
                getName(), userId, path, scopedPath);

        // 创建新的参数 map
        Map<String, Object> scopedInput = new HashMap<>(input);
        scopedInput.put("path", scopedPath);

        // 创建新的 ToolCallParam
        return ToolCallParam.builder()
                .toolUseBlock(param.getToolUseBlock())
                .input(scopedInput)
                .agent(param.getAgent())
                .context(param.getContext())
                .emitter(param.getEmitter())
                .build();
    }

    /**
     * 过滤搜索结果，只保留属于当前用户目录的内容。
     */
    private ToolResultBlock filterSearchResults(ToolResultBlock result) {
        if (result == null || result.getOutput() == null || result.getOutput().isEmpty()) {
            return result;
        }

        List<ContentBlock> filteredOutput = new ArrayList<>();
        String userPrefix = userId + "/";

        for (ContentBlock block : result.getOutput()) {
            if (block instanceof TextBlock textBlock) {
                String filteredText = filterTextByUser(textBlock.getText(), userPrefix);
                if (!filteredText.isBlank()) {
                    filteredOutput.add(TextBlock.builder().text(filteredText).build());
                }
            } else {
                filteredOutput.add(block);
            }
        }

        // 如果过滤后没有内容，返回提示信息
        if (filteredOutput.isEmpty()) {
            filteredOutput.add(TextBlock.builder()
                    .text("No memories found for user " + userId)
                    .build());
        }

        // 创建新的 ToolResultBlock
        return ToolResultBlock.of(result.getId(), result.getName(), filteredOutput);
    }

    /**
     * 从文本结果中过滤出属于当前用户的内容。
     * <p>
     * ReMe 搜索结果格式通常包含文件路径，如：
     * ```
     * [score=0.1234] daily/2025-07-12.md
     * 内容摘要...
     * ```
     * <p>
     * 我们需要过滤掉不包含用户目录前缀的结果。
     */
    private String filterTextByUser(String text, String userPrefix) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String[] lines = text.split("\n");
        StringBuilder result = new StringBuilder();
        boolean inUserBlock = false;

        for (String line : lines) {
            // 检查是否是新的结果块开始（通常包含文件路径）
            if (line.contains(userPrefix)) {
                inUserBlock = true;
                result.append(line).append("\n");
            } else if (inUserBlock && !line.isBlank()) {
                // 继续添加当前块的内容
                result.append(line).append("\n");
            } else if (line.isBlank()) {
                // 空行可能是块分隔符
                if (inUserBlock) {
                    result.append("\n");
                }
                inUserBlock = false;
            }
        }

        return result.toString().trim();
    }

    /**
     * 从 McpTool 实例中获取 McpClientWrapper。
     */
    private static McpClientWrapper getClientWrapper(McpTool tool) {
        try {
            var field = McpTool.class.getDeclaredField("clientWrapper");
            field.setAccessible(true);
            return (McpClientWrapper) field.get(tool);
        } catch (Exception e) {
            throw new BizException(HmeErrorCode.MCP_TOOL_CALL_ERROR, e);
        }
    }
}
