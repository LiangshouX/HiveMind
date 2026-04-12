package com.liangshou.common.utils;

/**
 * 字符串工具类。
 * <p>提供常用的字符串判断和操作方法。</p>
 * <p>此类为工具类，不可实例化，所有方法均为静态方法。</p>
 *
 * @author liangshou
 */
public final class StringUtils {
    private StringUtils() {}

    /**
     * 判断字符串是否为空白（null、空字符串或仅包含空白字符）。
     * <p>检查字符串是否为 null、长度为 0，或所有字符都是空白字符。</p>
     *
     * @param cs 要检查的字符串
     * @return 如果字符串为空白则返回 true，否则返回 false
     * @see Character#isWhitespace(char)
     */
    public static boolean isBlank(CharSequence cs) {
        int strLen;
        if (cs == null || (strLen = cs.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 判断字符串是否不为空白。
     * <p>与 {@link #isBlank(CharSequence)} 方法相反，检查字符串是否包含非空白字符。</p>
     *
     * @param cs 要检查的字符串
     * @return 如果字符串不为空白则返回 true，否则返回 false
     * @see #isBlank(CharSequence)
     */
    public static boolean isNotBlank(CharSequence cs) {
        return !isBlank(cs);
    }
}
