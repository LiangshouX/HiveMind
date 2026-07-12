package com.liangshou.agentic.agents.memory.reme;

import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.ToolCallParam;
import io.agentscope.core.tool.mcp.McpClientWrapper;
import io.agentscope.core.tool.mcp.McpTool;
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

    /** 需要添加用户路径前缀的文件操作工具 */
    private static final Set<String> FILE_PATH_TOOLS = Set.of(
            "read", "write", "edit", "list", "move", "delete",
            "read_image", "daily_write", "daily_list",
            "frontmatter_read", "frontmatter_update", "frontmatter_delete"
    );

    /** 需要过滤搜索结果的工具 */
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

        // 对搜索工具，调用后过滤结果
        if (SEARCH_TOOLS.contains(getName())) {
            return super.callAsync(param)
                    .map(this::filterSearchResults);
        }

        return super.callAsync(param);
    }

    /**
     * 对文件操作类工具的参数应用用户路径前缀。
     */
    private ToolCallParam applyFilePathScope(ToolCallParam param) {
        Map<String, Object> input = param.getInput();
        if (input == null || !input.containsKey("path")) {
            return param;
        }

        String path = (String) input.get("path");
        if (path == null || path.isBlank() || path.startsWith(userId + "/")) {
            return param;
        }

        // 创建新的参数 map，添加用户前缀
        Map<String, Object> scopedInput = new HashMap<>(input);
        scopedInput.put("path", userId + "/" + path);

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
     *
     * ReMe 搜索结果格式通常包含文件路径，如：
     * ```
     * [score=0.1234] daily/2025-07-12.md
     * 内容摘要...
     * ```
     *
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
            throw new RuntimeException("Failed to get clientWrapper from McpTool", e);
        }
    }
}
