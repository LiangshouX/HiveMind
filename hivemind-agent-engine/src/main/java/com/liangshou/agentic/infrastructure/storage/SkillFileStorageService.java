package com.liangshou.agentic.infrastructure.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Skill 文件存储服务
 * 负责 Skill 文件的打包（tar.gz）、上传、下载和版本管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "tdagent.skill.storage.oss.enabled", havingValue = "true")
public class SkillFileStorageService {

    private final ObjectStorageService storageService;
    private final OssProperties ossProperties;

    /**
     * 上传 Skill 版本文件（打包为 tar.gz）
     *
     * @param userId    用户 ID
     * @param skillId   Skill ID
     * @param version   版本号 (如 v1.0.0)
     * @param skillMarkdown SKILL.md 内容
     * @param resources 资源文件映射 (相对路径 -> 内容)
     * @return OSS 对象键
     */
    public String uploadSkillVersion(String userId, String skillId, String version,
                                     String skillMarkdown, Map<String, String> resources) {
        String objectKey = buildVersionPath(userId, skillId, version);
        try {
            byte[] tarGzBytes = createTarGz(skillMarkdown, resources);
            storageService.upload(
                    objectKey,
                    new ByteArrayInputStream(tarGzBytes),
                    tarGzBytes.length,
                    "application/gzip"
            );
            log.info("Skill 版本上传成功: userId={}, skillId={}, version={}, size={} bytes",
                    userId, skillId, version, tarGzBytes.length);
            return objectKey;
        } catch (Exception e) {
            log.error("Skill 版本上传失败: userId={}, skillId={}, version={}", userId, skillId, version, e);
            throw new RuntimeException("Skill 版本上传失败", e);
        }
    }

    /**
     * 下载并解压 Skill 版本文件
     *
     * @param userId  用户 ID
     * @param skillId Skill ID
     * @param version 版本号
     * @return Skill 文件内容（SKILL.md + 资源文件）
     */
    public SkillVersionContent downloadSkillVersion(String userId, String skillId, String version) {
        String objectKey = buildVersionPath(userId, skillId, version);
        if (!storageService.exists(objectKey)) {
            throw new RuntimeException("Skill 版本不存在: " + objectKey);
        }
        try (var inputStream = storageService.download(objectKey)) {
            return extractTarGz(inputStream);
        } catch (IOException e) {
            log.error("Skill 版本下载/解压失败: {}", objectKey, e);
            throw new RuntimeException("Skill 版本下载/解压失败", e);
        }
    }

    /**
     * 生成预签名下载 URL
     *
     * @param userId  用户 ID
     * @param skillId Skill ID
     * @param version 版本号
     * @return 预签名 URL
     */
    public URL generateDownloadUrl(String userId, String skillId, String version) {
        String objectKey = buildVersionPath(userId, skillId, version);
        return storageService.generatePresignedDownloadUrl(objectKey, ossProperties.getDefaultExpireMinutes());
    }

    /**
     * 删除 Skill 版本文件
     *
     * @param userId  用户 ID
     * @param skillId Skill ID
     * @param version 版本号
     */
    public void deleteSkillVersion(String userId, String skillId, String version) {
        String objectKey = buildVersionPath(userId, skillId, version);
        storageService.delete(objectKey);
        log.info("Skill 版本删除成功: userId={}, skillId={}, version={}", userId, skillId, version);
    }

    /**
     * 检查 Skill 版本是否存在
     *
     * @param userId  用户 ID
     * @param skillId Skill ID
     * @param version 版本号
     * @return 是否存在
     */
    public boolean versionExists(String userId, String skillId, String version) {
        String objectKey = buildVersionPath(userId, skillId, version);
        return storageService.exists(objectKey);
    }

    /**
     * 构建版本路径
     * 格式: {userId}/{skillId}/{version}/skill.tar.gz
     */
    private String buildVersionPath(String userId, String skillId, String version) {
        return userId + "/" + skillId + "/" + version + "/skill.tar.gz";
    }

    /**
     * 创建 tar.gz 打包文件
     *
     * @param skillMarkdown SKILL.md 内容
     * @param resources     资源文件
     * @return tar.gz 字节数组
     */
    private byte[] createTarGz(String skillMarkdown, Map<String, String> resources) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GzipCompressorOutputStream gzos = new GzipCompressorOutputStream(baos);
             TarArchiveOutputStream taos = new TarArchiveOutputStream(gzos)) {

            // 设置 longFileMode 以支持长文件名
            taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);

            // 添加 SKILL.md
            addTarEntry(taos, "SKILL.md", skillMarkdown.getBytes(StandardCharsets.UTF_8));

            // 添加资源文件
            if (resources != null) {
                for (Map.Entry<String, String> entry : resources.entrySet()) {
                    String path = entry.getKey();
                    String content = entry.getValue();
                    addTarEntry(taos, path, content.getBytes(StandardCharsets.UTF_8));
                }
            }

            taos.finish();
        }
        return baos.toByteArray();
    }

    /**
     * 添加 tar 条目
     */
    private void addTarEntry(TarArchiveOutputStream taos, String name, byte[] content) throws IOException {
        TarArchiveEntry entry = new TarArchiveEntry(name);
        entry.setSize(content.length);
        taos.putArchiveEntry(entry);
        taos.write(content);
        taos.closeArchiveEntry();
    }

    /**
     * 解压 tar.gz 文件
     *
     * @param inputStream 输入流
     * @return Skill 版本内容
     */
    private SkillVersionContent extractTarGz(java.io.InputStream inputStream) throws IOException {
        String skillMarkdown = null;
        Map<String, String> resources = new HashMap<>();

        try (GzipCompressorInputStream gzis = new GzipCompressorInputStream(inputStream);
             TarArchiveInputStream tais = new TarArchiveInputStream(gzis)) {

            TarArchiveEntry entry;
            while ((entry = (TarArchiveEntry) tais.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                String name = entry.getName();
                byte[] content = tais.readAllBytes();

                if ("SKILL.md".equals(name)) {
                    skillMarkdown = new String(content, StandardCharsets.UTF_8);
                } else {
                    resources.put(name, new String(content, StandardCharsets.UTF_8));
                }
            }
        }

        if (skillMarkdown == null) {
            throw new RuntimeException("Skill 包缺少 SKILL.md 文件");
        }

        return new SkillVersionContent(skillMarkdown, resources);
    }

    /**
     * Skill 版本内容封装
     */
    public record SkillVersionContent(String skillMarkdown, Map<String, String> resources) {
    }
}
