package com.liangshou.common.utils;

import cn.hutool.crypto.digest.DigestUtil;

/**
 * 加密工具类。
 * <p>提供常用的哈希加密算法，用于数据完整性校验和密码加密等场景。</p>
 * <p>此类为工具类，不可实例化，所有方法均为静态方法。</p>
 *
 * @author liangshou
 * @version 1.0
 * @see DigestUtil
 */
public final class CryptoUtils {

    private CryptoUtils() {
        // 私有构造函数，防止实例化
    }

    /**
     * 计算字符串的 MD5 哈希值。
     * <p>MD5 是一种广泛使用的哈希算法，生成 128 位（16 字节）的哈希值，通常用 32 位十六进制字符串表示。</p>
     * <p>适用于数据完整性校验、文件指纹生成等场景。</p>
     *
     * @param str 要计算哈希值的字符串
     * @return MD5 哈希值（32 位十六进制字符串）
     * @see DigestUtil#md5Hex(String)
     */
    public static String md5(String str) {
        return DigestUtil.md5Hex(str);
    }
    
    /**
     * 计算字符串的 SHA-256 哈希值。
     * <p>SHA-256 是一种更安全的哈希算法，生成 256 位（32 字节）的哈希值，通常用 64 位十六进制字符串表示。</p>
     * <p>相比 MD5 具有更高的安全性，适用于密码加密、数字签名等对安全性要求较高的场景。</p>
     *
     * @param str 要计算哈希值的字符串
     * @return SHA-256 哈希值（64 位十六进制字符串）
     * @see DigestUtil#sha256Hex(String)
     */
    public static String sha256(String str) {
        return DigestUtil.sha256Hex(str);
    }
}
