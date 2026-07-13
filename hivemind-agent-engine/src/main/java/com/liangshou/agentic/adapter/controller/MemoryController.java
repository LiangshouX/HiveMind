package com.liangshou.agentic.adapter.controller;

import com.alibaba.fastjson2.JSONObject;
import com.liangshou.agentic.agents.memory.reme.TdAgentReMeService;
import com.liangshou.agentic.common.exceptions.BizException;
import com.liangshou.agentic.common.exceptions.HmeErrorCode;
import com.liangshou.agentic.application.dto.ReMeMemoryRequest;
import com.liangshou.agentic.common.utils.Result;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;

/**
 * 记忆管理 REST API 控制器。
 *
 * <p>提供用户记忆文件的浏览、读取、编辑和搜索功能。
 * 通过 McpReMeClient 代理 ReMe 的 MCP 工具实现。
 * 所有接口通过 JWT token 获取用户身份，无需手动传递 userId。</p>
 *
 * @author LiangshouX
 */
@RestController
@RequestMapping("/api/v1/memory")
public class MemoryController {

    private static final Logger log = LoggerFactory.getLogger(MemoryController.class);

    private final TdAgentReMeService remeService;

    public MemoryController(TdAgentReMeService remeService) {
        this.remeService = remeService;
    }

    /**
     * 浏览用户的记忆文件列表。
     *
     * <p>通过 MCP 工具获取用户目录下的文件列表，实现用户隔离。</p>
     *
     * @param principal 当前登录用户（JWT 解析）
     * @param path      目录路径（可选，默认为根目录）
     * @return 文件列表（相对于用户根目录的路径）
     */
    @GetMapping("/files")
    public Result<List<String>> listFiles(
            Principal principal,
            @RequestParam(defaultValue = "") String path) {
        String userId = currentUserId(principal);
        log.info("Listing memory files for user: {}, path: {}", userId, path);
        try {
            Map<String, Object> args = new HashMap<>();
            args.put("path", path);
            args.put("recursive", true);

            // 调用 MCP 工具
            String resultText = remeService.callMcpTool("list", args, userId);
            log.debug("MCP list result: {}", resultText);

            // 解析 JSON 响应，提取文件列表
            List<String> fileList = parseFileListFromJson(resultText, userId);

            log.info("Listed {} files for user: {}, path: {}", fileList.size(), userId, path);
            return Result.success(fileList);
        } catch (Exception e) {
            log.error("Failed to list memory files", e);
            return Result.error(HmeErrorCode.MEMORY_LIST_ERROR.getCode(), "Failed to list memory files: " + e.getMessage());
        }
    }

    /**
     * 从 MCP list 工具的 JSON 响应中解析文件列表。
     *
     * <p>MCP 返回格式：</p>
     * <pre>
     * {"items": ["user-001\daily\2026-07-12\文件.md", ...], "count": 2, "path": "user-001/"}
     * </pre>
     *
     * <p>解析后返回相对于用户根目录的文件名列表。</p>
     *
     * @param json   MCP 返回的 JSON 字符串
     * @param userId 用户 ID（用于去除路径前缀）
     * @return 文件名列表
     */
    private List<String> parseFileListFromJson(String json, String userId) {
        if (json == null || json.isBlank()) {
            return List.of();
        }

        try {
            JSONObject obj = JSONObject.parseObject(json);
            if (!obj.containsKey("items")) {
                return List.of();
            }

            var items = obj.getJSONArray("items");
            if (items == null) {
                return List.of();
            }

            String userPrefix = userId + "\\";
            String userPrefixUnix = userId + "/";

            return items.toJavaList(String.class).stream()
                    .map(item -> {
                        // 去除用户前缀（支持 Windows 和 Unix 路径分隔符）
                        if (item.startsWith(userPrefix)) {
                            return item.substring(userPrefix.length());
                        }
                        if (item.startsWith(userPrefixUnix)) {
                            return item.substring(userPrefixUnix.length());
                        }
                        return item;
                    })
                    // 统一使用 Unix 路径分隔符
                    .map(item -> item.replace("\\", "/"))
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to parse file list JSON: {}", json, e);
            return List.of();
        }
    }


    /**
     * 读取记忆文件内容。
     *
     * @param principal 当前登录用户（JWT 解析）
     * @param path      文件路径
     * @return 文件内容
     */
    @GetMapping("/files/read")
    public Result<String> readFile(
            Principal principal,
            @RequestParam String path) {
        String userId = currentUserId(principal);
        log.info("Reading memory file for user: {}, path: {}", userId, path);
        try {
            Map<String, Object> args = Map.of("path", path);
            String content = remeService.callMcpTool("read", args, userId);
            return Result.success(content);
        } catch (Exception e) {
            log.error("Failed to read memory file", e);
            return Result.error(HmeErrorCode.MEMORY_READ_ERROR.getCode(), "Failed to read memory file: " + e.getMessage());
        }
    }

    /**
     * 编辑记忆文件内容。
     *
     * <p>使用 write 工具直接覆盖文件内容，避免 edit 工具的精确匹配问题。
     * 先读取原文档的 frontmatter（description、metadata），再还原到新内容中。</p>
     *
     * @param principal 当前登录用户（JWT 解析）
     * @param request   编辑请求（newText 为完整的新内容，包括 frontmatter）
     * @return 是否编辑成功
     */
    @PutMapping("/files/edit")
    public Result<Boolean> editFile(
            Principal principal,
            @Valid @RequestBody ReMeMemoryRequest request) {
        String userId = currentUserId(principal);
        log.info("Editing memory file for user: {}, path: {}", userId, request.getPath());
        try {
            // 1. 读取原文档，提取 frontmatter 信息
            Map<String, Object> readArgs = Map.of("path", request.getPath());
            String originalContent = remeService.callMcpTool("read", readArgs, userId);

            // 2. 从原文档和新内容中解析 frontmatter
            Map<String, String> originalFrontmatter = parseFrontmatter(originalContent);
            Map<String, String> newFrontmatter = parseFrontmatter(request.getNewText());

            // 3. 构建 write 工具参数，保留 frontmatter 信息
            Map<String, Object> writeArgs = new HashMap<>();
            writeArgs.put("path", request.getPath());
            writeArgs.put("name", extractFileName(request.getPath()));

            // description 字段：优先使用新内容的，否则使用原文档的
            String description = newFrontmatter.getOrDefault("description",
                    originalFrontmatter.getOrDefault("description", ""));
            writeArgs.put("description", description);

            // metadata 字段：合并原文档和新内容的额外 frontmatter 字段
            Map<String, Object> metadata = new HashMap<>();
            // 先放入原文档的非标准字段
            originalFrontmatter.forEach((key, value) -> {
                if (!key.equals("name") && !key.equals("description")) {
                    metadata.put(key, value);
                }
            });
            // 再放入新内容的字段（覆盖原文档的同名字段）
            newFrontmatter.forEach((key, value) -> {
                if (!key.equals("name") && !key.equals("description")) {
                    metadata.put(key, value);
                }
            });
            if (!metadata.isEmpty()) {
                writeArgs.put("metadata", metadata);
            }

            // 4. 提取 body 内容（不含 frontmatter）
            String bodyContent = extractBody(request.getNewText());
            writeArgs.put("content", bodyContent);

            log.debug("Write args: path={}, name={}, description={}, metadata={}",
                    request.getPath(), extractFileName(request.getPath()), description, metadata);

            // 5. 调用 write 工具
            String result = remeService.callMcpTool("write", writeArgs, userId);
            log.info("Write result: {}", result);
            return Result.success(true);
        } catch (Exception e) {
            log.error("Failed to edit memory file", e);
            return Result.error(HmeErrorCode.MEMORY_EDIT_ERROR.getCode(), "Failed to edit memory file: " + e.getMessage());
        }
    }

    /**
     * 搜索记忆。
     *
     * @param principal 当前登录用户（JWT 解析）
     * @param query     搜索查询
     * @param limit     返回结果数量限制
     * @return 搜索结果
     */
    @GetMapping("/search")
    public Result<String> search(
            Principal principal,
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit) {
        String userId = currentUserId(principal);
        log.info("Searching memory for user: {}, query: {}", userId, query);
        try {
            String result = remeService.retrieve(userId, query);
            return Result.success(result);
        } catch (Exception e) {
            log.error("Failed to search memory", e);
            return Result.error(HmeErrorCode.MEMORY_SEARCH_ERROR.getCode(), "Failed to search memory: " + e.getMessage());
        }
    }

    /**
     * 获取 ReMe 服务状态。
     *
     * @return 服务状态信息
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("useMcp", remeService.isUseMcp());
        status.put("enabled", true);
        return Result.success(status);
    }

    /**
     * 获取当前用户 ID。
     */
    private String currentUserId(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new BizException(HmeErrorCode.AGENT_USER_NOT_FOUND);
        }
        return principal.getName();
    }

    /**
     * 从文件路径中提取文件名。
     *
     * @param path 文件路径，如 "daily/2026-07-12/笔记.md"
     * @return 文件名，如 "笔记.md"
     */
    private String extractFileName(String path) {
        if (path == null || path.isBlank()) {
            return "untitled.md";
        }
        // 处理 Unix 和 Windows 路径分隔符
        String normalized = path.replace("\\", "/");
        int lastSlash = normalized.lastIndexOf('/');
        return lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
    }

    /**
     * 解析 Markdown 文件的 frontmatter。
     *
     * @param content Markdown 内容
     * @return frontmatter 键值对，如果没有 frontmatter 则返回空 Map
     */
    private Map<String, String> parseFrontmatter(String content) {
        Map<String, String> result = new HashMap<>();
        if (content == null || content.isBlank()) {
            return result;
        }

        String trimmed = content.stripLeading();
        if (!trimmed.startsWith("---")) {
            return result;
        }

        // 找到第二个 ---
        int endIndex = trimmed.indexOf("---", 3);
        if (endIndex < 0) {
            return result;
        }

        // 解析 frontmatter 内容
        String frontmatterStr = trimmed.substring(3, endIndex).trim();
        for (String line : frontmatterStr.split("\n")) {
            int colonIndex = line.indexOf(':');
            if (colonIndex > 0) {
                String key = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();
                result.put(key, value);
            }
        }

        return result;
    }

    /**
     * 提取 Markdown 文件的 body 内容（不含 frontmatter）。
     *
     * @param content 完整的 Markdown 内容
     * @return 不含 frontmatter 的 body 内容
     */
    private String extractBody(String content) {
        if (content == null || content.isBlank()) {
            return content;
        }

        String trimmed = content.stripLeading();
        if (!trimmed.startsWith("---")) {
            return content;
        }

        // 找到第二个 ---
        int endIndex = trimmed.indexOf("---", 3);
        if (endIndex < 0) {
            return content;
        }

        // 返回 --- 之后的内容
        return trimmed.substring(endIndex + 3).stripLeading();
    }

}
