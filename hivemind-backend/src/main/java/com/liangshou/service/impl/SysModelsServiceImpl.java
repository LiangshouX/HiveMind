package com.liangshou.service.impl;

import cn.hutool.crypto.symmetric.AES;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liangshou.common.utils.PageResult;
import com.liangshou.infrastructure.datasource.po.SysModelsPO;
import com.liangshou.infrastructure.datasource.support.ISysModelsSupport;
import com.liangshou.service.ISysModelsService;
import com.liangshou.service.dto.SysModelsDTO;
import com.liangshou.service.vo.SysModelsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysModelsServiceImpl implements ISysModelsService {

    private final ISysModelsSupport support;

    @Value("${app.crypto.api-key-secret:hivemind-default-api-key-secret-32b}")
    private String api_key_secret;

    // ======================== Query ========================

    @Override
    public SysModelsVO getById(String userId, Long id) {
        SysModelsPO po = support.lambdaQuery()
                .eq(SysModelsPO::getId, id)
                .eq(SysModelsPO::getUserId, userId)
                .one();
        if (po == null) return null;
        return toVO(po);
    }

    @Override
    public PageResult<SysModelsVO> page(String userId, int current, int size) {
        Page<SysModelsPO> page = support.lambdaQuery()
                .eq(SysModelsPO::getUserId, userId)
                .orderByDesc(SysModelsPO::getUpdateTime)
                .page(new Page<>(current, size));
        List<SysModelsVO> records = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return PageResult.of(page.getTotal(), records, page.getCurrent(), page.getSize());
    }

    @Override
    public List<SysModelsVO> listAll(String userId) {
        List<SysModelsPO> poList = support.lambdaQuery()
                .eq(SysModelsPO::getUserId, userId)
                .orderByDesc(SysModelsPO::getUpdateTime)
                .list();
        return poList.stream().map(this::toVO).collect(Collectors.toList());
    }

    // ======================== Create ========================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysModelsVO save(String userId, SysModelsDTO dto) {
        SysModelsPO po = SysModelsPO.builder()
                .userId(userId)
                .modelProviderId(dto.getModelProviderId())
                .providerName(dto.getProviderName())
                .modelProviderType(dto.getModelProviderType() != null ? dto.getModelProviderType() : "CUSTOM")
                .isProviderActivated(0)
                .isSystemBuiltIn(0)
                .baseUrl(dto.getBaseUrl())
                .apiKey(encryptApiKey(dto.getApiKey()))
                .modelsJson(dto.getModelsJson())
                .modelId(dto.getModelId())
                .modelName(dto.getModelName())
                .build();
        support.save(po);
        return toVO(po);
    }

    // ======================== Update ========================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(String userId, SysModelsDTO dto) {
        SysModelsPO po = support.lambdaQuery()
                .eq(SysModelsPO::getId, dto.getId())
                .eq(SysModelsPO::getUserId, userId)
                .one();
        if (po == null) {
            throw new RuntimeException("记录不存在或无权限修改");
        }
        if (dto.getProviderName() != null) po.setProviderName(dto.getProviderName());
        if (dto.getBaseUrl() != null) po.setBaseUrl(dto.getBaseUrl());
        if (dto.getApiKey() != null) po.setApiKey(encryptApiKey(dto.getApiKey()));
        if (dto.getModelsJson() != null) po.setModelsJson(dto.getModelsJson());
        if (dto.getModelId() != null) po.setModelId(dto.getModelId());
        if (dto.getModelName() != null) po.setModelName(dto.getModelName());
        return support.updateById(po);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean activate(String userId, Long id) {
        return setActivation(userId, id, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deactivate(String userId, Long id) {
        return setActivation(userId, id, false);
    }

    private boolean setActivation(String userId, Long id, boolean activated) {
        SysModelsPO po = support.lambdaQuery()
                .eq(SysModelsPO::getId, id)
                .eq(SysModelsPO::getUserId, userId)
                .one();
        if (po == null) {
            throw new RuntimeException("记录不存在或无权限修改");
        }
        po.setIsProviderActivated(activated ? 1 : 0);
        return support.updateById(po);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean selectModel(String userId, Long id, String modelId, String modelName) {
        SysModelsPO po = support.lambdaQuery()
                .eq(SysModelsPO::getId, id)
                .eq(SysModelsPO::getUserId, userId)
                .one();
        if (po == null) {
            throw new RuntimeException("记录不存在或无权限修改");
        }
        po.setModelId(modelId);
        po.setModelName(modelName);
        return support.updateById(po);
    }

    // ======================== Delete ========================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(String userId, Long id) {
        SysModelsPO po = support.lambdaQuery()
                .eq(SysModelsPO::getId, id)
                .eq(SysModelsPO::getUserId, userId)
                .one();
        if (po == null) {
            throw new RuntimeException("记录不存在或无权限删除");
        }
        if (po.getIsSystemBuiltIn() != null && po.getIsSystemBuiltIn() == 1) {
            throw new RuntimeException("系统内置 Provider 不可删除");
        }
        return support.removeById(id);
    }

    // ======================== Helpers ========================

    /**
     * 获取解密后的 API Key（仅内部使用，如连接测试）
     */
    public String getDecryptedApiKey(String userId, Long id) {
        SysModelsPO po = support.lambdaQuery()
                .eq(SysModelsPO::getId, id)
                .eq(SysModelsPO::getUserId, userId)
                .one();
        if (po == null || po.getApiKey() == null) return null;
        return decryptApiKey(po.getApiKey());
    }

    /**
     * 统计用户激活的 Provider 数量
     */
    public long countActivated(String userId) {
        return support.lambdaQuery()
                .eq(SysModelsPO::getUserId, userId)
                .eq(SysModelsPO::getIsProviderActivated, 1)
                .count();
    }

    /**
     * PO → VO 转换（含 API Key 掩码）
     */
    private SysModelsVO toVO(SysModelsPO po) {
        SysModelsVO vo = new SysModelsVO();
        vo.setId(po.getId());
        vo.setModelProviderId(po.getModelProviderId());
        vo.setProviderName(po.getProviderName());
        vo.setModelProviderType(po.getModelProviderType());
        vo.setIsProviderActivated(po.getIsProviderActivated() != null && po.getIsProviderActivated() == 1);
        vo.setIsSystemBuiltIn(po.getIsSystemBuiltIn() != null && po.getIsSystemBuiltIn() == 1);
        vo.setBaseUrl(po.getBaseUrl());
        vo.setApiKeyMask(maskApiKey(po.getApiKey()));
        vo.setModelId(po.getModelId());
        vo.setModelName(po.getModelName());
        vo.setModelsJson(po.getModelsJson());
        vo.setCreateTime(po.getCreateTime());
        vo.setUpdateTime(po.getUpdateTime());
        return vo;
    }

    // ======================== Encryption ========================

    private String encryptApiKey(String plainKey) {
        if (plainKey == null || plainKey.isBlank()) return null;
        try {
            AES aes = getAES();
            return aes.encryptBase64(plainKey);
        } catch (Exception e) {
            throw new RuntimeException("API Key 加密失败", e);
        }
    }

    private String decryptApiKey(String encryptedKey) {
        if (encryptedKey == null || encryptedKey.isBlank()) return null;
        try {
            AES aes = getAES();
            return aes.decryptStr(encryptedKey);
        } catch (Exception e) {
            throw new RuntimeException("API Key 解密失败", e);
        }
    }

    private String maskApiKey(String encryptedKey) {
        if (encryptedKey == null || encryptedKey.isBlank()) return null;
        try {
            String plainKey = decryptApiKey(encryptedKey);
            if (plainKey == null || plainKey.length() < 8) return "****";
            String prefix = plainKey.substring(0, Math.min(3, plainKey.length()));
            String suffix = plainKey.substring(Math.max(plainKey.length() - 4, 0));
            return prefix + "****" + suffix;
        } catch (Exception e) {
            return "****";
        }
    }

    private AES getAES() {
        byte[] keyBytes = api_key_secret.getBytes(StandardCharsets.UTF_8);
        SecretKey secretKey = new javax.crypto.spec.SecretKeySpec(keyBytes, 0, 16, "AES");
        return new AES(secretKey);
    }
}
