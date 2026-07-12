package com.liangshou.agentic.adapter.controller;

import com.alibaba.fastjson2.JSONObject;
import com.liangshou.agentic.agents.memory.reme.TdAgentReMeService;
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
            return Result.error(500, "Failed to list memory files: " + e.getMessage());
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
            return Result.error(500, "Failed to read memory file: " + e.getMessage());
        }
    }

    /**
     * 编辑记忆文件内容。
     *
     * @param principal 当前登录用户（JWT 解析）
     * @param request   编辑请求
     * @return 是否编辑成功
     */
    @PutMapping("/files/edit")
    public Result<Boolean> editFile(
            Principal principal,
            @Valid @RequestBody ReMeMemoryRequest request) {
        String userId = currentUserId(principal);
        log.info("Editing memory file for user: {}, path: {}", userId, request.getPath());
        try {
            Map<String, Object> args = Map.of(
                    "path", request.getPath(),
                    "old", request.getOldText(),
                    "new", request.getNewText()
            );
            remeService.callMcpTool("edit", args, userId);
            return Result.success(true);
        } catch (Exception e) {
            log.error("Failed to edit memory file", e);
            return Result.error(500, "Failed to edit memory file: " + e.getMessage());
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
            return Result.error(500, "Failed to search memory: " + e.getMessage());
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
            throw new IllegalArgumentException("未获取到当前登录用户");
        }
        return principal.getName();
    }
}
