package com.liangshou.agentic.infrastructure.storage;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.ObjectMetadata;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.Map;

/**
 * 阿里云 OSS 存储服务实现
 * 生产环境建议使用 RAM 子账号 + VPC 内网端点
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "tdagent.skill.storage.oss.enabled", havingValue = "true")
public class AliyunOssStorageService implements ObjectStorageService {

    private final OssProperties ossProperties;
    private OSS ossClient;

    @PostConstruct
    public void init() {
        if (ossProperties.getAccessKeyId() == null || ossProperties.getAccessKeySecret() == null) {
            throw new IllegalStateException("OSS AccessKey ID 和 Secret 必须配置");
        }
        this.ossClient = new OSSClientBuilder().build(
                ossProperties.getEndpoint(),
                ossProperties.getAccessKeyId(),
                ossProperties.getAccessKeySecret()
        );
        log.info("阿里云 OSS 客户端初始化成功: endpoint={}, bucket={}",
                ossProperties.getEndpoint(), ossProperties.getBucketName());
    }

    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
            log.info("阿里云 OSS 客户端已关闭");
        }
    }

    @Override
    public void upload(String objectKey, InputStream inputStream, long contentLength, String contentType) {
        upload(objectKey, inputStream, contentLength, contentType, null);
    }

    @Override
    public void upload(String objectKey, InputStream inputStream, long contentLength, String contentType,
                       Map<String, String> userMetadata) {
        String fullKey = buildFullPath(objectKey);
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(contentLength);
            if (contentType != null) {
                metadata.setContentType(contentType);
            }
            if (userMetadata != null && !userMetadata.isEmpty()) {
                metadata.setUserMetadata(userMetadata);
            }
            ossClient.putObject(ossProperties.getBucketName(), fullKey, inputStream, metadata);
            log.debug("文件上传成功: {}", fullKey);
        } catch (Exception e) {
            log.error("文件上传失败: {}", fullKey, e);
            throw new RuntimeException("文件上传失败: " + fullKey, e);
        }
    }

    @Override
    public InputStream download(String objectKey) {
        String fullKey = buildFullPath(objectKey);
        try {
            return ossClient.getObject(ossProperties.getBucketName(), fullKey).getObjectContent();
        } catch (Exception e) {
            log.error("文件下载失败: {}", fullKey, e);
            throw new RuntimeException("文件下载失败: " + fullKey, e);
        }
    }

    @Override
    public void delete(String objectKey) {
        String fullKey = buildFullPath(objectKey);
        try {
            ossClient.deleteObject(ossProperties.getBucketName(), fullKey);
            log.debug("文件删除成功: {}", fullKey);
        } catch (Exception e) {
            log.error("文件删除失败: {}", fullKey, e);
            throw new RuntimeException("文件删除失败: " + fullKey, e);
        }
    }

    @Override
    public boolean exists(String objectKey) {
        String fullKey = buildFullPath(objectKey);
        try {
            return ossClient.doesObjectExist(ossProperties.getBucketName(), fullKey);
        } catch (Exception e) {
            log.error("检查文件存在失败: {}", fullKey, e);
            throw new RuntimeException("检查文件存在失败: " + fullKey, e);
        }
    }

    @Override
    public URL generatePresignedDownloadUrl(String objectKey, int expireMinutes) {
        String fullKey = buildFullPath(objectKey);
        try {
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                    ossProperties.getBucketName(), fullKey, HttpMethod.GET);
            request.setExpiration(new Date(System.currentTimeMillis() + expireMinutes * 60 * 1000L));

            // 如果配置了 CDN 域名，可以在生成 URL 后进行替换
            URL url = ossClient.generatePresignedUrl(request);

            // 如果配置了 CDN 域名，替换为 CDN 域名
            if (ossProperties.getCdnDomain() != null && !ossProperties.getCdnDomain().isBlank()) {
                String cdnUrl = url.toString();
                String bucketEndpoint = ossProperties.getEndpoint().contains("://")
                        ? ossProperties.getEndpoint().substring(ossProperties.getEndpoint().indexOf("://") + 3)
                        : ossProperties.getEndpoint();
                cdnUrl = cdnUrl.replace(
                        ossProperties.getBucketName() + "." + bucketEndpoint,
                        ossProperties.getCdnDomain());
                try {
                    return new URL(cdnUrl);
                } catch (Exception e) {
                    log.warn("CDN URL 替换失败，返回原始 URL: {}", cdnUrl, e);
                }
            }

            return url;
        } catch (Exception e) {
            log.error("生成预签名下载 URL 失败: {}", objectKey, e);
            throw new RuntimeException("生成预签名下载 URL 失败: " + objectKey, e);
        }
    }

    @Override
    public URL generatePresignedUploadUrl(String objectKey, int expireMinutes) {
        String fullKey = buildFullPath(objectKey);
        try {
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                    ossProperties.getBucketName(), fullKey, HttpMethod.PUT);
            request.setExpiration(new Date(System.currentTimeMillis() + expireMinutes * 60 * 1000L));
            return ossClient.generatePresignedUrl(request);
        } catch (Exception e) {
            log.error("生成预签名上传 URL 失败: {}", objectKey, e);
            throw new RuntimeException("生成预签名上传 URL 失败: " + objectKey, e);
        }
    }

    /**
     * 构建完整的对象键路径
     * 格式：{basePath}/{objectKey}
     */
    private String buildFullPath(String objectKey) {
        String basePath = ossProperties.getBasePath();
        if (basePath.endsWith("/")) {
            basePath = basePath.substring(0, basePath.length() - 1);
        }
        if (objectKey.startsWith("/")) {
            objectKey = objectKey.substring(1);
        }
        return basePath + "/" + objectKey;
    }
}
