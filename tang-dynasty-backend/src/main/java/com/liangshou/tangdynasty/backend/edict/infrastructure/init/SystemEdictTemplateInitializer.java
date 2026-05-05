package com.liangshou.tangdynasty.backend.edict.infrastructure.init;

import com.liangshou.tangdynasty.backend.edict.domain.model.EdictTemplateDocument;
import com.liangshou.tangdynasty.backend.edict.domain.model.EdictTemplateDocument.TemplateParam;
import com.liangshou.tangdynasty.backend.edict.domain.model.EdictTemplateDocument.TemplateType;
import com.liangshou.tangdynasty.backend.edict.infrastructure.mongo.repository.EdictTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * 系统内置任务模板初始化器。
 *
 * <p>在应用启动时自动插入系统内置模板（如果尚未存在）。</p>
 *
 * @author LiangshouX
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class SystemEdictTemplateInitializer implements CommandLineRunner {

    private final EdictTemplateRepository templateRepository;

    @Override
    public void run(String... args) {
        log.info("开始初始化系统内置任务模板...");
        int count = 0;

        for (SystemTemplate tpl : BUILTIN_TEMPLATES) {
            if (!templateRepository.existsByTemplateId(tpl.templateId)) {
                EdictTemplateDocument doc = EdictTemplateDocument.builder()
                        .templateId(tpl.templateId)
                        .name(tpl.name)
                        .description(tpl.desc)
                        .category(tpl.cat)
                        .icon(tpl.icon)
                        .command(tpl.command)
                        .params(tpl.params)
                        .depts(tpl.depts)
                        .est(tpl.est)
                        .cost(tpl.cost)
                        .type(TemplateType.SYSTEM)
                        .userId(null)
                        .createdAt(Instant.now())
                        .updatedAt(null)
                        .build();
                templateRepository.save(doc);
                count++;
                log.info("  已插入内置模板: {} ({})", tpl.name, tpl.templateId);
            }
        }

        if (count > 0) {
            log.info("系统内置任务模板初始化完成，共插入 {} 个模板", count);
        } else {
            log.info("系统内置模板已存在，跳过初始化");
        }
    }

    // ── 内置模板数据（与前端 store.ts 保持一致） ──

    private static final SystemTemplate[] BUILTIN_TEMPLATES = {
        new SystemTemplate(
            "tpl_weekly_report", "日常办公", "周报生成",
            "基于本周看板数据和各部产出，自动生成结构化周报",
            "📝", List.of("户部", "礼部"), "~10分钟", "¥0.5",
            List.of(
                param("date_range", "报告周期", "text", true, "本周", null),
                param("focus", "重点关注（逗号分隔）", "text", false, "项目进展,下周计划", null),
                param("format", "输出格式", "select", false, "Markdown", List.of("Markdown", "飞书文档"))
            ),
            "生成{date_range}的周报，重点覆盖{focus}，输出为{format}格式"
        ),
        new SystemTemplate(
            "tpl_code_review", "工程开发", "代码审查",
            "对指定代码仓库/文件进行质量审查，输出问题清单和改进建议",
            "🔍", List.of("兵部", "刑部"), "~20分钟", "¥2",
            List.of(
                param("repo", "仓库/文件路径", "text", true, null, null),
                param("scope", "审查范围", "select", false, "增量(最近commit)", List.of("全量", "增量(最近commit)", "指定文件")),
                param("focus", "重点关注（可选）", "text", false, "安全漏洞,错误处理,性能", null)
            ),
            "对 {repo} 进行代码审查，范围：{scope}，重点关注：{focus}"
        ),
        new SystemTemplate(
            "tpl_api_design", "工程开发", "API 设计与实现",
            "从需求描述到 RESTful API 设计、实现、测试一条龙",
            "⚡", List.of("中书省", "兵部"), "~45分钟", "¥3",
            List.of(
                param("requirement", "需求描述", "textarea", true, null, null),
                param("tech", "技术栈", "select", false, "Python/FastAPI", List.of("Python/FastAPI", "Node/Express", "Go/Gin")),
                param("auth", "鉴权方式", "select", false, "JWT", List.of("JWT", "API Key", "无"))
            ),
            "设计并实现一个 {tech} 的 RESTful API：{requirement}。鉴权方式：{auth}"
        ),
        new SystemTemplate(
            "tpl_competitor", "数据分析", "竞品分析",
            "爬取竞品网站数据，分析对比，生成结构化报告",
            "📊", List.of("兵部", "户部", "礼部"), "~60分钟", "¥5",
            List.of(
                param("targets", "竞品名称/URL（每行一个）", "textarea", true, null, null),
                param("dimensions", "分析维度", "text", false, "产品功能,定价策略,用户评价", null),
                param("format", "输出格式", "select", false, "Markdown报告", List.of("Markdown报告", "表格对比"))
            ),
            "对以下竞品进行分析：\n{targets}\n\n分析维度：{dimensions}，输出格式：{format}"
        ),
        new SystemTemplate(
            "tpl_data_report", "数据分析", "数据报告",
            "对给定数据集进行清洗、分析、可视化，输出分析报告",
            "📈", List.of("户部", "礼部"), "~30分钟", "¥2",
            List.of(
                param("data_source", "数据源描述/路径", "text", true, null, null),
                param("questions", "分析问题（每行一个）", "textarea", false, null, null),
                param("viz", "是否需要可视化图表", "select", false, "是", List.of("是", "否"))
            ),
            "对数据 {data_source} 进行分析。{questions}\n需要可视化：{viz}"
        ),
        new SystemTemplate(
            "tpl_blog", "内容创作", "博客文章",
            "给定主题和要求，生成高质量博客文章",
            "✍️", List.of("礼部"), "~15分钟", "¥1",
            List.of(
                param("topic", "文章主题", "text", true, null, null),
                param("audience", "目标读者", "text", false, "技术人员", null),
                param("length", "期望字数", "select", false, "~2000字", List.of("~1000字", "~2000字", "~3000字")),
                param("style", "风格", "select", false, "技术教程", List.of("技术教程", "观点评论", "案例分析"))
            ),
            "写一篇关于「{topic}」的博客文章，面向{audience}，{length}，风格：{style}"
        ),
        new SystemTemplate(
            "tpl_deploy", "工程开发", "部署方案",
            "生成完整的部署检查单、Docker配置、CI/CD 流程",
            "🚀", List.of("兵部", "工部"), "~25分钟", "¥2",
            List.of(
                param("project", "项目名称/描述", "text", true, null, null),
                param("env", "部署环境", "select", false, "Docker", List.of("Docker", "K8s", "VPS", "Serverless")),
                param("ci", "CI/CD 工具", "select", false, "GitHub Actions", List.of("GitHub Actions", "GitLab CI", "无"))
            ),
            "为项目「{project}」生成{env}部署方案，CI/CD使用{ci}"
        ),
        new SystemTemplate(
            "tpl_email", "内容创作", "邮件/通知文案",
            "根据场景和目的，生成专业邮件或通知文案",
            "📧", List.of("礼部"), "~5分钟", "¥0.3",
            List.of(
                param("scenario", "使用场景", "select", false, "商务邮件", List.of("商务邮件", "产品发布", "客户通知", "内部公告")),
                param("purpose", "目的/内容", "textarea", true, null, null),
                param("tone", "语调", "select", false, "正式", List.of("正式", "友好", "简洁"))
            ),
            "撰写一封{scenario}，{tone}语调。内容：{purpose}"
        ),
        new SystemTemplate(
            "tpl_standup", "日常办公", "每日站会摘要",
            "汇总各部今日进展和明日计划，生成站会摘要",
            "🗓️", List.of("尚书省"), "~5分钟", "¥0.3",
            List.of(
                param("range", "汇总范围", "select", false, "今天", List.of("今天", "最近24小时", "昨天+今天"))
            ),
            "汇总{range}各部工作进展和待办，生成站会摘要"
        ),
    };

    private static TemplateParam param(String key, String label, String type, boolean required, String defaultValue, List<String> options) {
        return TemplateParam.builder()
                .key(key)
                .label(label)
                .type(type)
                .required(required)
                .defaultValue(defaultValue)
                .options(options)
                .build();
    }

    /**
     * 内置模板数据结构
     */
    private record SystemTemplate(
        String templateId,
        String cat,
        String name,
        String desc,
        String icon,
        List<String> depts,
        String est,
        String cost,
        List<TemplateParam> params,
        String command
    ) {}
}
