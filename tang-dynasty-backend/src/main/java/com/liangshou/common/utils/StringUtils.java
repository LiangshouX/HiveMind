package com.liangshou.common.utils;

public final class StringUtils {
    private StringUtils() {}

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
    
    public static boolean isNotBlank(CharSequence cs) {
        return !isBlank(cs);
    }
}
