package com.liangshou.agentic.adapter.controller;

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
     * <p>直接从文件系统读取用户目录下的文件列表，实现用户隔离。
     * ReMe 的 workspace 目录结构：.reme/{userId}/{path}</p>
     *
     * @param principal 当前登录用户（JWT 解析）
     * @param path      目录路径（可选，默认为根目录）
     * @return 文件列表
     */
    @GetMapping("/files")
    public Result<List<String>> listFiles(
            Principal principal,
            @RequestParam(defaultValue = "") String path) {
        String userId = currentUserId(principal);
        log.info("Listing memory files for user: {}, path: {}", userId, path);
        try {
            // 构建用户隔离的目录路径
            String userDir = userId + "/" + (path != null ? path : "");
            if (userDir.endsWith("/")) {
                userDir = userDir.substring(0, userDir.length() - 1);
            }

            // 直接从文件系统读取
            List<String> fileList = listFilesFromDirectory(userDir);

            log.info("Listed {} files for user: {}, path: {}", fileList.size(), userId, path);
            return Result.success(fileList);
        } catch (Exception e) {
            log.error("Failed to list memory files", e);
            return Result.error(500, "Failed to list memory files: " + e.getMessage());
        }
    }

    /**
     * 从文件系统读取目录下的文件和子目录列表。
     *
     * @param relativePath 相对于 ReMe workspace 的路径
     * @return 文件和子目录名称列表
     */
    private List<String> listFilesFromDirectory(String relativePath) {
        // ReMe workspace 目录
        java.nio.file.Path workspacePath = java.nio.file.Path.of(".reme").toAbsolutePath().normalize();
        java.nio.file.Path targetPath = workspacePath.resolve(relativePath).normalize();

        // 安全检查：确保路径在 workspace 内
        if (!targetPath.startsWith(workspacePath)) {
            log.warn("Path traversal attempt detected: {}", relativePath);
            return List.of();
        }

        // 目录不存在则返回空列表
        if (!java.nio.file.Files.exists(targetPath) || !java.nio.file.Files.isDirectory(targetPath)) {
            log.debug("Directory does not exist: {}", targetPath);
            return List.of();
        }

        try (var stream = java.nio.file.Files.list(targetPath)) {
            return stream
                    .map(java.nio.file.Path::getFileName)
                    .map(java.nio.file.Path::toString)
                    .sorted()
                    .toList();
        } catch (java.io.IOException e) {
            log.error("Failed to list directory: {}", targetPath, e);
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
