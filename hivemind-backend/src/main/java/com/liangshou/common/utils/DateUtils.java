package com.liangshou.common.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日期时间工具类。
 * <p>提供日期时间的格式化和解析功能，支持常用的日期时间格式。</p>
 * <p>此类为工具类，不可实例化，所有方法均为静态方法。</p>
 * <p>默认格式：yyyy-MM-dd HH:mm:ss</p>
 *
 * @author liangshou
 * @version 1.0
 * @see LocalDateTime
 * @see DateTimeFormatter
 */
public final class DateUtils {

    private DateUtils() {
        // 私有构造函数，防止实例化
    }

    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 格式化日期时间对象为字符串。
     * <p>使用默认格式 yyyy-MM-dd HH:mm:ss 进行格式化。</p>
     *
     * @param time 要格式化的日期时间对象
     * @return 格式化后的字符串，如果输入为 null 则返回 null
     * @see LocalDateTime
     * @see DateTimeFormatter
     */
    public static String format(LocalDateTime time) {
        if (time == null) return null;
        return time.format(DEFAULT_FORMATTER);
    }

    /**
     * 将字符串解析为日期时间对象。
     * <p>按照默认格式 yyyy-MM-dd HH:mm:ss 进行解析。</p>
     *
     * @param timeStr 要解析的日期时间字符串
     * @return 解析后的日期时间对象，如果输入为 null 或空白字符串则返回 null
     * @see LocalDateTime
     * @see DateTimeFormatter
     */
    public static LocalDateTime parse(String timeStr) {
        if (StringUtils.isBlank(timeStr)) return null;
        return LocalDateTime.parse(timeStr, DEFAULT_FORMATTER);
    }
}
