package com.liangshou.common.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DateUtils {
    private DateUtils() {}

    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String format(LocalDateTime time) {
        if (time == null) return null;
        return time.format(DEFAULT_FORMATTER);
    }

    public static LocalDateTime parse(String timeStr) {
        if (StringUtils.isBlank(timeStr)) return null;
        return LocalDateTime.parse(timeStr, DEFAULT_FORMATTER);
    }
}
