import { ToolOutlined } from "@ant-design/icons";
import { Button, Card, Drawer, Empty, Input, Space, Tag, Typography } from "antd";
import type { TextAreaProps } from "antd/es/input";
import type { ToolApproval } from "../../types";
import { useTheme } from "../../providers/ThemeProvider";

const { Text } = Typography;

interface ApprovalDrawerPanelProps {
  approvals: ToolApproval[];
  approvalComment: string;
  onApprovalCommentChange: TextAreaProps["onChange"];
  onApprove: () => void | Promise<void>;
  onReject: () => void | Promise<void>;
}

export function ApprovalDrawerPanel({
  approvals,
  approvalComment,
  onApprovalCommentChange,
  onApprove,
  onReject,
}: ApprovalDrawerPanelProps) {
  const { isDarkMode } = useTheme();
  return (
    <Drawer
      title="待审批工具调用"
      open={Boolean(approvals.length)}
      onClose={() => undefined}
      width={420}
      className="approval-drawer"
      style={{ background: isDarkMode ? '#07111f' : '#ffffff' }}
      extra={
        <Space>
          <Button onClick={() => void onReject()} style={{ background: isDarkMode ? '#0d1b33' : '#f1f3f5', border: `1px solid ${isDarkMode ? '#1e3a5f' : '#dee2e6'}`, color: isDarkMode ? '#f3f7ff' : '#2c3e50' }}>驳回并继续</Button>
          <Button type="primary" onClick={() => void onApprove()}>
            通过并继续
          </Button>
        </Space>
      }
    >
      {!approvals.length ? (
        <Empty description="当前没有待审批项" />
      ) : (
        <Space direction="vertical" size={12} style={{ width: "100%" }}>
          {approvals.map((approval) => (
            <Card key={approval.id} className="approval-card" style={{ background: isDarkMode ? '#0d1b33' : '#f8f9fa', border: `1px solid ${isDarkMode ? '#1e3a5f' : '#e9ecef'}` }}>
              <Space direction="vertical" size={8} style={{ width: "100%" }}>
                <Space wrap>
                  <Tag color="warning">{approval.riskLevel || "HIGH"}</Tag>
                  <Tag icon={<ToolOutlined />}>{approval.toolName || "未知工具"}</Tag>
                </Space>
                <Text style={{ color: isDarkMode ? '#f3f7ff' : '#2c3e50' }}>{approval.reason || "该工具调用被判定为高风险，需要人工确认。"}</Text>
                {approval.toolInputJson ? (
                  <pre className="message-raw" style={{ background: isDarkMode ? '#07111f' : '#f1f3f5', border: `1px solid ${isDarkMode ? '#1e3a5f' : '#dee2e6'}`, color: isDarkMode ? '#f3f7ff' : '#2c3e50' }}>{approval.toolInputJson}</pre>
                ) : null}
              </Space>
            </Card>
          ))}

          <Input.TextArea
            value={approvalComment}
            onChange={onApprovalCommentChange}
            autoSize={{ minRows: 3, maxRows: 6 }}
            placeholder="可填写审批备注，将随 approve / reject 请求一并提交..."
            style={{ background: isDarkMode ? '#07111f' : '#ffffff', border: `1px solid ${isDarkMode ? '#1e3a5f' : '#dee2e6'}`, color: isDarkMode ? '#f3f7ff' : '#2c3e50' }}
          />
        </Space>
      )}
    </Drawer>
  );
}
