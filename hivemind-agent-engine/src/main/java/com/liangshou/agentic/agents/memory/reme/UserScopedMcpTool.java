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
import java.util.regex.Pattern;

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

    /** 路径遍历检测：匹配 .. 段（独立的 .. 或以 /.. 或 ../ 开头/结尾） */
    private static final Pattern PATH_TRAVERSAL = Pattern.compile("(^|[/\\\\])\\.\\.($|[/\\\\])");

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
     * <p>使用传入的 clientWrapper 而非反射获取，避免依赖 SDK 内部实现细节。</p>
     *
     * @param original     原始 MCP 工具
     * @param userId       用户 ID，用作 workspace 隔离前缀
     * @param clientWrapper MCP 客户端包装器，从原始工具所在的 McpReMeClient 传入
     */
    public UserScopedMcpTool(McpTool original, String userId, McpClientWrapper clientWrapper) {
        super(
                original.getName(),
                original.getDescription(),
                original.getParameters(),
                clientWrapper,
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
     *
     * @throws BizException 如果路径包含遍历攻击（.. 段）
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

        // 安全检查：拒绝路径遍历攻击
        if (PATH_TRAVERSAL.matcher(normalizedPath).find()) {
            log.warn("[UserScoped] Path traversal rejected: userId={}, path='{}'", userId, path);
            throw new BizException(HmeErrorCode.MCP_TOOL_CALL_ERROR,
                    "Path traversal not allowed: " + path);
        }

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
     *
     * <p>ReMe 搜索结果以 {@code ============} 分隔各个结果块，每个块的首行
     * 包含文件路径（如 {@code daily/2026-07-12/session-a.md:12-28 [score=0.0317]}）。
     * 本方法按块解析，仅保留路径以 userPrefix 开头的结果块。</p>
     *
     * <p>对于非标准格式（不含分隔符的纯文本），回退为逐行匹配 userPrefix。</p>
     */
    private String filterTextByUser(String text, String userPrefix) {
        if (text == null || text.isBlank()) {
            return "";
        }

        // ReMe 搜索结果以 "=" 分隔符划分结果块
        if (text.contains("==========")) {
            return filterByResultBlocks(text, userPrefix);
        }

        // 回退：逐行匹配（适用于非标准格式或简短结果）
        return filterByLineMatch(text, userPrefix);
    }

    /**
     * 按 ReMe 搜索结果的 "=" 分隔符分块过滤。
     *
     * <p>每个结果块形如：
     * <pre>
     * ========== path/to/file.md:12-28 [score=0.0317] ==========
     * ... 正文内容 ...
     *   outlinks (2):
     *     -> ...
     * </pre>
     * 仅保留首行路径以 userPrefix 开头的块。</p>
     */
    private String filterByResultBlocks(String text, String userPrefix) {
        String[] blocks = text.split("(?=^={10,})", -1);
        StringBuilder result = new StringBuilder();

        for (String block : blocks) {
            if (block.isBlank()) {
                continue;
            }
            // 提取分隔符行中的路径部分
            String headerLine = block.lines().findFirst().orElse("");
            if (headerLine.contains("==========") && headerLine.contains(userPrefix)) {
                result.append(block);
                if (!block.endsWith("\n")) {
                    result.append("\n");
                }
            }
        }

        return result.toString().trim();
    }

    /**
     * 逐行匹配过滤（回退方案）。
     *
     * <p>识别包含 userPrefix 的行作为"用户块"的开始，持续收集后续非空行，
     * 直到遇到下一个包含路径模式的行或文件结束。</p>
     */
    private String filterByLineMatch(String text, String userPrefix) {
        String[] lines = text.split("\n");
        StringBuilder result = new StringBuilder();
        boolean inUserBlock = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            if (line.contains(userPrefix)) {
                // 命中用户路径：开始新块
                inUserBlock = true;
                result.append(line).append("\n");
            } else if (inUserBlock) {
                // 当前块的延续行
                // 如果遇到看起来是新结果块开始的行（包含路径模式），结束当前块
                if (isLikelyResultHeader(line)) {
                    inUserBlock = false;
                } else {
                    result.append(line).append("\n");
                }
            }
        }

        return result.toString().trim();
    }

    /**
     * 判断一行是否看起来像新的搜索结果块的标题行。
     *
     * <p>特征：以 "=" 开头，或包含 ".md" 路径后缀 + 分数模式。</p>
     */
    private boolean isLikelyResultHeader(String line) {
        if (line.startsWith("==========")) {
            return true;
        }
        // 匹配类似 "path/file.md:12-28 [score=0.0317]" 的模式
        return line.contains(".md") && line.contains("[score=");
    }
}
