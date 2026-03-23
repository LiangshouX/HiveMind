package com.liangshou.tangdynasty.agentic.utils;

import com.liangshou.tangdynasty.agentic.common.exceptions.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

import static com.liangshou.tangdynasty.agentic.common.enums.ErrorCodeEnum.SOUL_LOADER_ERROR;

/**
 * SOUL.md 文件加载工具，
 * 负责加载 {@code resources/souls/} 路径下的所有 SOUL.md 文件，
 * 一方面提供为 具体 Agent 的System Prompts，
 * 另一方面为前端查询时，分对象提供视图
 */
public class SoulPromptLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(SoulPromptLoader.class);

    private static final String SOUL_PATH_PREFIX = "souls/";

    private static final ConcurrentHashMap<String, String> soulCache = new ConcurrentHashMap<>();

    private SoulPromptLoader() {
        // DO NOTHING
    }

    public static String loadSoul(String agentSoulName) {
        return soulCache.computeIfAbsent(agentSoulName, SoulPromptLoader::loadSoulFromMdFiles);
    }

    /**
     * 从文件加载指定名称的 Soul 提示词内容
     *
     * @param agentSoulName 对应 Agent 名称，如 ZDXL、VSUU、MFXX、UHUU
     * @return SOUL 文件内容
     */
    private static String loadSoulFromMdFiles(String agentSoulName) {
        // 步骤 1: 记录正在加载的 soul 名称
        LOGGER.info("开始加载 SOUL: {}", agentSoulName);
    
        // 步骤 2: 构造文件路径
        String fileName = SOUL_PATH_PREFIX + agentSoulName + "/SOUL.md";
        LOGGER.debug("构造的文件路径：{}", fileName);
    
        // 步骤 3: 从类加载器获取资源流
        try (InputStream inputStream = SoulPromptLoader.class.getClassLoader().getResourceAsStream(fileName)) {
            if (inputStream == null) {
                LOGGER.error("找不到 SOUL 文件：{}", fileName);
                throw new BizException(SOUL_LOADER_ERROR);
            }
                
            // 步骤 4: 读取并转换为字符串
            String content = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
            LOGGER.info("成功加载 SOUL: {}, 内容长度：{}", agentSoulName, content.length());
                
            return content;
        } catch (IOException exception) {
            LOGGER.error("加载 SOUL 文件失败：{}", fileName);
            throw new BizException(SOUL_LOADER_ERROR, exception);
        }
    }


    /**
     * 清空 prompt 缓存
     */
    public static void clearCache() {
        soulCache.clear();
    }

    /**
     * 获取 prompt 缓存大小
     *
     * @return 缓存中的 prompt 数量
     */
    public static int getCacheSize() {
        return soulCache.size();
    }
}
