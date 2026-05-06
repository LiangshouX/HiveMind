package com.liangshou.tangdynasty.agentic.infrastructure.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OSS 配置属性类
 * 支持通过 application.yml 配置中心注入
 */
@Data
@Component
@ConfigurationProperties(prefix = "tdagent.skill.storage.oss")
public class OssProperties {

    /** 是否启用 OSS 存储 */
    private boolean enabled = false;

    /** OSS 端点（内网端点优先） */
    private String endpoint = "https://oss-cn-hangzhou.aliyuncs.com";

    /** AccessKey ID */
    private String accessKeyId;

    /** AccessKey Secret */
    private String accessKeySecret;

    /** Bucket 名称 */
    private String bucketName = "agent-skills";

    /** 根路径前缀 */
    private String basePath = "agent-skills";

    /** 预签名 URL 默认过期时间（分钟） */
    private int defaultExpireMinutes = 30;

    /** STS 端点 */
    private String stsEndpoint = "sts.cn-hangzhou.aliyuncs.com";

    /** STS 角色 ARN（用于前端直传） */
    private String stsRoleArn;

    /** CDN 域名（可选，用于加速下载） */
    private String cdnDomain;
}
