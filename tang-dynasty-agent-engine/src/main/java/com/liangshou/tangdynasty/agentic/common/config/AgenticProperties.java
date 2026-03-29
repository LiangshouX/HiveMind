package com.liangshou.tangdynasty.agentic.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 读取 agentic 配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "agentic")
public class AgenticProperties {

    private int maxIters;
}
