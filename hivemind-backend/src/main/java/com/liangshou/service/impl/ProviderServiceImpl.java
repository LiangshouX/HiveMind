package com.liangshou.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liangshou.common.utils.PageResult;
import com.liangshou.common.utils.SecurityUtils;
import com.liangshou.infrastructure.datasource.po.SysModelsPO;
import com.liangshou.infrastructure.datasource.support.ISysModelsSupport;
import com.liangshou.service.IProviderService;
import com.liangshou.service.ISysModelsService;
import com.liangshou.service.dto.ConnectionTestResult;
import com.liangshou.service.dto.ProviderDTO;
import com.liangshou.service.vo.ProviderVO;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Provider 管理服务实现
 * <p>通过委托 {@link ISysModelsService} 实现 CRUD，补充 Provider 层面的业务规则：</p>
 * <ul>
 *     <li>activate / deactivate 带有最后一个激活检查</li>
 *     <li>delete 禁止删除系统内置 Provider</li>
 *     <li>initializeBuiltIn 从 builtin_provider.json 为用户创建内置记录</li>
 *     <li>testConnection 通过 HTTP 调用 Provider 的模型列表接口</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ProviderServiceImpl implements IProviderService {

    private static final Logger log = LoggerFactory.getLogger(ProviderServiceImpl.class);

    private final ISysModelsService sysModelsService;
    private final ISysModelsSupport sysModelsSupport;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    // ======================== Query ========================

    @Override
    public PageResult<ProviderVO> list(int current, int size) {
        String userId = SecurityUtils.getCurrentUserId();
        PageResult<com.liangshou.service.vo.SysModelsVO> pageResult = sysModelsService.page(userId, current, size);
        List<ProviderVO> voList = pageResult.getRecords().stream()
                .map(this::toProviderVO)
                .collect(Collectors.toList());
        return PageResult.of(pageResult.getTotal(), voList, pageResult.getCurrent(), pageResult.getSize());
    }

    @Override
    public ProviderVO getById(Long id) {
        String userId = SecurityUtils.getCurrentUserId();
        com.liangshou.service.vo.SysModelsVO vo = sysModelsService.getById(userId, id);
        if (vo == null) {
            throw new IllegalArgumentException("Provider 不存在或无权限访问");
        }
        return toProviderVO(vo);
    }

    // ======================== Create ========================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProviderVO create(ProviderDTO dto) {
        String userId = SecurityUtils.getCurrentUserId();
        com.liangshou.service.dto.SysModelsDTO sysModelsDTO = toSysModelsDTO(dto);
        com.liangshou.service.vo.SysModelsVO saved = sysModelsService.save(userId, sysModelsDTO);
        return toProviderVO(saved);
    }

    // ======================== Update ========================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProviderVO update(Long id, ProviderDTO dto) {
        String userId = SecurityUtils.getCurrentUserId();
        com.liangshou.service.dto.SysModelsDTO sysModelsDTO = toSysModelsDTO(dto);
        sysModelsDTO.setId(id);
        sysModelsService.update(userId, sysModelsDTO);
        return getById(id);
    }

    // ======================== Delete ========================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        String userId = SecurityUtils.getCurrentUserId();
        // delete 在 SysModelsServiceImpl 中已检查 isSystemBuiltIn
        return sysModelsService.delete(userId, id);
    }

    // ======================== Activation ========================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean activate(Long id) {
        String userId = SecurityUtils.getCurrentUserId();
        return sysModelsService.activate(userId, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deactivate(Long id) {
        String userId = SecurityUtils.getCurrentUserId();

        // 检查是否是最后一个激活的 Provider（通过 ISysModelsSupport 直接查询）
        long activatedCount = sysModelsSupport.lambdaQuery()
                .eq(SysModelsPO::getUserId, userId)
                .eq(SysModelsPO::getIsProviderActivated, 1)
                .count();
        if (activatedCount <= 1) {
            throw new IllegalStateException("至少需要保留一个激活的 Provider，无法停用最后一个");
        }

        return sysModelsService.deactivate(userId, id);
    }

    // ======================== Model Selection ========================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean selectModel(Long id, String modelId, String modelName) {
        String userId = SecurityUtils.getCurrentUserId();
        return sysModelsService.selectModel(userId, id, modelId, modelName);
    }

    // ======================== Default Model ========================

    @Override
    public ProviderVO getDefaultModel() {
        String userId = SecurityUtils.getCurrentUserId();
        List<com.liangshou.service.vo.SysModelsVO> allModels = sysModelsService.listAll(userId);
        // 返回第一个激活的 Provider
        Optional<com.liangshou.service.vo.SysModelsVO> firstActivated = allModels.stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsProviderActivated()))
                .findFirst();
        return firstActivated.map(this::toProviderVO).orElse(null);
    }

    // ======================== Initialize Built-in ========================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ProviderVO> initializeBuiltIn() {
        String userId = SecurityUtils.getCurrentUserId();
        List<ProviderVO> result = new ArrayList<>();
        // 追踪本轮已插入的 providerId，避免同一事务内 save 未 flush 导致重复插入
        Set<String> insertedInThisRun = new HashSet<>();

        try {
            // 读取内置 Provider 配置
            List<Map<String, Object>> builtInProviders = loadBuiltInProviders();

            for (Map<String, Object> provider : builtInProviders) {
                String modelProviderId = (String) provider.get("id");

                // 检查用户是否已存在该 Provider（DB 已有 + 本轮已插入）
                if (insertedInThisRun.contains(modelProviderId) || providerExistsForUser(userId, modelProviderId)) {
                    continue;
                }

                // 构造 DTO 并保存
                ProviderDTO dto = new ProviderDTO();
                dto.setModelProviderId(modelProviderId);
                dto.setProviderName((String) provider.get("name"));
                dto.setModelProviderType("SYSTEM");
                dto.setBaseUrl((String) provider.get("baseUrl"));

                // apiKey 为环境变量占位符时留空
                String apiKey = (String) provider.get("apiKey");
                if (apiKey != null && !apiKey.startsWith("{{")) {
                    dto.setApiKey(apiKey);
                }

                // 序列化 models 列表为 JSON
                Object models = provider.get("models");
                if (models != null) {
                    dto.setModelsJson(objectMapper.writeValueAsString(models));
                }

                // 默认选中第一个模型
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> modelsList = (List<Map<String, Object>>) provider.get("models");
                if (modelsList != null && !modelsList.isEmpty()) {
                    Map<String, Object> firstModel = modelsList.get(0);
                    dto.setModelId((String) firstModel.get("id"));
                    dto.setModelName((String) firstModel.get("name"));
                }

                try {
                    com.liangshou.service.vo.SysModelsVO saved = sysModelsService.save(userId, toSysModelsDTO(dto));
                    result.add(toProviderVO(saved));
                    insertedInThisRun.add(modelProviderId);
                    log.info("内置 Provider 初始化完成 - userId: {}, providerId: {}", userId, modelProviderId);
                } catch (DuplicateKeyException e) {
                    // 并发请求（如 React StrictMode 双重挂载）导致唯一键冲突，视为已初始化，跳过
                    log.info("内置 Provider 已存在（并发插入），跳过 - userId: {}, providerId: {}", userId, modelProviderId);
                }
            }
        } catch (Exception e) {
            log.error("内置 Provider 初始化失败 - userId: {}, error: {}", userId, e.getMessage(), e);
            throw new RuntimeException("内置 Provider 初始化失败: " + e.getMessage(), e);
        }

        return result;
    }

    // ======================== Connection Test ========================

    @Override
    public ConnectionTestResult testConnection(Long id) {
        String userId = SecurityUtils.getCurrentUserId();
        ConnectionTestResult testResult = new ConnectionTestResult();

        // 获取 Provider 详情（含解密后的 API Key）
        com.liangshou.service.vo.SysModelsVO providerVO = sysModelsService.getById(userId, id);
        if (providerVO == null) {
            testResult.setReachable(false);
            testResult.setErrorMessage("Provider 不存在或无权限访问");
            return testResult;
        }

        String baseUrl = providerVO.getBaseUrl();
        String decryptedApiKey = getDecryptedApiKey(userId, id);

        if (baseUrl == null || baseUrl.isBlank()) {
            testResult.setReachable(false);
            testResult.setErrorMessage("baseUrl 未配置");
            return testResult;
        }

        try {
            long startTime = System.currentTimeMillis();

            // 智能构建 models URL
            String modelsUrl = buildModelsUrl(baseUrl);
            log.info("[连接测试] 请求 URL: {}, providerId: {}", modelsUrl, providerVO.getModelProviderId());

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            if (decryptedApiKey != null && !decryptedApiKey.isBlank()) {
                headers.set("Authorization", "Bearer " + decryptedApiKey);
            }

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(modelsUrl, HttpMethod.GET, entity, String.class);

            long latency = System.currentTimeMillis() - startTime;
            testResult.setLatencyMs(latency);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                testResult.setReachable(true);

                // 解析模型列表
                List<ConnectionTestResult.ModelInfo> models = parseModelList(response.getBody());
                testResult.setDiscoveredModels(models);
                log.info("[连接测试] 成功 - userId: {}, id: {}, 延迟: {}ms, 发现模型: {}",
                        userId, id, latency, models.size());
            } else {
                testResult.setReachable(false);
                testResult.setErrorMessage("HTTP " + response.getStatusCode().value());
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.warn("[连接测试] HTTP 错误 - userId: {}, id: {}, status: {}, response: {}",
                    userId, id, e.getStatusCode(), e.getResponseBodyAsString());

            // 如果 /models 端点返回 401，尝试使用 /chat/completions 端点验证
            if (e.getStatusCode().value() == 401) {
                log.info("[连接测试] /models 端点返回 401，尝试 /chat/completions 验证");
                return testConnectionWithChatEndpoint(providerVO.getModelProviderId(), baseUrl, decryptedApiKey);
            }

            testResult.setReachable(false);
            if (e.getStatusCode().value() == 403) {
                testResult.setErrorMessage("无权限访问该 API");
            } else if (e.getStatusCode().value() == 404) {
                testResult.setErrorMessage("API 端点不存在，请检查 baseUrl 是否正确");
            } else {
                testResult.setErrorMessage("HTTP " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString());
            }
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.warn("[连接测试] 网络错误 - userId: {}, id: {}, baseUrl: {}, error: {}",
                    userId, id, baseUrl, e.getMessage());
            testResult.setReachable(false);
            testResult.setErrorMessage("无法连接到服务器，请检查网络和 baseUrl 是否正确");
        } catch (Exception e) {
            log.warn("[连接测试] 未知错误 - userId: {}, id: {}, error: {}", userId, id, e.getMessage());
            testResult.setReachable(false);
            testResult.setErrorMessage(e.getMessage());
        }

        return testResult;
    }

    // ======================== Connection Test by Params ========================

    @Override
    public ConnectionTestResult testConnectionByParams(String providerId, String baseUrl, String apiKey) {
        ConnectionTestResult testResult = new ConnectionTestResult();

        if (baseUrl == null || baseUrl.isBlank()) {
            testResult.setReachable(false);
            testResult.setErrorMessage("baseUrl 未配置");
            return testResult;
        }

        try {
            long startTime = System.currentTimeMillis();

            // 智能构建 models URL：处理不同 provider 的 API 格式
            String modelsUrl = buildModelsUrl(baseUrl);
            log.info("[连接测试] 请求 URL: {}, providerId: {}", modelsUrl, providerId);

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            if (apiKey != null && !apiKey.isBlank()) {
                headers.set("Authorization", "Bearer " + apiKey);
            }

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(modelsUrl, HttpMethod.GET, entity, String.class);

            long latency = System.currentTimeMillis() - startTime;
            testResult.setLatencyMs(latency);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                testResult.setReachable(true);
                List<ConnectionTestResult.ModelInfo> models = parseModelList(response.getBody());
                testResult.setDiscoveredModels(models);
                log.info("[连接测试] 成功 - providerId: {}, 延迟: {}ms, 发现模型: {}",
                        providerId, latency, models.size());
            } else {
                testResult.setReachable(false);
                testResult.setErrorMessage("HTTP " + response.getStatusCode().value());
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.warn("[连接测试] HTTP 错误 - providerId: {}, baseUrl: {}, status: {}, response: {}",
                    providerId, baseUrl, e.getStatusCode(), e.getResponseBodyAsString());

            // 如果 /models 端点返回 401，尝试使用 /chat/completions 端点验证
            if (e.getStatusCode().value() == 401) {
                log.info("[连接测试] /models 端点返回 401，尝试 /chat/completions 验证");
                return testConnectionWithChatEndpoint(providerId, baseUrl, apiKey);
            }

            testResult.setReachable(false);
            // 提供更友好的错误信息
            if (e.getStatusCode().value() == 403) {
                testResult.setErrorMessage("无权限访问该 API");
            } else if (e.getStatusCode().value() == 404) {
                testResult.setErrorMessage("API 端点不存在，请检查 baseUrl 是否正确");
            } else {
                testResult.setErrorMessage("HTTP " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString());
            }
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.warn("[连接测试] 网络错误 - providerId: {}, baseUrl: {}, error: {}",
                    providerId, baseUrl, e.getMessage());
            testResult.setReachable(false);
            testResult.setErrorMessage("无法连接到服务器，请检查网络和 baseUrl 是否正确");
        } catch (Exception e) {
            log.warn("[连接测试] 未知错误 - providerId: {}, baseUrl: {}, error: {}",
                    providerId, baseUrl, e.getMessage());
            testResult.setReachable(false);
            testResult.setErrorMessage(e.getMessage());
        }

        return testResult;
    }

    /**
     * 使用 /chat/completions 端点验证连接。
     * 当 /models 端点返回 401 时，尝试使用此方法验证。
     */
    private ConnectionTestResult testConnectionWithChatEndpoint(String providerId, String baseUrl, String apiKey) {
        ConnectionTestResult testResult = new ConnectionTestResult();

        try {
            long startTime = System.currentTimeMillis();

            // 构建 chat completions URL
            String chatUrl = baseUrl.replaceAll("/+$", "");
            if (!chatUrl.endsWith("/chat/completions")) {
                chatUrl = chatUrl + "/chat/completions";
            }

            log.info("[连接测试] 尝试 /chat/completions 端点: {}", chatUrl);

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            if (apiKey != null && !apiKey.isBlank()) {
                headers.set("Authorization", "Bearer " + apiKey);
            }

            // 构建一个最小的请求体
            String requestBody = """
                    {
                        "model": "gpt-3.5-turbo",
                        "messages": [{"role": "user", "content": "hi"}],
                        "max_tokens": 1,
                        "stream": false
                    }
                    """;

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(chatUrl, HttpMethod.POST, entity, String.class);

            long latency = System.currentTimeMillis() - startTime;
            testResult.setLatencyMs(latency);

            if (response.getStatusCode().is2xxSuccessful()) {
                testResult.setReachable(true);
                log.info("[连接测试] /chat/completions 验证成功 - providerId: {}, 延迟: {}ms",
                        providerId, latency);
            } else {
                testResult.setReachable(false);
                testResult.setErrorMessage("HTTP " + response.getStatusCode().value());
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.warn("[连接测试] /chat/completions 也失败 - providerId: {}, status: {}, response: {}",
                    providerId, e.getStatusCode(), e.getResponseBodyAsString());

            testResult.setReachable(false);
            if (e.getStatusCode().value() == 401) {
                testResult.setErrorMessage("API Key 无效或已过期");
            } else if (e.getStatusCode().value() == 400) {
                // 400 可能是因为模型名称不正确，但连接是通的
                testResult.setReachable(true);
                testResult.setErrorMessage("连接成功，但模型配置可能需要调整");
            } else {
                testResult.setErrorMessage("HTTP " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString());
            }
        } catch (Exception e) {
            log.warn("[连接测试] /chat/completions 异常 - providerId: {}, error: {}",
                    providerId, e.getMessage());
            testResult.setReachable(false);
            testResult.setErrorMessage(e.getMessage());
        }

        return testResult;
    }

    /**
     * 智能构建 models URL
     * 处理不同 provider 的 API 格式：
     * - 如果 baseUrl 已包含 /models，直接使用
     * - 如果 baseUrl 以 /v1 结尾，追加 /models
     * - 如果 baseUrl 以 /v1/ 结尾，追加 models
     * - 其他情况，追加 /v1/models
     */
    private String buildModelsUrl(String baseUrl) {
        String normalized = baseUrl.replaceAll("/+$", "");

        // 如果已经包含 /models，直接使用
        if (normalized.endsWith("/models")) {
            return normalized;
        }

        // 如果以 /v1 结尾，追加 /models
        if (normalized.endsWith("/v1")) {
            return normalized + "/models";
        }

        // 如果以 /v1/ 结尾，追加 models
        if (normalized.endsWith("/v1/")) {
            return normalized + "models";
        }

        // 检查是否是常见的 API 路径格式
        if (normalized.matches(".*\\/v\\d+$")) {
            // 已经是版本化路径，追加 /models
            return normalized + "/models";
        }

        // 其他情况，尝试追加 /v1/models
        return normalized + "/v1/models";
    }

    // ======================== Active Models ========================

    @Override
    public List<ProviderVO> getActiveModels() {
        String userId = SecurityUtils.getCurrentUserId();
        List<com.liangshou.service.vo.SysModelsVO> allModels = sysModelsService.listAll(userId);
        return allModels.stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsProviderActivated()))
                .map(this::toProviderVO)
                .collect(Collectors.toList());
    }

    // ======================== Private Helpers ========================

    /**
     * 检查用户是否已存在指定 providerId 的记录
     */
    private boolean providerExistsForUser(String userId, String modelProviderId) {
        return sysModelsSupport.lambdaQuery()
                .eq(SysModelsPO::getUserId, userId)
                .eq(SysModelsPO::getModelProviderId, modelProviderId)
                .exists();
    }

    /**
     * 获取解密后的 API Key
     */
    private String getDecryptedApiKey(String userId, Long id) {
        // 使用 SysModelsServiceImpl 的 getDecryptedApiKey 方法
        if (sysModelsService instanceof SysModelsServiceImpl impl) {
            return impl.getDecryptedApiKey(userId, id);
        }
        return null;
    }

    /**
     * 从 classpath 读取 builtin_provider.json
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> loadBuiltInProviders() throws Exception {
        // 尝试从 agent-engine 模块的 classpath 加载
        Resource resource = resourceLoader.getResource("classpath:provider/builtin_provider.json");
        if (!resource.exists()) {
            // fallback: 尝试从 backend 模块的 classpath 加载
            resource = resourceLoader.getResource("classpath:builtin_provider.json");
        }

        try (InputStream is = resource.getInputStream()) {
            return objectMapper.readValue(is, new TypeReference<List<Map<String, Object>>>() {});
        }
    }

    /**
     * 解析 Provider 模型列表响应
     * <p>兼容 OpenAI 格式: {"data": [{"id": "...", "name": "..."}]}</p>
     */
    @SuppressWarnings("unchecked")
    private List<ConnectionTestResult.ModelInfo> parseModelList(String responseBody) {
        List<ConnectionTestResult.ModelInfo> result = new ArrayList<>();
        try {
            Map<String, Object> root = objectMapper.readValue(responseBody, Map.class);
            List<Map<String, Object>> data = (List<Map<String, Object>>) root.get("data");
            if (data != null) {
                for (Map<String, Object> item : data) {
                    ConnectionTestResult.ModelInfo info = new ConnectionTestResult.ModelInfo();
                    info.setId((String) item.get("id"));
                    info.setName((String) item.get("name"));
                    result.add(info);
                }
            }
        } catch (Exception e) {
            log.warn("解析模型列表响应失败: {}", e.getMessage());
        }
        return result;
    }

    // ======================== Conversion Helpers ========================

    /**
     * SysModelsVO -> ProviderVO
     */
    private ProviderVO toProviderVO(com.liangshou.service.vo.SysModelsVO sysModelsVO) {
        if (sysModelsVO == null) return null;
        ProviderVO vo = new ProviderVO();
        vo.setId(sysModelsVO.getId());
        vo.setModelProviderId(sysModelsVO.getModelProviderId());
        vo.setProviderName(sysModelsVO.getProviderName());
        vo.setModelProviderType(sysModelsVO.getModelProviderType());
        vo.setIsProviderActivated(sysModelsVO.getIsProviderActivated());
        vo.setIsSystemBuiltIn(sysModelsVO.getIsSystemBuiltIn());
        vo.setBaseUrl(sysModelsVO.getBaseUrl());
        vo.setApiKeyMask(sysModelsVO.getApiKeyMask());
        vo.setModelId(sysModelsVO.getModelId());
        vo.setModelName(sysModelsVO.getModelName());
        vo.setModelsJson(sysModelsVO.getModelsJson());
        vo.setCreateTime(sysModelsVO.getCreateTime());
        vo.setUpdateTime(sysModelsVO.getUpdateTime());
        return vo;
    }

    /**
     * ProviderDTO -> SysModelsDTO
     */
    private com.liangshou.service.dto.SysModelsDTO toSysModelsDTO(ProviderDTO dto) {
        com.liangshou.service.dto.SysModelsDTO sysModelsDTO = new com.liangshou.service.dto.SysModelsDTO();
        sysModelsDTO.setModelProviderId(dto.getModelProviderId());
        sysModelsDTO.setProviderName(dto.getProviderName());
        sysModelsDTO.setModelProviderType(dto.getModelProviderType());
        sysModelsDTO.setBaseUrl(dto.getBaseUrl());
        sysModelsDTO.setApiKey(dto.getApiKey());
        sysModelsDTO.setModelId(dto.getModelId());
        sysModelsDTO.setModelName(dto.getModelName());
        sysModelsDTO.setModelsJson(dto.getModelsJson());
        return sysModelsDTO;
    }
}
