import { LinkOutlined, RobotOutlined } from "@ant-design/icons";
import { Select, Space, Spin, Typography } from "antd";
import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getJson } from "../../services/http";

const { Text } = Typography;

interface ActiveModel {
  providerId: string;
  providerName: string;
  modelId: string;
  modelName: string;
  modelDisplayName?: string;
}

/** 后端返回的嵌套结构 */
interface ActiveModelsResponse {
  providers: Array<{
    providerId: string;
    providerName: string;
    models: Array<{
      modelId: string;
      modelName: string;
    }>;
  }>;
}

interface ModelSelectorProps {
  selectedModelId?: string;
  onSelect?: (providerId: string, modelId: string) => void;
}

export function ModelSelector({ selectedModelId, onSelect }: ModelSelectorProps) {
  const [models, setModels] = useState<ActiveModel[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    getJson<ActiveModelsResponse>("/tdagent/active-models")
      .then((resp) => {
        if (!cancelled) {
          // 将嵌套结构展平为扁平数组
          const flat: ActiveModel[] = (resp.providers ?? []).flatMap((p) =>
            (p.models ?? []).map((m) => ({
              providerId: p.providerId,
              providerName: p.providerName,
              modelId: m.modelId,
              modelName: m.modelName,
            })),
          );
          setModels(flat);
          setError(null);
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : "加载模型列表失败");
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const handleChange = useCallback(
    (value: string) => {
      if (!onSelect) return;
      const selected = models.find((m) => m.modelId === value);
      if (selected) {
        onSelect(selected.providerId, selected.modelId);
      }
    },
    [models, onSelect],
  );

  if (loading) {
    return (
      <Spin size="small">
        <div style={{ minWidth: 160, height: 32 }} />
      </Spin>
    );
  }

  if (error || models.length === 0) {
    return (
      <Link
        to="/model-config"
        style={{
          display: "inline-flex",
          alignItems: "center",
          gap: 6,
          padding: "4px 12px",
          borderRadius: 8,
          border: "1px dashed var(--td-border-color)",
          color: "var(--td-text-tertiary)",
          fontSize: 13,
          textDecoration: "none",
          whiteSpace: "nowrap",
        }}
      >
        <LinkOutlined />
        请先配置模型
      </Link>
    );
  }

  return (
    <Select
      value={selectedModelId || models[0]?.modelId}
      onChange={handleChange}
      style={{
        minWidth: 220,
        background: "var(--td-bg-container)",
        borderColor: "var(--td-border-color)",
        color: "var(--td-text-base)",
        borderRadius: 8,
      }}
      placeholder="选择模型"
      popupMatchSelectWidth={false}
      optionLabelProp="label"
      dropdownStyle={{
        backgroundColor: "var(--td-bg-container)",
        border: "1px solid var(--td-border-color)",
        boxShadow: "var(--td-shadow-elevated)",
      }}
      options={models.map((m) => ({
        value: m.modelId,
        label: (
          <Space size={4}>
            <RobotOutlined style={{ color: "var(--td-highlight)" }} />
            <Text style={{ color: "var(--td-text-base)" }}>
              {m.providerName} &gt; {m.modelDisplayName || m.modelName}
            </Text>
          </Space>
        ),
      }))}
      optionRender={(option) => {
        const model = models.find((m) => m.modelId === option.value);
        if (!model) return option.label;
        return (
          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <RobotOutlined style={{ color: "var(--td-highlight)", fontSize: 14 }} />
            <div>
              <Text style={{ color: "var(--td-text-base)", fontSize: 13, display: "block" }}>
                {model.modelDisplayName || model.modelName}
              </Text>
              <Text style={{ color: "var(--td-text-tertiary)", fontSize: 11 }}>
                {model.providerName}
              </Text>
            </div>
          </div>
        );
      }}
    />
  );
}
