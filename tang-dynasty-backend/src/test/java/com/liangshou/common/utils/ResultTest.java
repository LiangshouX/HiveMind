package com.liangshou.common.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResultTest {
    @Test
    void testSuccess() {
        Result<String> result = Result.success("ok");
        assertEquals(200, result.getCode());
        assertEquals("Success", result.getMessage());
        assertEquals("ok", result.getData());
        
        Result<Void> empty = Result.success();
        assertNull(empty.getData());
    }
    
    @Test
    void testError() {
        Result<Void> result = Result.error(500, "error");
        assertEquals(500, result.getCode());
        assertEquals("error", result.getMessage());
        assertNull(result.getData());
    }
}
