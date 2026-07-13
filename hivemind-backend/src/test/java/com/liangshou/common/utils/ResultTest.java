package com.liangshou.common.utils;

import com.liangshou.agentic.common.utils.Result;
import com.liangshou.agentic.common.exceptions.HmeErrorCode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResultTest {
    @Test
    void testSuccess() {
        Result<String> result = Result.success("ok");
        assertEquals("HME_SYSTEM_000", result.getCode());
        assertEquals("Success", result.getMessage());
        assertEquals("ok", result.getData());

        Result<Void> empty = Result.success();
        assertNull(empty.getData());
    }

    @Test
    void testErrorWithStringCode() {
        Result<Void> result = Result.error("HME_SYSTEM_001", "error");
        assertEquals("HME_SYSTEM_001", result.getCode());
        assertEquals("error", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void testErrorWithErrorCode() {
        Result<Void> result = Result.error(HmeErrorCode.SYSTEM_ERROR);
        assertEquals("HME_SYSTEM_001", result.getCode());
        assertEquals("系统内部错误", result.getMessage());
        assertNull(result.getData());
    }
}
