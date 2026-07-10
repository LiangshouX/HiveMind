/**
 * ModelConfigPage - Full page for managing model providers.
 *
 * Displays a grid of ProviderCard components with add/edit modal.
 * On mount: initializes built-in providers if none exist, then loads the list.
 */

import React, { useEffect, useRef, useState, useCallback } from "react";
import {
  Typography,
  Button,
  Row,
  Col,
  Spin,
  Empty,
  Modal,
  Form,
  Input,
  Select,
  Checkbox,
  message,
  Space,
  Tag,
  Alert,
  Tooltip,
} from "antd";
import {
  PlusOutlined,
  CloudServerOutlined,
  ApiOutlined,
  InfoCircleOutlined,
} from "@ant-design/icons";
import ProviderCard from "../../components/model/ProviderCard";
import {
  listProviders,
  createProvider,
  updateProvider,
  deleteProvider,
  activateProvider,
  deactivateProvider,
  selectModel,
  testConnection,
  initializeBuiltIn,
  getDefaultModel,
} from "../../services/providerApi";
import type { ProviderVO, ProviderDTO } from "../../services/providerApi";

const { Title, Text } = Typography;

// ── Model Selection Modal ──

interface ModelSelectModalProps {
  open: boolean;
  provider: ProviderVO | null;
  onClose: () => void;
  onConfirm: (modelId: string, modelName: string) => void;
}

interface ModelItem {
  id: string;
  name: string;
  supportsMultimodal?: boolean;
  supportsVideo?: boolean;
}

const ModelSelectModal: React.FC<ModelSelectModalProps> = ({
  open,
  provider,
  onClose,
  onConfirm,
}) => {
  const [selectedModelId, setSelectedModelId] = useState<string>("");
  const [selectedModelName, setSelectedModelName] = useState<string>("");

  useEffect(() => {
    if (provider) {
      setSelectedModelId(provider.modelId || "");
      setSelectedModelName(provider.modelName || "");
    }
  }, [provider]);

  const models: ModelItem[] = React.useMemo(() => {
    if (!provider?.modelsJson) return [];
    try {
      const parsed = JSON.parse(provider.modelsJson);
      if (!Array.isArray(parsed)) return [];
      // 支持新旧两种格式
      return parsed.map((item: unknown) => {
        if (typeof item === "string") {
          return { id: item, name: item };
        }
        return item as ModelItem;
      });
    } catch {
      return [];
    }
  }, [provider]);

  const handleOk = () => {
    if (selectedModelId && selectedModelName) {
      onConfirm(selectedModelId, selectedModelName);
    }
  };

  return (
    <Modal
      title={`选择模型 - ${provider?.providerName || ""}`}
      open={open}
      onOk={handleOk}
      onCancel={onClose}
      okText="确认选择"
      cancelText="取消"
      okButtonProps={{ disabled: !selectedModelId }}
    >
      {models.length === 0 ? (
        <Alert
          message="暂无可用模型"
          description="请先添加模型配置，或测试连接以发现可用模型"
          type="info"
          showIcon
        />
      ) : (
        <Select
          showSearch
          style={{ width: "100%" }}
          placeholder="选择一个模型"
          value={selectedModelId || undefined}
          onChange={(value) => {
            const model = models.find((m) => m.id === value);
            setSelectedModelId(value);
            setSelectedModelName(model?.name || value);
          }}
          filterOption={(input, option) =>
            (option?.label ?? "").toLowerCase().includes(input.toLowerCase())
          }
          options={models.map((m) => ({
            value: m.id,
            label: `${m.name} (${m.id})`,
          }))}
        />
      )}
    </Modal>
  );
};

// ── Main Page ──

const ModelConfigPage: React.FC = () => {
  // Data
  const [providers, setProviders] = useState<ProviderVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [defaultModel, setDefaultModel] = useState<ProviderVO | null>(null);

  // Create/Edit modal
  const [modalOpen, setModalOpen] = useState(false);
  const [editingProvider, setEditingProvider] = useState<ProviderVO | null>(
    null,
  );
  const [form] = Form.useForm();
  const [saving, setSaving] = useState(false);

  // Model selection modal
  const [modelModalOpen, setModelModalOpen] = useState(false);
  const [modelModalProvider, setModelModalProvider] =
    useState<ProviderVO | null>(null);

  // Connection test
  const [testing, setTesting] = useState(false);

  // 防止 React StrictMode 双重挂载导致 initializeBuiltIn 被并发调用两次
  const initDoneRef = useRef(false);

  // ── Data Loading ──

  const loadProviders = useCallback(async () => {
    setLoading(true);
    try {
      const result = await listProviders(1, 100);
      setProviders(result.records || []);
    } catch {
      message.error("加载 Provider 列表失败");
    } finally {
      setLoading(false);
    }
  }, []);

  const loadDefaultModel = useCallback(async () => {
    try {
      const dm = await getDefaultModel();
      setDefaultModel(dm);
    } catch {
      // no default model configured yet
    }
  }, []);

  // Initialize built-in providers if none exist, then load
  useEffect(() => {
    if (initDoneRef.current) return; // React StrictMode 双重挂载保护
    initDoneRef.current = true;

    const init = async () => {
      setLoading(true);
      try {
        const result = await listProviders(1, 100);
        if (!result.records || result.records.length === 0) {
          await initializeBuiltIn();
        }
        await loadProviders();
        await loadDefaultModel();
      } catch {
        message.error("初始化 Provider 失败");
      } finally {
        setLoading(false);
      }
    };
    init();
  }, [loadProviders, loadDefaultModel]);

  // ── Actions ──

  const handleCreate = () => {
    setEditingProvider(null);
    form.resetFields();
    form.setFieldsValue({ modelProviderType: "CUSTOM" });
    setModalOpen(true);
  };

  const handleEdit = (provider: ProviderVO) => {
    setEditingProvider(provider);

    // 解析模型列表
    let models: Array<{ id: string; name: string; supportsMultimodal?: boolean; supportsVideo?: boolean }> = [];
    try {
      if (provider.modelsJson) {
        const parsed = JSON.parse(provider.modelsJson);
        if (Array.isArray(parsed)) {
          models = parsed.map((item: unknown) => {
            if (typeof item === "string") {
              return { id: item, name: item, supportsMultimodal: false, supportsVideo: false };
            }
            return item as { id: string; name: string; supportsMultimodal?: boolean; supportsVideo?: boolean };
          });
        }
      }
    } catch {
      // ignore parse error
    }

    form.setFieldsValue({
      providerName: provider.providerName,
      modelProviderId: provider.modelProviderId,
      modelProviderType: provider.modelProviderType,
      baseUrl: provider.baseUrl,
      apiKey: "", // do not fill real key
      models,
      modelId: provider.modelId,
      modelName: provider.modelName,
    });
    setModalOpen(true);
  };

  const handleDelete = async (provider: ProviderVO) => {
    try {
      await deleteProvider(provider.id);
      message.success("Provider 已删除");
      await loadProviders();
      await loadDefaultModel();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "删除失败";
      message.error(msg);
    }
  };

  const handleActivate = async (provider: ProviderVO) => {
    try {
      await activateProvider(provider.id);
      message.success(`${provider.providerName} 已激活`);
      await loadProviders();
      await loadDefaultModel();
    } catch {
      message.error("激活失败");
    }
  };

  const handleDeactivate = async (provider: ProviderVO) => {
    try {
      await deactivateProvider(provider.id);
      message.success(`${provider.providerName} 已停用`);
      await loadProviders();
      await loadDefaultModel();
    } catch {
      message.error("停用失败");
    }
  };

  const handleSelectModel = (provider: ProviderVO) => {
    setModelModalProvider(provider);
    setModelModalOpen(true);
  };

  const handleModelConfirm = async (modelId: string, modelName: string) => {
    if (!modelModalProvider) return;
    try {
      await selectModel(modelModalProvider.id, modelId, modelName);
      message.success(`已选择模型: ${modelName}`);
      setModelModalOpen(false);
      await loadProviders();
      await loadDefaultModel();
    } catch {
      message.error("选择模型失败");
    }
  };

  // ── Create / Update ──

  const handleSave = async () => {
    try {
      const values = await form.validateFields();
      setSaving(true);

      const dto: ProviderDTO = {
        providerName: values.providerName,
        modelProviderId: values.modelProviderId,
        modelProviderType: values.modelProviderType,
        baseUrl: values.baseUrl,
      };

      // Only include apiKey if user entered a new one
      if (values.apiKey && values.apiKey.trim()) {
        dto.apiKey = values.apiKey.trim();
      }

      // 处理模型列表：转换为 modelsJson 格式
      if (values.models && Array.isArray(values.models)) {
        const modelsJson = values.models
          .filter((m: { id?: string; name?: string }) => m.id && m.name)
          .map((m: { id: string; name: string; supportsMultimodal?: boolean; supportsVideo?: boolean }) => ({
            id: m.id,
            name: m.name,
            supportsMultimodal: m.supportsMultimodal || false,
            supportsVideo: m.supportsVideo || false,
          }));
        dto.modelsJson = JSON.stringify(modelsJson);

        // 如果没有选择模型，默认选择第一个
        if (modelsJson.length > 0 && !values.modelId) {
          dto.modelId = modelsJson[0].id;
          dto.modelName = modelsJson[0].name;
        }
      }

      if (editingProvider) {
        await updateProvider(editingProvider.id, dto);
        message.success("Provider 已更新");
      } else {
        await createProvider(dto);
        message.success("Provider 已创建");
      }

      setModalOpen(false);
      await loadProviders();
      await loadDefaultModel();
    } catch (err: unknown) {
      if (err && typeof err === "object" && "errorFields" in err) {
        message.error("请检查表单输入");
      } else {
        const msg = err instanceof Error ? err.message : "保存失败";
        message.error(msg);
      }
    } finally {
      setSaving(false);
    }
  };

  // ── Test Connection ──

  const handleTestConnection = async () => {
    try {
      const values = await form.validateFields(["baseUrl"]);
      setTesting(true);

      const result = await testConnection({
        baseUrl: values.baseUrl,
        apiKey: values.apiKey || undefined,
        modelProviderId: values.modelProviderId || undefined,
      });

      if (result.reachable) {
        const modelCount = result.discoveredModels?.length || 0;
        message.success(
          `连接成功! 延迟 ${result.latencyMs}ms, 发现 ${modelCount} 个模型`,
        );

        // 如果发现了模型，询问用户是否要添加到模型列表
        if (modelCount > 0) {
          Modal.confirm({
            title: "发现可用模型",
            content: `发现 ${modelCount} 个模型，是否将它们添加到模型列表中？`,
            okText: "添加",
            cancelText: "跳过",
            onOk: () => {
              // 获取当前表单中的模型列表
              const currentModels = form.getFieldValue("models") || [];
              const existingIds = new Set(currentModels.map((m: { id: string }) => m.id));

              // 过滤掉已存在的模型
              const newModels = (result.discoveredModels || [])
                .filter((m) => !existingIds.has(m.id))
                .map((m) => ({
                  id: m.id,
                  name: m.name,
                  supportsMultimodal: false,
                  supportsVideo: false,
                }));

              if (newModels.length > 0) {
                form.setFieldValue("models", [...currentModels, ...newModels]);
                message.success(`已添加 ${newModels.length} 个新模型`);
              } else {
                message.info("所有模型已存在");
              }
            },
          });
        }
      } else {
        message.warning(`连接失败: ${result.errorMessage || "未知错误"}`);
      }
    } catch (err: unknown) {
      if (err && typeof err === "object" && "errorFields" in err) {
        // form validation error, skip
      } else {
        const msg = err instanceof Error ? err.message : "测试连接失败";
        message.error(msg);
      }
    } finally {
      setTesting(false);
    }
  };

  // ── Render ──

  return (
    <div
      style={{
        height: "calc(100vh - 120px)",
        display: "flex",
        flexDirection: "column",
        padding: 16,
      }}
    >
      {/* Header */}
      <div
        style={{
          marginBottom: 24,
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
        }}
      >
        <div>
          <Title
            level={4}
            style={{ margin: 0, display: "flex", alignItems: "center", gap: 8 }}
          >
            <CloudServerOutlined />
            模型配置
          </Title>
          <Space>
            <Text type="secondary">
              管理 AI 模型供应商配置，包括 API 密钥、模型选择和连接测试。
            </Text>
            <Tooltip title="Provider 定义了 AI 模型的接入方式。系统内置 Provider 不可删除，但可以停用。">
              <InfoCircleOutlined
                style={{ color: "var(--td-text-tertiary)", cursor: "help" }}
              />
            </Tooltip>
          </Space>
        </div>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={handleCreate}
          size="large"
        >
          添加 Provider
        </Button>
      </div>

      {/* Default model banner */}
      {defaultModel && (
        <Alert
          message={
            <Space>
              <Text strong>当前默认模型:</Text>
              <Tag color="gold">{defaultModel.providerName}</Tag>
              <Text strong style={{ color: "var(--td-highlight)" }}>
                {defaultModel.modelName || "未选择模型"}
              </Text>
            </Space>
          }
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
        />
      )}

      {/* Provider grid */}
      <div style={{ flex: 1, overflowY: "auto" }}>
        {loading ? (
          <div style={{ textAlign: "center", padding: "100px 0" }}>
            <Spin size="large" tip="加载中..." />
          </div>
        ) : providers.length === 0 ? (
          <Empty
            description="暂无 Provider 配置"
            image={Empty.PRESENTED_IMAGE_SIMPLE}
          >
            <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
              添加第一个 Provider
            </Button>
          </Empty>
        ) : (
          <Row gutter={[16, 16]}>
            {providers.map((p) => (
              <Col xs={24} sm={12} md={8} lg={6} key={p.id}>
                <ProviderCard
                  provider={p}
                  onEdit={handleEdit}
                  onDelete={handleDelete}
                  onActivate={handleActivate}
                  onDeactivate={handleDeactivate}
                  onSelectModel={handleSelectModel}
                />
              </Col>
            ))}

            {/* Add button card */}
            <Col xs={24} sm={12} md={8} lg={6}>
              <Button
                type="text"
                onClick={handleCreate}
                style={{
                  height: "100%",
                  minHeight: 260,
                  border: "2px dashed var(--td-border-color)",
                  borderRadius: 8,
                  display: "flex",
                  flexDirection: "column",
                  alignItems: "center",
                  justifyContent: "center",
                  gap: 8,
                  color: "var(--td-text-tertiary)",
                }}
              >
                <PlusOutlined style={{ fontSize: 32 }} />
                <span>添加新 Provider</span>
              </Button>
            </Col>
          </Row>
        )}
      </div>

      {/* Create / Edit Modal */}
      <Modal
        title={
          editingProvider ? `编辑 Provider - ${editingProvider.providerName}` : "添加 Provider"
        }
        open={modalOpen}
        onOk={handleSave}
        onCancel={() => setModalOpen(false)}
        okText={editingProvider ? "保存" : "创建"}
        cancelText="取消"
        confirmLoading={saving}
        width={600}
        destroyOnClose
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item
            name="providerName"
            label="显示名称"
            rules={[{ required: true, message: "请输入 Provider 名称" }]}
          >
            <Input placeholder="例如: DashScope, OpenAI, DeepSeek" />
          </Form.Item>

          <Form.Item
            name="modelProviderId"
            label="Provider 标识"
            rules={[{ required: true, message: "请输入 Provider 标识" }]}
          >
            <Input
              placeholder="例如: dashscope, openai, deepseek"
              disabled={!!editingProvider}
            />
          </Form.Item>

          <Form.Item name="modelProviderType" label="类型">
            <Select>
              <Select.Option value="SYSTEM">SYSTEM (系统内置)</Select.Option>
              <Select.Option value="CUSTOM">CUSTOM (自定义)</Select.Option>
              <Select.Option value="LOCAL">LOCAL (本地)</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="baseUrl"
            label={
              <Space>
                API 端点
                <Tooltip title="Provider 的 API 基础 URL，例如 https://dashscope.aliyuncs.com/compatible-mode/v1">
                  <InfoCircleOutlined style={{ color: "var(--td-text-tertiary)" }} />
                </Tooltip>
              </Space>
            }
            rules={[{ required: true, message: "请输入 API 端点" }]}
          >
            <Input placeholder="https://api.example.com/v1" />
          </Form.Item>

          <Form.Item
            name="apiKey"
            label={
              <Space>
                API Key
                {editingProvider && (
                  <Text type="secondary" style={{ fontSize: 12 }}>
                    (留空则保持不变)
                  </Text>
                )}
              </Space>
            }
          >
            <Input.Password
              placeholder={editingProvider ? "sk-**** (留空保持不变)" : "sk-..."}
            />
          </Form.Item>

          <Button
            icon={<ApiOutlined />}
            onClick={handleTestConnection}
            loading={testing}
            style={{ marginBottom: 16 }}
          >
            测试连接
          </Button>

          {/* 模型列表配置 */}
          <div style={{ marginTop: 16 }}>
            <Text strong style={{ display: "block", marginBottom: 8 }}>
              模型配置
            </Text>
            <Text type="secondary" style={{ display: "block", marginBottom: 12, fontSize: 12 }}>
              配置该 Provider 支持的模型。模型 ID 用于 API 调用，显示名称用于界面展示。
            </Text>
            <Form.List name="models">
              {(fields, { add, remove }) => (
                <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
                  {fields.map((field, index) => (
                    <div
                      key={field.key}
                      style={{
                        display: "flex",
                        flexDirection: "column",
                        gap: 8,
                        padding: "12px",
                        border: "1px solid var(--td-border-color)",
                        borderRadius: "8px",
                        background: "var(--td-bg-container)",
                      }}
                    >
                      <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                        <span style={{ color: "var(--td-text-secondary)", fontSize: 12, minWidth: 20 }}>
                          #{index + 1}
                        </span>
                        <Form.Item
                          {...field}
                          name={[field.name, "id"]}
                          rules={[{ required: true, message: "请输入模型 ID" }]}
                          style={{ marginBottom: 0, flex: 1 }}
                        >
                          <Input
                            placeholder="模型 ID (例如: qwen-turbo)"
                            style={{ flex: 1 }}
                          />
                        </Form.Item>
                        <Form.Item
                          {...field}
                          name={[field.name, "name"]}
                          rules={[{ required: true, message: "请输入模型名称" }]}
                          style={{ marginBottom: 0, flex: 1 }}
                        >
                          <Input
                            placeholder="显示名称 (例如: 通义千问-Turbo)"
                            style={{ flex: 1 }}
                          />
                        </Form.Item>
                        <Button
                          type="text"
                          danger
                          size="small"
                          onClick={() => remove(field.name)}
                        >
                          删除
                        </Button>
                      </div>
                      <div style={{ display: "flex", gap: 16, paddingLeft: 28 }}>
                        <Tooltip title="启用后该模型可处理图片等多模态内容">
                          <Form.Item
                            {...field}
                            name={[field.name, "supportsMultimodal"]}
                            valuePropName="checked"
                            style={{ marginBottom: 0 }}
                          >
                            <Checkbox>多模态支持</Checkbox>
                          </Form.Item>
                        </Tooltip>
                        <Tooltip title="启用后该模型可处理视频内容">
                          <Form.Item
                            {...field}
                            name={[field.name, "supportsVideo"]}
                            valuePropName="checked"
                            style={{ marginBottom: 0 }}
                          >
                            <Checkbox>视频支持</Checkbox>
                          </Form.Item>
                        </Tooltip>
                      </div>
                    </div>
                  ))}
                  <Button
                    type="dashed"
                    onClick={() => add({ id: "", name: "", supportsMultimodal: false, supportsVideo: false })}
                    icon={<PlusOutlined />}
                    block
                  >
                    添加模型
                  </Button>
                </div>
              )}
            </Form.List>
          </div>
        </Form>
      </Modal>

      {/* Model Selection Modal */}
      <ModelSelectModal
        open={modelModalOpen}
        provider={modelModalProvider}
        onClose={() => setModelModalOpen(false)}
        onConfirm={handleModelConfirm}
      />
    </div>
  );
};

export default ModelConfigPage;
