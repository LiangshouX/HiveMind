package com.liangshou.tangdynasty.agentic.application.service;

import com.liangshou.tangdynasty.agentic.infrastructure.mysql.po.SkillMetaManagePO;
import com.liangshou.tangdynasty.agentic.infrastructure.mysql.support.SkillMetaManageSupport;
import com.liangshou.tangdynasty.agentic.infrastructure.mysql.support.dto.SkillCreateRequest;
import com.liangshou.tangdynasty.agentic.infrastructure.mysql.support.dto.SkillResponse;
import com.liangshou.tangdynasty.agentic.infrastructure.mysql.support.dto.SkillVersionRequest;
import com.liangshou.tangdynasty.agentic.infrastructure.storage.OssProperties;
import com.liangshou.tangdynasty.agentic.infrastructure.storage.SkillFileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SkillApplicationService 集成测试
 * 测试 Skill 的完整生命周期：创建、更新、发布、下载、归档、删除
 */
@ExtendWith(MockitoExtension.class)
class SkillApplicationServiceIntegrationTest {

    @Mock
    private SkillMetaManageSupport skillMetaSupport;

    @Mock
    private SkillFileStorageService fileStorageService;

    @Mock
    private OssProperties ossProperties;

    private SkillApplicationService skillAppService;

    @BeforeEach
    void setUp() {
        skillAppService = new SkillApplicationService(skillMetaSupport, fileStorageService, ossProperties);
    }

    @Test
    void testCreateSkill_Success() {
        // Given
        String userId = "user-123";
        SkillCreateRequest request = new SkillCreateRequest();
        request.setName("test-skill");
        request.setDescription("测试技能");
        request.setSkillMarkdown("---\nname: test-skill\ndescription: 测试\n---\n内容");
        request.setVersion("1.0.0");
        request.setPublish(true);

        Map<String, String> resources = new HashMap<>();
        resources.put("scripts/test.py", "#!/usr/bin/env python3\nprint('test')");
        request.setResources(resources);
        request.setTags(new String[]{"test", "demo"});

        when(skillMetaSupport.findByUserIdAndName(userId, "test-skill")).thenReturn(null);
        when(skillMetaSupport.createSkill(anyString(), anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    SkillMetaManagePO po = new SkillMetaManagePO();
                    po.setSkillId("skill-uuid");
                    po.setUserId(invocation.getArgument(0));
                    po.setName(invocation.getArgument(1));
                    po.setDescription(invocation.getArgument(2));
                    po.setCurrentVersion(invocation.getArgument(3));
                    po.setStatus("draft");
                    return po;
                });

        when(fileStorageService.uploadSkillVersion(anyString(), anyString(), anyString(), anyString(), anyMap()))
                .thenReturn("user-123/skill-uuid/v1.0.0/skill.tar.gz");

        when(skillMetaSupport.publishSkill("skill-uuid", "1.0.0")).thenReturn(true);
        when(skillMetaSupport.updateById(any())).thenReturn(true);
        when(ossProperties.isEnabled()).thenReturn(true);
        when(fileStorageService.generateDownloadUrl(anyString(), anyString(), anyString()))
                .thenReturn(new URL("http://example.com/download.tar.gz"));

        // When
        SkillResponse response = skillAppService.createSkill(userId, request);

        // Then
        assertNotNull(response);
        assertEquals("skill-uuid", response.getSkillId());
        assertEquals("test-skill", response.getName());
        assertEquals("1.0.0", response.getCurrentVersion());
        assertEquals("published", response.getStatus());
        assertNotNull(response.getDownloadUrl());

        verify(skillMetaSupport, times(1)).createSkill(anyString(), anyString(), anyString(), anyString());
        verify(fileStorageService, times(1)).uploadSkillVersion(anyString(), anyString(), anyString(), anyString(), anyMap());
        verify(skillMetaSupport, times(1)).publishSkill("skill-uuid", "1.0.0");
    }

    @Test
    void testCreateSkill_DuplicateName() {
        // Given
        String userId = "user-123";
        SkillCreateRequest request = new SkillCreateRequest();
        request.setName("test-skill");
        request.setSkillMarkdown("---\nname: test-skill\n---\n内容");

        when(skillMetaSupport.findByUserIdAndName(userId, "test-skill"))
                .thenReturn(new SkillMetaManagePO());

        // When / Then
        assertThrows(IllegalArgumentException.class, () -> {
            skillAppService.createSkill(userId, request);
        });
    }

    @Test
    void testUpdateSkill_NewVersion() {
        // Given
        String userId = "user-123";
        String skillId = "skill-uuid";

        SkillMetaManagePO existingSkill = new SkillMetaManagePO();
        existingSkill.setSkillId(skillId);
        existingSkill.setUserId(userId);
        existingSkill.setCurrentVersion("1.0.0");

        SkillVersionRequest request = new SkillVersionRequest();
        request.setVersion("2.0.0");
        request.setSkillMarkdown("---\nname: test-skill\ndescription: 更新\n---\n新内容");

        when(skillMetaSupport.getById(skillId)).thenReturn(existingSkill);
        when(fileStorageService.versionExists(userId, skillId, "2.0.0")).thenReturn(false);
        when(fileStorageService.uploadSkillVersion(anyString(), anyString(), anyString(), anyString(), anyMap()))
                .thenReturn("user-123/skill-uuid/v2.0.0/skill.tar.gz");
        when(skillMetaSupport.updateCurrentVersion(skillId, "2.0.0")).thenReturn(true);
        when(skillMetaSupport.updateFileManifest(anyString(), anyMap())).thenReturn(true);

        // When
        SkillResponse response = skillAppService.updateSkill(userId, skillId, request);

        // Then
        assertNotNull(response);
        verify(skillMetaSupport, times(1)).updateCurrentVersion(skillId, "2.0.0");
        verify(fileStorageService, times(1)).uploadSkillVersion(anyString(), anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    void testUpdateSkill_VersionAlreadyExists() {
        // Given
        String userId = "user-123";
        String skillId = "skill-uuid";

        SkillMetaManagePO existingSkill = new SkillMetaManagePO();
        existingSkill.setSkillId(skillId);
        existingSkill.setUserId(userId);

        SkillVersionRequest request = new SkillVersionRequest();
        request.setVersion("1.0.0");
        request.setSkillMarkdown("---\nname: test-skill\n---\n内容");

        when(skillMetaSupport.getById(skillId)).thenReturn(existingSkill);
        when(fileStorageService.versionExists(userId, skillId, "1.0.0")).thenReturn(true);

        // When / Then
        assertThrows(IllegalArgumentException.class, () -> {
            skillAppService.updateSkill(userId, skillId, request);
        });
    }

    @Test
    void testPublishSkill() {
        // Given
        String userId = "user-123";
        String skillId = "skill-uuid";

        SkillMetaManagePO existingSkill = new SkillMetaManagePO();
        existingSkill.setSkillId(skillId);
        existingSkill.setUserId(userId);
        existingSkill.setCurrentVersion("1.0.0");
        existingSkill.setStatus("draft");

        when(skillMetaSupport.getById(skillId)).thenReturn(existingSkill);
        when(skillMetaSupport.publishSkill(skillId, "1.0.0")).thenReturn(true);

        // When
        SkillResponse response = skillAppService.publishSkill(userId, skillId);

        // Then
        assertNotNull(response);
        verify(skillMetaSupport, times(1)).publishSkill(skillId, "1.0.0");
    }

    @Test
    void testArchiveSkill() {
        // Given
        String userId = "user-123";
        String skillId = "skill-uuid";

        SkillMetaManagePO existingSkill = new SkillMetaManagePO();
        existingSkill.setSkillId(skillId);
        existingSkill.setUserId(userId);

        when(skillMetaSupport.getById(skillId)).thenReturn(existingSkill);
        when(skillMetaSupport.archiveSkill(skillId)).thenReturn(true);

        // When
        skillAppService.archiveSkill(userId, skillId);

        // Then
        verify(skillMetaSupport, times(1)).archiveSkill(skillId);
    }

    @Test
    void testDeleteSkill() {
        // Given
        String userId = "user-123";
        String skillId = "skill-uuid";

        SkillMetaManagePO existingSkill = new SkillMetaManagePO();
        existingSkill.setSkillId(skillId);
        existingSkill.setUserId(userId);

        when(skillMetaSupport.getById(skillId)).thenReturn(existingSkill);
        when(skillMetaSupport.removeById(skillId)).thenReturn(true);

        // When
        skillAppService.deleteSkill(userId, skillId);

        // Then
        verify(skillMetaSupport, times(1)).removeById(skillId);
    }

    @Test
    void testGetDownloadUrl() {
        // Given
        String userId = "user-123";
        String skillId = "skill-uuid";

        SkillMetaManagePO existingSkill = new SkillMetaManagePO();
        existingSkill.setSkillId(skillId);
        existingSkill.setUserId(userId);
        existingSkill.setCurrentVersion("1.0.0");

        when(skillMetaSupport.getById(skillId)).thenReturn(existingSkill);
        when(fileStorageService.generateDownloadUrl(userId, skillId, "v1.0.0"))
                .thenReturn(new URL("http://example.com/download.tar.gz"));

        // When
        String downloadUrl = skillAppService.getDownloadUrl(userId, skillId);

        // Then
        assertNotNull(downloadUrl);
        assertEquals("http://example.com/download.tar.gz", downloadUrl);
    }

    @Test
    void testGetSkill_NotFound() {
        // Given
        String userId = "user-123";
        String skillId = "non-existent";

        when(skillMetaSupport.getById(skillId)).thenReturn(null);

        // When / Then
        assertThrows(IllegalArgumentException.class, () -> {
            skillAppService.getSkill(userId, skillId);
        });
    }

    @Test
    void testGetSkill_NoPermission() {
        // Given
        String userId = "user-123";
        String skillId = "skill-uuid";

        SkillMetaManagePO existingSkill = new SkillMetaManagePO();
        existingSkill.setSkillId(skillId);
        existingSkill.setUserId("other-user");

        when(skillMetaSupport.getById(skillId)).thenReturn(existingSkill);

        // When / Then
        assertThrows(IllegalArgumentException.class, () -> {
            skillAppService.getSkill(userId, skillId);
        });
    }
}
