import { ToolOutlined } from "@ant-design/icons";
import { Button, Card, Drawer, Empty, Input, Space, Tag, Typography } from "antd";
import type { TextAreaProps } from "antd/es/input";
import type { ToolApproval } from "../../types";

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
  return (
    <Drawer
      title="待审批工具调用"
      open={Boolean(approvals.length)}
      onClose={() => undefined}
      width={420}
      className="approval-drawer"
      extra={
        <Space>
          <Button onClick={() => void onReject()}>驳回并继续</Button>
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
            <Card key={approval.id} className="approval-card">
              <Space direction="vertical" size={8} style={{ width: "100%" }}>
                <Space wrap>
                  <Tag color="warning">{approval.riskLevel || "HIGH"}</Tag>
                  <Tag icon={<ToolOutlined />}>{approval.toolName || "未知工具"}</Tag>
                </Space>
                <Text>{approval.reason || "该工具调用被判定为高风险，需要人工确认。"}</Text>
                {approval.toolInputJson ? (
                  <pre className="message-raw">{approval.toolInputJson}</pre>
                ) : null}
              </Space>
            </Card>
          ))}

          <Input.TextArea
            value={approvalComment}
            onChange={onApprovalCommentChange}
            autoSize={{ minRows: 3, maxRows: 6 }}
            placeholder="可填写审批备注，将随 approve / reject 请求一并提交..."
          />
        </Space>
      )}
    </Drawer>
  );
}
