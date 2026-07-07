/**
 * ProviderCard - Displays a single provider configuration.
 *
 * Shows provider name, type badge, activation switch, current model,
 * API key mask, and action buttons (edit/delete).
 */

import React from "react";
import { Card, Switch, Tag, Button, Popconfirm, Space, Typography } from "antd";
import {
  EditOutlined,
  DeleteOutlined,
  CheckCircleOutlined,
  StopOutlined,
} from "@ant-design/icons";
import type { ProviderVO } from "../../services/providerApi";

const { Text } = Typography;

// ── Props ──

export interface ProviderCardProps {
  provider: ProviderVO;
  onEdit: (provider: ProviderVO) => void;
  onDelete: (provider: ProviderVO) => void;
  onActivate: (provider: ProviderVO) => void;
  onDeactivate: (provider: ProviderVO) => void;
  onSelectModel: (provider: ProviderVO) => void;
}

// ── Component ──

const ProviderCard: React.FC<ProviderCardProps> = ({
  provider,
  onEdit,
  onDelete,
  onActivate,
  onDeactivate,
  onSelectModel,
}) => {
  const isActivated = provider.isProviderActivated;
  const isSystem = provider.modelProviderType === "SYSTEM";

  const handleToggleActivation = (checked: boolean) => {
    if (checked) {
      onActivate(provider);
    } else {
      onDeactivate(provider);
    }
  };

  // Parse available models from modelsJson for display
  let modelCount = 0;
  try {
    const models = JSON.parse(provider.modelsJson || "[]");
    modelCount = Array.isArray(models) ? models.length : 0;
  } catch {
    // ignore parse error
  }

  return (
    <Card
      hoverable
      style={{
        height: "100%",
        display: "flex",
        flexDirection: "column",
        opacity: isActivated ? 1 : 0.65,
      }}
      bodyStyle={{
        flex: 1,
        display: "flex",
        flexDirection: "column",
        padding: 16,
      }}
    >
      {/* Header: name + type badge */}
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "flex-start",
          marginBottom: 12,
        }}
      >
        <Text
          strong
          style={{ fontSize: 15, flex: 1, wordBreak: "break-word" }}
        >
          {provider.providerName}
        </Text>
        <Tag
          color={isSystem ? "gold" : "blue"}
          style={{ marginLeft: 8, flexShrink: 0 }}
        >
          {provider.modelProviderType}
        </Tag>
      </div>

      {/* Activation switch */}
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginBottom: 10,
        }}
      >
        <Text style={{ fontSize: 12, color: "var(--td-text-secondary)" }}>
          状态
        </Text>
        <Switch
          checked={isActivated}
          onChange={handleToggleActivation}
          checkedChildren={<CheckCircleOutlined />}
          unCheckedChildren={<StopOutlined />}
          size="small"
        />
      </div>

      {/* Current model */}
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginBottom: 10,
        }}
      >
        <Text style={{ fontSize: 12, color: "var(--td-text-secondary)" }}>
          当前模型
        </Text>
        <Text
          strong
          style={{
            fontSize: 13,
            color: isActivated ? "var(--td-highlight)" : "var(--td-text-tertiary)",
            cursor: "pointer",
          }}
          onClick={() => onSelectModel(provider)}
        >
          {provider.modelName || "未选择"}
        </Text>
      </div>

      {/* Model count */}
      {modelCount > 0 && (
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            marginBottom: 10,
          }}
        >
          <Text style={{ fontSize: 12, color: "var(--td-text-secondary)" }}>
            可用模型
          </Text>
          <Tag style={{ margin: 0 }}>{modelCount} 个</Tag>
        </div>
      )}

      {/* API Key mask */}
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginBottom: 12,
        }}
      >
        <Text style={{ fontSize: 12, color: "var(--td-text-secondary)" }}>
          API Key
        </Text>
        <Text
          code
          style={{ fontSize: 11, color: "var(--td-text-tertiary)" }}
        >
          {provider.apiKeyMask || "未配置"}
        </Text>
      </div>

      {/* Spacer */}
      <div style={{ flex: 1 }} />

      {/* Actions */}
      <div
        style={{
          borderTop: "1px solid var(--td-border-light)",
          paddingTop: 12,
          display: "flex",
          justifyContent: "flex-end",
          gap: 4,
        }}
      >
        <Space size={4}>
          <Button
            type="text"
            size="small"
            icon={<EditOutlined />}
            onClick={() => onEdit(provider)}
          >
            编辑
          </Button>
          {!isSystem && (
            <Popconfirm
              title="确定要删除此 Provider 吗？"
              description="删除后将无法恢复"
              onConfirm={() => onDelete(provider)}
              okText="确定"
              cancelText="取消"
            >
              <Button
                type="text"
                size="small"
                danger
                icon={<DeleteOutlined />}
              >
                删除
              </Button>
            </Popconfirm>
          )}
        </Space>
      </div>
    </Card>
  );
};

export default ProviderCard;
