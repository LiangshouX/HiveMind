package com.liangshou.common.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CryptoUtilsTest {
    @Test
    void testMd5() {
        String md5 = CryptoUtils.md5("123456");
        assertNotNull(md5);
        assertEquals(32, md5.length());
    }
    
    @Test
    void testSha256() {
        String sha256 = CryptoUtils.sha256("123456");
        assertNotNull(sha256);
        assertEquals(64, sha256.length());
    }
}
