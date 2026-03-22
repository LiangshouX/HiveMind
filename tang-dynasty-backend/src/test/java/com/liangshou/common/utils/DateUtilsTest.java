package com.liangshou.common.utils;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class DateUtilsTest {
    @Test
    void testFormatAndParse() {
        LocalDateTime now = LocalDateTime.of(2026, 3, 22, 10, 0, 0);
        String formatted = DateUtils.format(now);
        assertEquals("2026-03-22 10:00:00", formatted);
        
        LocalDateTime parsed = DateUtils.parse(formatted);
        assertEquals(now, parsed);
        
        assertNull(DateUtils.format(null));
        assertNull(DateUtils.parse(null));
        assertNull(DateUtils.parse(""));
    }
}
