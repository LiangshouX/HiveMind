package com.liangshou.tangdynasty.agentic.infrastructure.storage;

import java.io.InputStream;
import java.net.URL;
import java.util.Map;

/**
 * 对象存储服务接口（抽象层，支持阿里云 OSS/MinIO/S3 等后端）
 */
public interface ObjectStorageService {

    /**
     * 上传文件
     * @param objectKey 对象键（路径）
     * @param inputStream 输入流
     * @param contentLength 内容长度
     * @param contentType 内容类型
     */
    void upload(String objectKey, InputStream inputStream, long contentLength, String contentType);

    /**
     * 上传文件（带自定义元数据）
     * @param objectKey 对象键
     * @param inputStream 输入流
     * @param contentLength 内容长度
     * @param contentType 内容类型
     * @param userMetadata 用户自定义元数据
     */
    void upload(String objectKey, InputStream inputStream, long contentLength, String contentType, Map<String, String> userMetadata);

    /**
     * 下载文件
     * @param objectKey 对象键
     * @return 输入流
     */
    InputStream download(String objectKey);

    /**
     * 删除文件
     * @param objectKey 对象键
     */
    void delete(String objectKey);

    /**
     * 检查文件是否存在
     * @param objectKey 对象键
     * @return 是否存在
     */
    boolean exists(String objectKey);

    /**
     * 生成预签名下载 URL
     * @param objectKey 对象键
     * @param expireMinutes 过期时间（分钟）
     * @return 预签名 URL
     */
    URL generatePresignedDownloadUrl(String objectKey, int expireMinutes);

    /**
     * 生成预签名上传 URL
     * @param objectKey 对象键
     * @param expireMinutes 过期时间（分钟）
     * @return 预签名 URL
     */
    URL generatePresignedUploadUrl(String objectKey, int expireMinutes);
}
