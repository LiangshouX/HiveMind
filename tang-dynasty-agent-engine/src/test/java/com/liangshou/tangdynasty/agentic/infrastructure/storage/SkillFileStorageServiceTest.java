package com.liangshou.tangdynasty.agentic.infrastructure.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SkillFileStorageService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class SkillFileStorageServiceTest {

    @Mock
    private ObjectStorageService storageService;

    @Mock
    private OssProperties ossProperties;

    private SkillFileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new SkillFileStorageService(storageService, ossProperties);
    }

    @Test
    void testUploadSkillVersion() {
        // Given
        String userId = "user-123";
        String skillId = "skill-456";
        String version = "v1.0.0";
        String skillMarkdown = "---\nname: test\n---\nTest content";
        Map<String, String> resources = new HashMap<>();
        resources.put("scripts/test.py", "#!/usr/bin/env python3\nprint('test')");

        when(ossProperties.getDefaultExpireMinutes()).thenReturn(30);
        doNothing().when(storageService).upload(
                anyString(), any(InputStream.class), anyLong(), anyString());

        // When
        String objectKey = fileStorageService.uploadSkillVersion(
                userId, skillId, version, skillMarkdown, resources);

        // Then
        assertNotNull(objectKey);
        assertEquals("user-123/skill-456/v1.0.0/skill.tar.gz", objectKey);
        verify(storageService, times(1)).upload(
                eq(objectKey),
                any(InputStream.class),
                anyLong(),
                eq("application/gzip")
        );
    }

    @Test
    void testDownloadSkillVersion() {
        // Given
        String userId = "user-123";
        String skillId = "skill-456";
        String version = "v1.0.0";
        String objectKey = "user-123/skill-456/v1.0.0/skill.tar.gz";

        when(storageService.exists(objectKey)).thenReturn(true);
        when(storageService.download(objectKey)).thenAnswer(invocation -> {
            // 返回一个模拟 的 tar.gz 流
            return new ByteArrayInputStream(new byte[0]);
        });

        // When / Then
        assertThrows(RuntimeException.class, () -> {
            fileStorageService.downloadSkillVersion(userId, skillId, version);
        });
    }

    @Test
    void testDownloadUrlGeneration() {
        // Given
        String userId = "user-123";
        String skillId = "skill-456";
        String version = "v1.0.0";
        String objectKey = "user-123/skill-456/v1.0.0/skill.tar.gz";
        URL expectedUrl = new URL("http://example.com/test.tar.gz");

        when(ossProperties.getDefaultExpireMinutes()).thenReturn(30);
        when(storageService.generatePresignedDownloadUrl(objectKey, 30))
                .thenReturn(expectedUrl);

        // When
        URL actualUrl = fileStorageService.generateDownloadUrl(userId, skillId, version);

        // Then
        assertNotNull(actualUrl);
        assertEquals(expectedUrl, actualUrl);
        verify(storageService, times(1)).generatePresignedDownloadUrl(objectKey, 30);
    }

    @Test
    void testDeleteSkillVersion() {
        // Given
        String userId = "user-123";
        String skillId = "skill-456";
        String version = "v1.0.0";
        String objectKey = "user-123/skill-456/v1.0.0/skill.tar.gz";

        doNothing().when(storageService).delete(objectKey);

        // When
        fileStorageService.deleteSkillVersion(userId, skillId, version);

        // Then
        verify(storageService, times(1)).delete(objectKey);
    }

    @Test
    void testVersionExists() {
        // Given
        String userId = "user-123";
        String skillId = "skill-456";
        String version = "v1.0.0";
        String objectKey = "user-123/skill-456/v1.0.0/skill.tar.gz";

        when(storageService.exists(objectKey)).thenReturn(true);

        // When
        boolean exists = fileStorageService.versionExists(userId, skillId, version);

        // Then
        assertTrue(exists);
        verify(storageService, times(1)).exists(objectKey);
    }

    @Test
    void testVersionNotExists() {
        // Given
        String userId = "user-123";
        String skillId = "skill-456";
        String version = "v1.0.0";
        String objectKey = "user-123/skill-456/v1.0.0/skill.tar.gz";

        when(storageService.exists(objectKey)).thenReturn(false);

        // When
        boolean exists = fileStorageService.versionExists(userId, skillId, version);

        // Then
        assertFalse(exists);
    }
}
