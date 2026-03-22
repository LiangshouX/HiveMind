package com.liangshou.common.utils;

import cn.hutool.crypto.digest.DigestUtil;

public final class CryptoUtils {
    private CryptoUtils() {}

    public static String md5(String str) {
        return DigestUtil.md5Hex(str);
    }
    
    public static String sha256(String str) {
        return DigestUtil.sha256Hex(str);
    }
}
