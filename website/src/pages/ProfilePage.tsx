import { ArrowLeftOutlined, LockOutlined, SaveOutlined, UserOutlined } from "@ant-design/icons";
import { Avatar, Button, Card, Form, Input, Space, Typography, message } from "antd";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../providers/AuthProvider";
import type { UpdateProfilePayload } from "../types";

const { Paragraph, Text, Title } = Typography;

export function ProfilePage() {
  const navigate = useNavigate();
  const { updateProfile, user } = useAuth();
  const [messageApi, contextHolder] = message.useMessage();
  const [form] = Form.useForm<UpdateProfilePayload>();

  const handleFinish = async (values: UpdateProfilePayload) => {
    try {
      const payload: UpdateProfilePayload = {
        nickname: values.nickname?.trim() || undefined,
        password: values.password?.trim() || undefined,
      };
      await updateProfile(payload);
      messageApi.success("个人信息已更新");
      navigate("/", { replace: true });
    } catch (error) {
      messageApi.error(error instanceof Error ? error.message : "更新失败");
    }
  };

  return (
    <>
      {contextHolder}
      <div className="profile-shell" style={{ minHeight: "100vh", background: "var(--td-bg-base)", padding: "24px" }}>
        <Card
          className="profile-card"
          bordered={false}
          style={{
            maxWidth: "900px",
            margin: "0 auto",
            background: "var(--td-bg-container)",
            border: "1px solid var(--td-border-light)",
          }}
        >
          <Space direction="vertical" size={24} style={{ width: "100%" }}>
            <Space align="center" style={{ justifyContent: "space-between", width: "100%" }}>
              <div>
                <Text
                  className="auth-card-kicker"
                  style={{ color: "var(--td-primary-text)", fontSize: "14px", fontWeight: 600, textTransform: "uppercase" }}
                >
                  Profile Center
                </Text>
                <Title level={2} className="auth-card-title" style={{ margin: "8px 0", color: "var(--td-text-base)" }}>
                  修改个人信息
                </Title>
                <Paragraph className="auth-card-subtitle" style={{ color: "var(--td-text-secondary)", margin: 0 }}>
                  当前登录身份将同步用于 JWT 鉴权、会话读取和 Agent 审批恢复。
                </Paragraph>
              </div>
              <Button icon={<ArrowLeftOutlined />} onClick={() => navigate(-1)}>
                返回
              </Button>
            </Space>

            <div className="profile-grid" style={{ display: "grid", gridTemplateColumns: "1fr 2fr", gap: "24px" }}>
              <Card
                className="profile-summary-card"
                style={{
                  background: "var(--td-surface-nested)",
                  border: "1px solid var(--td-border-light)",
                  borderRadius: "12px",
                  textAlign: "center",
                  padding: "32px",
                }}
              >
                <Avatar
                  size={80}
                  style={{
                    background: "var(--td-brand-gradient)",
                    color: "var(--td-text-inverse)",
                    fontWeight: 800,
                    fontSize: "32px",
                    marginBottom: "16px",
                  }}
                >
                  {user?.nickname?.slice(0, 1).toUpperCase()}
                </Avatar>
                <Space direction="vertical" size={12} style={{ width: "100%" }}>
                  <Text className="metric-label" style={{ color: "var(--td-text-tertiary)", fontSize: "12px" }}>
                    当前账号
                  </Text>
                  <Title level={4} className="brand-title" style={{ margin: 0, color: "var(--td-text-base)" }}>
                    {user?.nickname}
                  </Title>
                  <Text className="metric-foot" style={{ color: "var(--td-primary-text)", fontWeight: 500 }}>
                    {user?.userId}
                  </Text>
                  <Text className="metric-foot" style={{ color: "var(--td-text-tertiary)" }}>
                    角色：{user?.role}
                  </Text>
                </Space>
              </Card>

              <Card
                className="profile-form-card"
                style={{
                  background: "var(--td-surface-nested)",
                  border: "1px solid var(--td-border-light)",
                  borderRadius: "12px",
                  padding: "24px",
                }}
              >
                <Form
                  layout="vertical"
                  size="large"
                  form={form}
                  initialValues={{ nickname: user?.nickname }}
                  onFinish={(values) => void handleFinish(values)}
                  style={{ width: "100%" }}
                >
                  <Form.Item label="userId" style={{ marginBottom: "16px" }}>
                    <Input
                      prefix={<UserOutlined style={{ color: "var(--td-primary)" }} />}
                      value={user?.userId}
                      disabled
                      style={{
                        background: "var(--td-input-bg)",
                        border: "1px solid var(--td-border-color)",
                        color: "var(--td-primary-text)",
                        borderRadius: "8px",
                      }}
                    />
                  </Form.Item>
                  <Form.Item label="昵称" name="nickname" style={{ marginBottom: "16px" }}>
                    <Input
                      prefix={<UserOutlined style={{ color: "var(--td-primary)" }} />}
                      placeholder="默认显示昵称，留空则回退为 userId"
                      style={{
                        background: "var(--td-input-bg)",
                        border: "1px solid var(--td-border-color)",
                        color: "var(--td-text-base)",
                        borderRadius: "8px",
                      }}
                    />
                  </Form.Item>
                  <Form.Item
                    label="新密码"
                    name="password"
                    rules={[{ min: 6, message: "新密码至少 6 位" }]}
                    style={{ marginBottom: "24px" }}
                  >
                    <Input.Password
                      prefix={<LockOutlined style={{ color: "var(--td-primary)" }} />}
                      placeholder="不修改密码可留空"
                      style={{
                        background: "var(--td-input-bg)",
                        border: "1px solid var(--td-border-color)",
                        color: "var(--td-text-base)",
                        borderRadius: "8px",
                      }}
                    />
                  </Form.Item>
                  <Button
                    type="primary"
                    htmlType="submit"
                    icon={<SaveOutlined />}
                    className="auth-submit-button"
                    block
                    style={{
                      height: "48px",
                      fontSize: "16px",
                      fontWeight: 600,
                      background: "var(--td-brand-gradient)",
                      border: "none",
                      color: "var(--td-text-inverse)",
                    }}
                  >
                    保存个人资料
                  </Button>
                </Form>
              </Card>
            </div>
          </Space>
        </Card>
      </div>
    </>
  );
}
