import { LockOutlined, LoginOutlined, UserOutlined } from "@ant-design/icons";
import { Button, Form, Input, Typography, message } from "antd";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { AuthLayout } from "../components/auth/AuthLayout";
import { useAuth } from "../providers/AuthProvider";
import type { LoginPayload } from "../types";

const { Text } = Typography;

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();
  const [messageApi, contextHolder] = message.useMessage();

  const handleFinish = async (values: LoginPayload) => {
    try {
      await login(values);
      const nextPath = (location.state as { from?: string } | null)?.from || "/";
      navigate(nextPath, { replace: true });
    } catch (error) {
      messageApi.error(error instanceof Error ? error.message : "登录失败");
    }
  };

  return (
    <>
      {contextHolder}
      <AuthLayout
        title="欢迎回来"
        subtitle="使用 userId 与密码登录你的 AI Assistant 控制台。"
        footer={
          <Text className="auth-footer-text">
            没有账号？<Link to="/register">立即注册</Link>
          </Text>
        }
      >
        <Form layout="vertical" size="large" onFinish={(values) => void handleFinish(values)}>
          <Form.Item
            label="userId"
            name="userId"
            rules={[{ required: true, message: "请输入 userId" }]}
          >
            <Input prefix={<UserOutlined />} placeholder="例如：liangshou" />
          </Form.Item>
          <Form.Item
            label="密码"
            name="password"
            rules={[{ required: true, message: "请输入密码" }]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="请输入密码" />
          </Form.Item>
          <Button
            type="primary"
            htmlType="submit"
            icon={<LoginOutlined />}
            block
            className="auth-submit-button"
          >
            登录进入控制台
          </Button>
        </Form>
      </AuthLayout>
    </>
  );
}
