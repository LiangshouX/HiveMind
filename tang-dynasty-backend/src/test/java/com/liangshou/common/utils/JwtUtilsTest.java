package com.liangshou.common.utils;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {
    @Test
    void testTokenGenerationAndValidation() {
        JwtUtils jwtUtils = new JwtUtils("TangDynastyJwtSecretKeyForConsoleLoginAndAgentAccess2026", 86400000L);
        String token = jwtUtils.generateToken("admin");
        assertNotNull(token);

        String username = jwtUtils.extractUsername(token);
        assertEquals("admin", username);

        User admin = new User("admin", "password", java.util.List.of());
        User user = new User("user", "password", java.util.List.of());

        assertTrue(jwtUtils.validateToken(token, admin));
        assertFalse(jwtUtils.validateToken(token, user));
    }
}
