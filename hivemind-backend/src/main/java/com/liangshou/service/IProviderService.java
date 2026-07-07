package com.liangshou.service;

import com.liangshou.common.utils.PageResult;
import com.liangshou.service.dto.ConnectionTestResult;
import com.liangshou.service.dto.ProviderDTO;
import com.liangshou.service.vo.ProviderVO;

import java.util.List;

/**
 * Provider 管理服务接口
 * <p>提供模型供应商的完整管理能力，包括 CRUD、激活/停用、模型选择、连接测试等。</p>
 */
public interface IProviderService {

    /**
     * 分页查询当前用户的 Provider 列表
     *
     * @param current 当前页码（从 1 开始）
     * @param size    每页大小
     * @return 分页结果
     */
    PageResult<ProviderVO> list(int current, int size);

    /**
     * 根据主键查询 Provider 详情
     *
     * @param id 主键 ID
     * @return Provider 详情
     */
    ProviderVO getById(Long id);

    /**
     * 创建 Provider
     *
     * @param dto 创建数据
     * @return 创建后的 Provider
     */
    ProviderVO create(ProviderDTO dto);

    /**
     * 更新 Provider
     *
     * @param id  主键 ID
     * @param dto 更新数据
     * @return 更新后的 Provider
     */
    ProviderVO update(Long id, ProviderDTO dto);

    /**
     * 删除 Provider
     *
     * @param id 主键 ID
     * @return 操作是否成功
     */
    boolean delete(Long id);

    /**
     * 激活 Provider
     *
     * @param id 主键 ID
     * @return 操作是否成功
     */
    boolean activate(Long id);

    /**
     * 停用 Provider
     * <p>如果这是最后一个激活的 Provider，则不允许停用。</p>
     *
     * @param id 主键 ID
     * @return 操作是否成功
     */
    boolean deactivate(Long id);

    /**
     * 为 Provider 选择模型
     *
     * @param id        Provider 主键 ID
     * @param modelId   模型 ID
     * @param modelName 模型显示名称
     * @return 操作是否成功
     */
    boolean selectModel(Long id, String modelId, String modelName);

    /**
     * 获取当前用户的默认模型（第一个激活 Provider 的选中模型）
     *
     * @return 默认模型信息
     */
    ProviderVO getDefaultModel();

    /**
     * 为新用户初始化内置 Provider
     * <p>从 builtin_provider.json 读取内置供应商定义，为用户创建对应的 sys_models 记录。</p>
     *
     * @return 初始化后的 Provider 列表
     */
    List<ProviderVO> initializeBuiltIn();

    /**
     * 测试 Provider 连接
     *
     * @param id Provider 主键 ID
     * @return 连接测试结果
     */
    ConnectionTestResult testConnection(Long id);

    /**
     * 通过参数直接测试连接（不需要先保存 Provider）
     *
     * @param providerId Provider 标识（可为 null）
     * @param baseUrl    API 端点 URL
     * @param apiKey     API Key（明文）
     * @return 连接测试结果
     */
    ConnectionTestResult testConnectionByParams(String providerId, String baseUrl, String apiKey);

    /**
     * 获取所有激活 Provider 的可用模型列表
     *
     * @return 激活的模型信息列表
     */
    List<ProviderVO> getActiveModels();
}
