import { ArrowLeftOutlined, LockOutlined, SaveOutlined, UserOutlined } from "@ant-design/icons";
import { Button, Card, Form, Input, Space, Typography, message } from "antd";
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
    <div className="profile-shell">
      {contextHolder}
      <Card className="profile-card" bordered={false}>
        <Space direction="vertical" size={18} style={{ width: "100%" }}>
          <Space align="center" style={{ justifyContent: "space-between", width: "100%" }}>
            <div>
              <Text className="auth-card-kicker">Profile Center</Text>
              <Title level={2} className="auth-card-title">
                修改个人信息
              </Title>
              <Paragraph className="auth-card-subtitle">
                当前登录身份将同步用于 JWT 鉴权、会话读取和 Agent 审批恢复。
              </Paragraph>
            </div>
            <Button icon={<ArrowLeftOutlined />} onClick={() => navigate(-1)}>
              返回
            </Button>
          </Space>

          <div className="profile-grid">
            <Card className="profile-summary-card">
              <Space direction="vertical" size={10}>
                <Text className="metric-label">当前账号</Text>
                <Title level={4} className="brand-title">
                  {user?.nickname}
                </Title>
                <Text className="metric-foot">{user?.userId}</Text>
                <Text className="metric-foot">角色：{user?.role}</Text>
              </Space>
            </Card>

            <Card className="profile-form-card">
              <Form
                layout="vertical"
                size="large"
                form={form}
                initialValues={{ nickname: user?.nickname }}
                onFinish={(values) => void handleFinish(values)}
              >
                <Form.Item label="userId">
                  <Input prefix={<UserOutlined />} value={user?.userId} disabled />
                </Form.Item>
                <Form.Item label="昵称" name="nickname">
                  <Input prefix={<UserOutlined />} placeholder="默认显示昵称，留空则回退为 userId" />
                </Form.Item>
                <Form.Item
                  label="新密码"
                  name="password"
                  rules={[{ min: 6, message: "新密码至少 6 位" }]}
                >
                  <Input.Password prefix={<LockOutlined />} placeholder="不修改密码可留空" />
                </Form.Item>
                <Button
                  type="primary"
                  htmlType="submit"
                  icon={<SaveOutlined />}
                  className="auth-submit-button"
                  block
                >
                  保存个人资料
                </Button>
              </Form>
            </Card>
          </div>
        </Space>
      </Card>
    </div>
  );
}
