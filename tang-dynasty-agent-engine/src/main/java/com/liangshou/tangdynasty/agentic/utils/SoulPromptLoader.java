package com.liangshou.tangdynasty.agentic.utils;

import com.liangshou.tangdynasty.agentic.common.exceptions.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static com.liangshou.tangdynasty.agentic.common.exceptions.ErrorCodeEnum.SOUL_LOADER_ERROR;

/**
 * SOUL.md 文件加载工具，
 * 负责加载 {@code resources/souls/} 路径下的所有 SOUL.md 文件，
 * 一方面提供为 具体 Agent 的System Prompts，
 * 另一方面为前端查询时，分对象提供视图
 */
@SuppressWarnings("unused")
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

    /**
     * 扫描 resources/souls/ 路径下所有配置的 SOUL 名称
     * 每个子文件夹代表一个 SOUL，文件夹名称即为 SOUL 名称
     *
     * @return 所有 SOUL 名称列表，可用于后续注册 Agent 时作为 agentId
     */
    public static List<String> scanAllSouls() {
        LOGGER.info("开始扫描所有 SOUL 配置...");

        List<String> soulNames = new ArrayList<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        try {
            // 使用类路径通配符扫描 souls/ 下的所有一级子目录
            String pattern = "classpath*:" + SOUL_PATH_PREFIX + "*/";
            LOGGER.debug("扫描模式：{}", pattern);

            Resource[] resources = resolver.getResources(pattern);

            for (Resource resource : resources) {
                if (resource != null && resource.exists()) {
                    // 从文件路径中提取 SOUL 名称（文件夹名）
                    String path = resource.getURL().getPath();
                    // 提取最后一个路径段作为 SOUL 名称
                    String soulName = extractSoulNameFromPath(path);

                    if (soulName != null && !soulName.isEmpty()) {
                        soulNames.add(soulName);
                        LOGGER.debug("发现 SOUL: {}", soulName);
                    }
                }
            }

            LOGGER.info("成功扫描到 {} 个 SOUL 配置：{}", soulNames.size(), soulNames);
            return soulNames;

        } catch (IOException e) {
            LOGGER.error("扫描 SOUL 配置失败");
            throw new BizException(SOUL_LOADER_ERROR, e);
        }
    }

    /**
     * 从资源路径中提取 SOUL 名称
     *
     * @param path 资源路径，可能是文件系统路径或 JAR 内路径
     * @return SOUL 名称（文件夹名）
     */
    private static String extractSoulNameFromPath(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        // 移除末尾的斜杠
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        // 找到最后一个斜杠位置，提取其后的部分
        int lastSlashIndex = path.lastIndexOf('/');
        if (lastSlashIndex >= 0 && lastSlashIndex < path.length() - 1) {
            return path.substring(lastSlashIndex + 1);
        }

        return path;
    }
}
