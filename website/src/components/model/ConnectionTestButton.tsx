import { ApiOutlined, CheckCircleOutlined, CloseCircleOutlined, PlusOutlined } from "@ant-design/icons";
import { Alert, Button, Checkbox, List, Space, Tag, Typography } from "antd";
import { useState } from "react";
import { postJson } from "../../services/http";

const { Text } = Typography;

interface ConnectionTestResult {
  success: boolean;
  latencyMs?: number;
  models?: string[];
  error?: string;
}

interface ConnectionTestButtonProps {
  baseUrl: string;
  apiKey?: string;
  providerType?: string;
  onModelsDiscovered?: (models: string[]) => void;
}

function mapErrorMessage(raw?: string): string {
  if (!raw) {
    return "未知错误";
  }
  const lower = raw.toLowerCase();
  if (lower.includes("invalid api key") || lower.includes("unauthorized") || lower.includes("401")) {
    return "Invalid API key — 请检查输入的密钥是否正确";
  }
  if (lower.includes("econnrefused") || lower.includes("fetch failed") || lower.includes("network")) {
    return "Cannot reach server — 无法连接到目标服务器，请检查 URL";
  }
  if (lower.includes("model not found") || lower.includes("404") || lower.includes("model")) {
    return "Model not found — 该 Provider 未提供指定模型";
  }
  return raw;
}

export function ConnectionTestButton({
  baseUrl,
  apiKey,
  providerType,
  onModelsDiscovered,
}: ConnectionTestButtonProps) {
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<ConnectionTestResult | null>(null);
  const [selectedModels, setSelectedModels] = useState<string[]>([]);
  const [expanded, setExpanded] = useState(false);

  const handleTest = async () => {
    if (!baseUrl || !apiKey) {
      setResult({
        success: false,
        error: "请先填写 URL 和 API Key",
      });
      setExpanded(true);
      return;
    }

    setLoading(true);
    setResult(null);
    setSelectedModels([]);

    try {
      const data = await postJson<ConnectionTestResult>("/tdagent/providers/test-connection", {
        baseUrl,
        apiKey,
        providerType,
      });
      setResult(data);
      setExpanded(true);
    } catch (error) {
      setResult({
        success: false,
        error: error instanceof Error ? error.message : "连接测试失败",
      });
      setExpanded(true);
    } finally {
      setLoading(false);
    }
  };

  const handleAddSelectedModels = () => {
    if (selectedModels.length > 0 && onModelsDiscovered) {
      onModelsDiscovered(selectedModels);
    }
  };

  const toggleModelSelection = (model: string, checked: boolean) => {
    setSelectedModels((prev) =>
      checked ? [...prev, model] : prev.filter((m) => m !== model),
    );
  };

  const toggleSelectAll = (checked: boolean) => {
    if (checked) {
      setSelectedModels(result?.models ?? []);
    } else {
      setSelectedModels([]);
    }
  };

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
      <Button
        icon={<ApiOutlined />}
        onClick={() => void handleTest()}
        loading={loading}
        style={{
          borderColor: "var(--td-border-color)",
          color: "var(--td-text-base)",
          background: "var(--td-bg-container)",
        }}
      >
        {loading ? "测试中..." : "测试连接"}
      </Button>

      {expanded && result && (
        <div style={{ marginTop: 4 }}>
          {result.success ? (
            <Space direction="vertical" size={8} style={{ width: "100%" }}>
              <Alert
                type="success"
                showIcon
                icon={<CheckCircleOutlined />}
                message={
                  <Space>
                    <span>连接成功</span>
                    {result.latencyMs != null && (
                      <Tag color="success" style={{ marginLeft: 4 }}>
                        {result.latencyMs}ms
                      </Tag>
                    )}
                  </Space>
                }
                style={{
                  background: "var(--td-stat-green-bg)",
                  border: "1px solid var(--td-success-color)",
                }}
              />

              {result.models && result.models.length > 0 && (
                <div
                  style={{
                    background: "var(--td-bg-container)",
                    border: "1px solid var(--td-border-color)",
                    borderRadius: 8,
                    padding: "12px 16px",
                  }}
                >
                  <div style={{ marginBottom: 8 }}>
                    <Space>
                      <Text strong style={{ color: "var(--td-text-base)" }}>
                        发现 {result.models.length} 个模型
                      </Text>
                      <Checkbox
                        checked={selectedModels.length === result.models.length}
                        indeterminate={selectedModels.length > 0 && selectedModels.length < result.models.length}
                        onChange={(e) => toggleSelectAll(e.target.checked)}
                      >
                        <Text style={{ color: "var(--td-text-secondary)" }}>全选</Text>
                      </Checkbox>
                    </Space>
                  </div>
                  <List
                    size="small"
                    dataSource={result.models}
                    style={{ maxHeight: 200, overflow: "auto" }}
                    renderItem={(model) => (
                      <List.Item style={{ padding: "4px 0", border: "none" }}>
                        <Checkbox
                          checked={selectedModels.includes(model)}
                          onChange={(e) => toggleModelSelection(model, e.target.checked)}
                        >
                          <Text style={{ color: "var(--td-text-base)" }}>{model}</Text>
                        </Checkbox>
                      </List.Item>
                    )}
                  />
                  {selectedModels.length > 0 && (
                    <Button
                      type="primary"
                      icon={<PlusOutlined />}
                      size="small"
                      onClick={() => void handleAddSelectedModels()}
                      style={{ marginTop: 8 }}
                    >
                      添加已选中的 {selectedModels.length} 个模型
                    </Button>
                  )}
                </div>
              )}
            </Space>
          ) : (
            <Alert
              type="error"
              showIcon
              icon={<CloseCircleOutlined />}
              message={mapErrorMessage(result.error)}
              style={{
                background: "var(--td-bg-container)",
                border: "1px solid var(--td-primary)",
              }}
            />
          )}
        </div>
      )}
    </div>
  );
}
