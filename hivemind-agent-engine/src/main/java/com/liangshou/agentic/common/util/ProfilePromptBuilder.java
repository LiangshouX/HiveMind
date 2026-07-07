package com.liangshou.agentic.common.util;

import com.liangshou.agentic.domain.profile.model.AgentProfileDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Profile Prompt 构建器 - 根据用户的 Profile 配置构建 System Prompt 的一部分。
 *
 * <p>该工具类参考 Python 实现，负责：</p>
 * <ul>
 *     <li>按顺序加载 Profile 文件（SOUL.md → AGENTS.md → PROFILE.md）</li>
 *     <li>自动剥离 YAML frontmatter（--- 开头的元数据块）</li>
 *     <li>处理 AGENTS.md 中的 heartbeat 标记</li>
 *     <li>拼接为完整的 prompt 字符串</li>
 * </ul>
 *
 * <p>加载规则：</p>
 * <ul>
 *     <li>所有文件都是可选的，不存在不报错</li>
 *     <li>如果文件被禁用（enabled=false），跳过该文件</li>
 *     <li>如果文件内容为空，跳过该文件</li>
 *     <li>如果所有文件都不存在，返回空字符串（由调用方兜底）</li>
 * </ul>
 *
 * @author LiangshouX
 */
public class ProfilePromptBuilder {

    private static final Logger log = LoggerFactory.getLogger(ProfilePromptBuilder.class);

    /**
     * Heartbeat 匹配正则：<!-- heartbeat:start -->...<!-- heartbeat:end -->
     */
    private static final Pattern HEARTBEAT_PATTERN = Pattern.compile(
            "<!-- heartbeat:start -->.*?<!-- heartbeat:end -->",
            Pattern.DOTALL
    );

    /**
     * Profile 文件加载顺序
     */
    private static final List<String> LOAD_ORDER = List.of("SOUL.md", "AGENTS.md", "PROFILE.md");

    private ProfilePromptBuilder() {
        // 工具类，禁止实例化
    }

    /**
     * 根据用户的 Profile 配置构建 System Prompt。
     *
     * <p>该方法执行以下步骤：</p>
     * <ol>
     *     <li>按加载顺序遍历 Profile 文件（SOUL.md → AGENTS.md → PROFILE.md）</li>
     *     <li>对于每个文件：</li>
     *     <ul>
     *         <li>如果文件被禁用，跳过</li>
     *         <li>剥离 YAML frontmatter</li>
     *         <li>如果是 AGENTS.md，处理 heartbeat 标记</li>
     *         <li>如果内容不为空，添加 "# {filename}" 标题和内容到结果列表</li>
     *     </ul>
     *     <li>使用 "\n\n" 拼接所有部分</li>
     * </ol>
     *
     * <p><strong>示例输出：</strong></p>
     * <pre>
     * # SOUL.md
     * 你是多Agent协作 AI 助手...
     *
     * # AGENTS.md
     * 工作流规则...
     *
     * # PROFILE.md
     * 用户画像...
     * </pre>
     *
     * @param profiles 用户的所有 Profile 配置（从 {@link com.liangshou.agentic.application.ITdAgentProfileService} 加载）
     * @return 拼接后的 prompt 字符串；如果没有任何有效 Profile，返回空字符串
     */
    public static String build(List<AgentProfileDocument> profiles) {
        List<String> parts = new ArrayList<>();

        for (String filename : LOAD_ORDER) {
            // 查找对应的 Profile
            Optional<AgentProfileDocument> opt = profiles.stream()
                    .filter(p -> filename.equals(p.getFilename()))
                    .findFirst();

            if (opt.isEmpty()) {
                log.debug("Profile 不存在，跳过 - filename: {}", filename);
                continue;
            }

            AgentProfileDocument profile = opt.get();

            // 如果禁用，跳过
            if (!profile.isEnabled()) {
                log.debug("Profile 已禁用，跳过 - filename: {}", filename);
                continue;
            }

            String content = profile.getContent();
            if (content == null || content.isBlank()) {
                log.debug("Profile 内容为空，跳过 - filename: {}", filename);
                continue;
            }

            // 剥离 YAML frontmatter
            content = stripFrontmatter(content);

            // AGENTS.md 特殊处理：heartbeat
            if ("AGENTS.md".equals(filename)) {
                content = processHeartbeat(content);
            }

            // 如果处理后内容为空，跳过
            if (content.isBlank()) {
                log.debug("Profile 处理后为空，跳过 - filename: {}", filename);
                continue;
            }

            // 添加到结果列表
            parts.add("# " + filename);
            parts.add(content);

            log.debug("成功加载 Profile - filename: {}, content length: {}", filename, content.length());
        }

        // 降级兜底
        if (parts.isEmpty()) {
            log.info("所有 Profile 都为空，返回空字符串");
            return "";
        }

        String result = String.join("\n\n", parts);
        log.debug("Profile Prompt 构建成功 - total length: {}", result.length());
        return result;
    }

    /**
     * 剥离 YAML frontmatter（--- 开头的元数据块）。
     *
     * <p>YAML frontmatter 格式：</p>
     * <pre>
     * ---
     * summary: "SOUL.md 工作区模板"
     * read_when:
     *   - 手动引导工作区
     * ---
     * 实际内容...
     * </pre>
     *
     * <p>该方法会移除 --- 之间的所有元数据，只保留实际内容。</p>
     *
     * @param content 原始文件内容
     * @return 剥离 frontmatter 后的内容
     */
    private static String stripFrontmatter(String content) {
        if (content == null) {
            return "";
        }

        String trimmed = content.trim();
        if (!trimmed.startsWith("---")) {
            return trimmed;
        }

        // 分割为最多 3 部分：--- ... --- ... (剩余部分)
        String[] parts = trimmed.split("---", 3);
        if (parts.length >= 3) {
            // 第 3 部分是实际内容
            return parts[2].trim();
        }

        // 如果没有找到结束标记，返回原始内容
        return trimmed;
    }

    /**
     * 处理 AGENTS.md 中的 heartbeat 标记。
     *
     * <p>当前版本直接移除 heartbeat 标记（因为 heartbeat_enabled 配置暂未实现）。</p>
     *
     * <p>未来可以根据配置决定是否保留 heartbeat 段：</p>
     * <ul>
     *     <li>如果 heartbeat_enabled = true：只移除 <!-- heartbeat:start/end --> 标记，保留内容</li>
     *     <li>如果 heartbeat_enabled = false：移除整个 heartbeat 段（包括内容）</li>
     * </ul>
     *
     * @param content AGENTS.md 的内容
     * @return 处理 heartbeat 后的内容
     */
    private static String processHeartbeat(String content) {
        // 当前版本：移除整个 heartbeat 段
        String result = HEARTBEAT_PATTERN.matcher(content).replaceAll("");

        // 也移除单独的标记（如果有没有匹配的）
        result = result.replace("<!-- heartbeat:start -->", "");
        result = result.replace("<!-- heartbeat:end -->", "");

        return result.trim();
    }
}
