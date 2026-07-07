import { MinusCircleOutlined, PlusOutlined } from "@ant-design/icons";
import { Button, Form, Input, Modal, Select, Space } from "antd";
import type { InputRef } from "antd";
import { useEffect, useRef, useState } from "react";

export type ProviderType = "SYSTEM" | "CUSTOM" | "LOCAL";

export interface ProviderFormData {
  id?: string;
  name: string;
  type: ProviderType;
  baseUrl: string;
  apiKey?: string;
  models: string[];
}

interface ProviderFormProps {
  visible: boolean;
  provider?: ProviderFormData;
  onSubmit: (values: ProviderFormData) => void | Promise<void>;
  onCancel: () => void;
}

export function ProviderForm({ visible, provider, onSubmit, onCancel }: ProviderFormProps) {
  const [form] = Form.useForm<ProviderFormData>();
  const [loading, setLoading] = useState(false);
  const apiKeyInputRef = useRef<InputRef>(null);
  const [apiKeyCleared, setApiKeyCleared] = useState(false);

  const isEdit = Boolean(provider?.id);
  const isApiKeyMasked = isEdit && !apiKeyCleared;

  useEffect(() => {
    if (visible) {
      if (provider) {
        form.setFieldsValue({
          ...provider,
          apiKey: isApiKeyMasked ? "••••••••" : undefined,
        });
      } else {
        form.resetFields();
      }
      setApiKeyCleared(false);
    }
  }, [visible, provider, isApiKeyMasked, form]);

  const handleClearApiKey = () => {
    setApiKeyCleared(true);
    form.setFieldsValue({ apiKey: "" });
    setTimeout(() => {
      apiKeyInputRef.current?.focus();
    }, 0);
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      // 如果是编辑模式且 API key 没有被重新输入，不要提交 apiKey
      if (isEdit && !apiKeyCleared) {
        await onSubmit({ ...values, id: provider?.id, apiKey: undefined });
      } else {
        await onSubmit(values);
      }
    } catch {
      // validation failed, do nothing
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      title={isEdit ? "编辑 Provider" : "添加 Provider"}
      open={visible}
      onCancel={onCancel}
      width={640}
      styles={{
        mask: {
          backgroundColor: "rgba(0, 0, 0, 0.45)",
        },
        body: {
          paddingTop: 12,
          background: "var(--td-bg-container)",
          color: "var(--td-text-base)",
        },
      }}
      footer={
        <Space>
          <Button onClick={onCancel}>取消</Button>
          <Button type="primary" loading={loading} onClick={() => void handleSubmit()}>
            {isEdit ? "保存" : "添加"}
          </Button>
        </Space>
      }
    >
      <Form form={form} layout="vertical" initialValues={{ type: "CUSTOM" as ProviderType, models: [] }}>
        <Form.Item
          name="name"
          label={<span style={{ color: "var(--td-text-base)" }}>名称</span>}
          rules={[{ required: true, message: "请输入 Provider 名称" }]}
        >
          <Input placeholder="例如: DashScope / DeepSeek" />
        </Form.Item>

        <Form.Item
          name="type"
          label={<span style={{ color: "var(--td-text-base)" }}>类型</span>}
          rules={[{ required: true, message: "请选择类型" }]}
        >
          <Select
            options={[
              { value: "SYSTEM", label: "系统内置" },
              { value: "CUSTOM", label: "自定义" },
              { value: "LOCAL", label: "本地部署" },
            ]}
          />
        </Form.Item>

        <Form.Item
          name="baseUrl"
          label={<span style={{ color: "var(--td-text-base)" }}>URL</span>}
          rules={[
            { required: true, message: "请输入 API 地址" },
            {
              pattern: /^https?:\/\/.+/,
              message: "请输入有效的 URL 地址（以 http:// 或 https:// 开头）",
            },
          ]}
        >
          <Input placeholder="https://api.example.com/v1" />
        </Form.Item>

        <Form.Item
          name="apiKey"
          label={
            <Space>
              <span style={{ color: "var(--td-text-base)" }}>API Key</span>
              {isEdit && !apiKeyCleared && (
                <Button type="link" size="small" onClick={handleClearApiKey} style={{ padding: 0 }}>
                  重新输入
                </Button>
              )}
            </Space>
          }
          rules={
            isApiKeyMasked
              ? []
              : [{ required: true, message: "请输入 API Key" }]
          }
        >
          <Input.Password
            ref={apiKeyInputRef}
            placeholder={isApiKeyMasked ? "已设置，点击「重新输入」修改" : "输入 API Key"}
            disabled={isApiKeyMasked}
          />
        </Form.Item>

        <Form.Item
          label={<span style={{ color: "var(--td-text-base)" }}>模型列表</span>}
          tooltip="添加该 Provider 支持的模型名称"
        >
          <Form.List name="models">
            {(fields, { add, remove }) => (
              <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
                {fields.map((field) => (
                  <div key={field.key} style={{ display: "flex", alignItems: "center", gap: 8 }}>
                    <Form.Item
                      {...field}
                      rules={[{ required: true, message: "请输入模型名称" }]}
                      style={{ marginBottom: 0, flex: 1 }}
                    >
                      <Input placeholder="例如: qwen-turbo / deepseek-chat" />
                    </Form.Item>
                    <MinusCircleOutlined
                      onClick={() => remove(field.name)}
                      style={{
                        color: "var(--td-text-tertiary)",
                        cursor: "pointer",
                        fontSize: 16,
                        flexShrink: 0,
                      }}
                    />
                  </div>
                ))}
                <Button
                  type="dashed"
                  onClick={() => add()}
                  icon={<PlusOutlined />}
                  block
                  style={{
                    borderColor: "var(--td-border-color)",
                    color: "var(--td-text-secondary)",
                  }}
                >
                  添加模型
                </Button>
              </div>
            )}
          </Form.List>
        </Form.Item>
      </Form>
    </Modal>
  );
}
