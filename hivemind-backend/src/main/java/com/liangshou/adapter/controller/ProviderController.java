package com.liangshou.adapter.controller;

import com.liangshou.common.utils.PageResult;
import com.liangshou.common.utils.Result;
import com.liangshou.service.IProviderService;
import com.liangshou.service.dto.ConnectionTestResult;
import com.liangshou.service.dto.ProviderDTO;
import com.liangshou.service.vo.ProviderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Provider 管理控制器
 * <p>提供模型供应商的完整管理接口，包括 CRUD、激活/停用、模型选择、连接测试等。</p>
 */
@Tag(name = "Provider API", description = "模型供应商管理相关接口")
@RestController
@RequestMapping("/api/v1/providers")
@RequiredArgsConstructor
public class ProviderController {

    private final IProviderService providerService;

    /**
     * 分页查询 Provider 列表
     *
     * @param current 当前页码（默认 1）
     * @param size    每页大小（默认 10）
     * @return 分页结果
     */
    @Operation(summary = "分页查询", description = "分页获取当前用户的 Provider 列表")
    @GetMapping
    public Result<PageResult<ProviderVO>> list(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(providerService.list(current, size));
    }

    /**
     * 查询 Provider 详情
     *
     * @param id 主键 ID
     * @return Provider 详情
     */
    @Operation(summary = "查询详情", description = "根据主键获取 Provider 详情")
    @GetMapping("/{id}")
    public Result<ProviderVO> getById(@PathVariable Long id) {
        return Result.success(providerService.getById(id));
    }

    /**
     * 创建 Provider
     *
     * @param dto 创建数据
     * @return 创建后的 Provider
     */
    @Operation(summary = "创建 Provider", description = "创建一个新的模型供应商配置")
    @PostMapping
    public Result<ProviderVO> create(@RequestBody ProviderDTO dto) {
        return Result.success(providerService.create(dto));
    }

    /**
     * 更新 Provider
     *
     * @param id  主键 ID
     * @param dto 更新数据
     * @return 更新后的 Provider
     */
    @Operation(summary = "更新 Provider", description = "根据主键更新 Provider 配置")
    @PutMapping("/{id}")
    public Result<ProviderVO> update(@PathVariable Long id, @RequestBody ProviderDTO dto) {
        return Result.success(providerService.update(id, dto));
    }

    /**
     * 删除 Provider
     *
     * @param id 主键 ID
     * @return 操作结果
     */
    @Operation(summary = "删除 Provider", description = "根据主键删除 Provider（系统内置不可删除）")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(providerService.delete(id));
    }

    /**
     * 激活/停用 Provider
     *
     * @param id     主键 ID
     * @param active true=激活, false=停用
     * @return 操作结果
     */
    @Operation(summary = "激活/停用 Provider", description = "激活或停用指定的 Provider")
    @PatchMapping("/{id}/activation")
    public Result<Boolean> activate(@PathVariable Long id,
                                    @RequestParam(defaultValue = "true") boolean active) {
        if (active) {
            return Result.success(providerService.activate(id));
        } else {
            return Result.success(providerService.deactivate(id));
        }
    }

    /**
     * 选择模型
     *
     * @param id        Provider 主键 ID
     * @param modelId   模型 ID
     * @param modelName 模型显示名称
     * @return 操作结果
     */
    @Operation(summary = "选择模型", description = "为 Provider 选择当前使用的模型")
    @PatchMapping("/{id}/select-model")
    public Result<Boolean> selectModel(@PathVariable Long id,
                                       @RequestParam String modelId,
                                       @RequestParam String modelName) {
        return Result.success(providerService.selectModel(id, modelId, modelName));
    }

    /**
     * 测试 Provider 连接
     *
     * @param dto 包含 baseUrl 和 apiKey 的测试请求（可包含 modelProviderId）
     * @return 连接测试结果
     */
    @Operation(summary = "测试连接", description = "测试 Provider 的连接可用性及发现模型列表")
    @PostMapping("/test-connection")
    public Result<ConnectionTestResult> testConnection(@RequestBody ProviderDTO dto) {
        if (dto.getBaseUrl() == null || dto.getBaseUrl().isBlank()) {
            return Result.error(400, "缺少 baseUrl");
        }
        ConnectionTestResult result = providerService.testConnectionByParams(
                dto.getModelProviderId(), dto.getBaseUrl(), dto.getApiKey());
        return Result.success(result);
    }

    /**
     * 获取默认模型
     *
     * @return 默认模型信息
     */
    @Operation(summary = "获取默认模型", description = "获取当前用户默认激活的 Provider 及其选中模型")
    @GetMapping("/default-model")
    public Result<ProviderVO> getDefaultModel() {
        return Result.success(providerService.getDefaultModel());
    }

    /**
     * 初始化内置 Provider
     *
     * @return 初始化后的 Provider 列表
     */
    @Operation(summary = "初始化内置 Provider", description = "为当前用户初始化系统内置的 Provider 配置")
    @PostMapping("/initialize-built-in")
    public Result<List<ProviderVO>> initializeBuiltIn() {
        return Result.success(providerService.initializeBuiltIn());
    }
}
