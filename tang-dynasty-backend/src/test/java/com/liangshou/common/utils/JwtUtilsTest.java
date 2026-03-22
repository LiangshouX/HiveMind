package com.liangshou.common.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {
    @Test
    void testTokenGenerationAndValidation() {
        String token = JwtUtils.generateToken("admin");
        assertNotNull(token);
        
        String username = JwtUtils.extractUsername(token);
        assertEquals("admin", username);
        
        assertTrue(JwtUtils.validateToken(token, "admin"));
        assertFalse(JwtUtils.validateToken(token, "user"));
    }
}
