package com.liangshou.agentic.adapter.controller;

import com.liangshou.agentic.application.ITdAgentProfileService;
import com.liangshou.agentic.application.dto.BatchUpdateRequest;
import com.liangshou.agentic.application.dto.ProfileListResponse;
import com.liangshou.agentic.application.dto.ProfileUpdateRequest;
import com.liangshou.agentic.domain.profile.model.AgentProfileDocument;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Agent Profile 管理 REST API 控制器，提供用户 Profile 配置的查询、更新和重置接口。
 *
 * <p>提供的 API 端点包括：</p>
 * <ul>
 *   <li>GET /api/v1/tdagent/profiles - 获取用户的 Profile 列表</li>
 *   <li>GET /api/v1/tdagent/profiles/{filename} - 获取单个 Profile 文件</li>
 *   <li>PUT /api/v1/tdagent/profiles/{filename} - 更新单个 Profile 文件</li>
 *   <li>PUT /api/v1/tdagent/profiles/batch - 批量更新 Profile 文件</li>
 *   <li>POST /api/v1/tdagent/profiles/{filename}/reset - 重置 Profile 为默认值</li>
 *   <li>GET /api/v1/tdagent/profiles/{filename}/download - 下载 Profile 文件</li>
 *   <li>POST /api/v1/tdagent/profiles/{filename}/upload - 上传 Profile 文件</li>
 * </ul>
 *
 * <p>所有接口都需要用户认证，通过 JWT token 获取用户身份。</p>
 *
 * @author LiangshouX
 */
@RestController
@RequestMapping("/api/v1/tdagent/profiles")
@SuppressWarnings("unused")
public class TdAgentProfileController {

    private static final Logger log = LoggerFactory.getLogger(TdAgentProfileController.class);

    private final ITdAgentProfileService profileService;

    /**
     * 构造器
     *
     * @param profileService Profile 服务
     */
    public TdAgentProfileController(ITdAgentProfileService profileService) {
        this.profileService = profileService;
    }

    /**
     * 获取用户的 Profile 列表。
     *
     * @param principal 当前用户
     * @return Profile 列表
     */
    @GetMapping
    public ResponseEntity<List<ProfileListResponse>> listProfiles(Principal principal) {
        String userId = currentUserId(principal);
        log.debug("[Profile 列表] 获取用户 Profile 列表 - userId: {}", userId);

        List<AgentProfileDocument> profiles = profileService.loadUserProfiles(userId);

        List<ProfileListResponse> response = profiles.stream()
                .map(this::toProfileListResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * 获取单个 Profile 文件。
     *
     * @param principal 当前用户
     * @param filename  文件名
     * @return Profile 文件
     */
    @GetMapping("/{filename}")
    public ResponseEntity<ProfileListResponse> getProfile(
            Principal principal,
            @PathVariable String filename) {
        String userId = currentUserId(principal);
        log.debug("[Profile 获取] 获取单个 Profile - userId: {}, filename: {}", userId, filename);

        return profileService.getProfile(userId, filename)
                .map(profile -> ResponseEntity.ok(toProfileListResponse(profile)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 更新单个 Profile 文件。
     *
     * @param principal 当前用户
     * @param filename  文件名
     * @param request   更新请求
     * @return 更新结果
     */
    @PutMapping("/{filename}")
    public ResponseEntity<ProfileListResponse> updateProfile(
            Principal principal,
            @PathVariable String filename,
            @Valid @RequestBody ProfileUpdateRequest request) {
        String userId = currentUserId(principal);

        // 确保路径参数和请求体中的 filename 一致
        if (!filename.equals(request.getFilename())) {
            log.warn("[Profile 更新] 路径参数和请求体中的 filename 不一致 - path: {}, body: {}",
                    filename, request.getFilename());
            return ResponseEntity.badRequest().build();
        }

        log.info("[Profile 更新] 更新 Profile - userId: {}, filename: {}", userId, filename);

        try {
            AgentProfileDocument updated = profileService.updateProfile(
                    userId, filename, request.getContent(), request.isEnabled());
            return ResponseEntity.ok(toProfileListResponse(updated));
        } catch (IllegalArgumentException e) {
            log.warn("[Profile 更新] 参数校验失败 - userId: {}, filename: {}, error: {}",
                    userId, filename, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 批量更新 Profile 文件。
     *
     * @param principal 当前用户
     * @param request   批量更新请求
     * @return 更新结果
     */
    @PutMapping("/batch")
    public ResponseEntity<?> batchUpdateProfiles(
            Principal principal,
            @Valid @RequestBody BatchUpdateRequest request) {
        String userId = currentUserId(principal);
        log.info("[Profile 批量更新] 批量更新 Profile - userId: {}, 数量: {}", userId, request.getProfiles().size());

        try {
            List<String> filenames = request.getProfiles().stream()
                    .map(ProfileUpdateRequest::getFilename)
                    .collect(Collectors.toList());
            List<String> contents = request.getProfiles().stream()
                    .map(ProfileUpdateRequest::getContent)
                    .collect(Collectors.toList());
            List<Boolean> enabled = request.getProfiles().stream()
                    .map(ProfileUpdateRequest::isEnabled)
                    .collect(Collectors.toList());

            int updatedCount = profileService.batchUpdateProfiles(userId, filenames, contents, enabled);

            return ResponseEntity.ok(java.util.Map.of(
                    "updatedCount", updatedCount,
                    "updatedAt", Instant.now().toString()
            ));
        } catch (Exception e) {
            log.error("[Profile 批量更新] 批量更新失败 - userId: {}, error: {}", userId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    /**
     * 重置 Profile 文件为默认值。
     *
     * @param principal 当前用户
     * @param filename  文件名
     * @return 重置结果
     */
    @PostMapping("/{filename}/reset")
    public ResponseEntity<ProfileListResponse> resetProfile(
            Principal principal,
            @PathVariable String filename) {
        String userId = currentUserId(principal);
        log.info("[Profile 重置] 重置 Profile 为默认值 - userId: {}, filename: {}", userId, filename);

        try {
            AgentProfileDocument reset = profileService.resetProfile(userId, filename);
            return ResponseEntity.ok(toProfileListResponse(reset));
        } catch (IllegalArgumentException e) {
            log.warn("[Profile 重置] 默认文件不存在 - userId: {}, filename: {}, error: {}",
                    userId, filename, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 下载 Profile 文件。
     *
     * @param principal 当前用户
     * @param filename  文件名
     * @return 文件内容（text/markdown 格式）
     */
    @GetMapping("/{filename}/download")
    public ResponseEntity<String> downloadProfile(
            Principal principal,
            @PathVariable String filename) {
        String userId = currentUserId(principal);
        log.debug("[Profile 下载] 下载 Profile 文件 - userId: {}, filename: {}", userId, filename);

        return profileService.getProfile(userId, filename)
                .map(profile -> {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.parseMediaType("text/markdown"));
                    headers.setContentDispositionFormData("attachment", filename);
                    return ResponseEntity.ok()
                            .headers(headers)
                            .body(profile.getContent());
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 上传 Profile 文件。
     *
     * @param principal 当前用户
     * @param filename  文件名
     * @param file      上传的文件
     * @return 上传结果
     */
    @PostMapping("/{filename}/upload")
    public ResponseEntity<ProfileListResponse> uploadProfile(
            Principal principal,
            @PathVariable String filename,
            @RequestParam("file") MultipartFile file) {
        String userId = currentUserId(principal);
        log.info("[Profile 上传] 上传 Profile 文件 - userId: {}, filename: {}", userId, filename);

        try {
            String content = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
            AgentProfileDocument updated = profileService.updateProfile(userId, filename, content, true);
            return ResponseEntity.ok(toProfileListResponse(updated));
        } catch (Exception e) {
            log.error("[Profile 上传] 上传失败 - userId: {}, filename: {}, error: {}",
                    userId, filename, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
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

    /**
     * 将 AgentProfileDocument 转换为 ProfileListResponse。
     */
    private ProfileListResponse toProfileListResponse(AgentProfileDocument document) {
        String content = document.getContent() != null ? document.getContent() : "";
        String size = formatSize(content.length());

        return ProfileListResponse.builder()
                .filename(document.getFilename())
                .content(content)
                .enabled(document.isEnabled())
                .source(document.getSource() != null ? document.getSource().name() : "DEFAULT")
                .size(size)
                .updatedAt(document.getUpdatedAt() != null ? document.getUpdatedAt() : Instant.now())
                .build();
    }

    /**
     * 格式化文件大小。
     */
    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return new DecimalFormat("#.#").format(bytes / 1024.0) + " KB";
        } else {
            return new DecimalFormat("#.#").format(bytes / (1024.0 * 1024.0)) + " MB";
        }
    }
}
