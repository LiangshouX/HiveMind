package com.liangshou.agentic.adapter.controller;

import com.liangshou.agentic.application.dto.ReMeMemoryRequest;
import com.liangshou.agentic.agents.memory.reme.TdAgentReMeService;
import com.liangshou.agentic.common.utils.Result;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 记忆管理 REST API 控制器。
 *
 * <p>提供用户记忆文件的浏览、读取、编辑和搜索功能。
 * 通过 McpReMeClient 代理 ReMe 的 MCP 工具实现。</p>
 *
 * @author LiangshouX
 */
@RestController
@RequestMapping("/api/memory")
@CrossOrigin(origins = "*")
public class MemoryController {

    private static final Logger log = LoggerFactory.getLogger(MemoryController.class);

    private final TdAgentReMeService remeService;

    public MemoryController(TdAgentReMeService remeService) {
        this.remeService = remeService;
    }

    /**
     * 浏览用户的记忆文件列表。
     *
     * @param userId 用户 ID
     * @param path   目录路径（可选，默认为根目录）
     * @return 文件列表
     */
    @GetMapping("/files")
    public Result<List<String>> listFiles(
            @RequestParam String userId,
            @RequestParam(defaultValue = "") String path) {
        log.info("Listing memory files for user: {}, path: {}", userId, path);
        try {
            Map<String, Object> args = new HashMap<>();
            args.put("path", path);
            args.put("recursive", false);
            String result = remeService.callMcpTool("list", args, userId);
            return Result.success(parseFileList(result));
        } catch (Exception e) {
            log.error("Failed to list memory files", e);
            return Result.error(500, "Failed to list memory files: " + e.getMessage());
        }
    }

    /**
     * 读取记忆文件内容。
     *
     * @param userId 用户 ID
     * @param path   文件路径
     * @return 文件内容
     */
    @GetMapping("/files/read")
    public Result<String> readFile(
            @RequestParam String userId,
            @RequestParam String path) {
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
     * @param userId  用户 ID
     * @param request 编辑请求
     * @return 是否编辑成功
     */
    @PutMapping("/files/edit")
    public Result<Boolean> editFile(
            @RequestParam String userId,
            @Valid @RequestBody ReMeMemoryRequest request) {
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
     * @param userId 用户 ID
     * @param query  搜索查询
     * @param limit  返回结果数量限制
     * @return 搜索结果
     */
    @GetMapping("/search")
    public Result<String> search(
            @RequestParam String userId,
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit) {
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
     * 解析文件列表字符串为列表。
     *
     * @param fileListStr 文件列表字符串
     * @return 文件列表
     */
    private List<String> parseFileList(String fileListStr) {
        if (fileListStr == null || fileListStr.isBlank()) {
            return List.of();
        }
        return Arrays.stream(fileListStr.split("\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
