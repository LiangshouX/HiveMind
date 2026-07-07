package com.liangshou.common.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StringUtilsTest {
    @Test
    void testIsBlank() {
        assertTrue(StringUtils.isBlank(null));
        assertTrue(StringUtils.isBlank(""));
        assertTrue(StringUtils.isBlank("   "));
        assertFalse(StringUtils.isBlank("abc"));
        assertFalse(StringUtils.isBlank(" a "));
    }
    
    @Test
    void testIsNotBlank() {
        assertFalse(StringUtils.isNotBlank(null));
        assertTrue(StringUtils.isNotBlank("abc"));
    }
}
