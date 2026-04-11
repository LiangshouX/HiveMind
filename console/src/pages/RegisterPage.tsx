import { LockOutlined, SolutionOutlined, UserAddOutlined, UserOutlined } from "@ant-design/icons";
import { Button, Form, Input, Typography, message } from "antd";
import { Link, useNavigate } from "react-router-dom";
import { AuthLayout } from "../components/auth/AuthLayout";
import { useAuth } from "../providers/AuthProvider";
import type { RegisterPayload } from "../types";

const { Text } = Typography;

export function RegisterPage() {
  const navigate = useNavigate();
  const { register } = useAuth();
  const [messageApi, contextHolder] = message.useMessage();

  const handleFinish = async (values: RegisterPayload) => {
    try {
      await register(values);
      messageApi.success("注册成功，请使用新账号登录");
      navigate("/login", { replace: true });
    } catch (error) {
      messageApi.error(error instanceof Error ? error.message : "注册失败");
    }
  };

  return (
    <>
      {contextHolder}
      <AuthLayout
        title="创建账号"
        subtitle="userId 与 password 为必填项；昵称可选，未填写时系统自动使用 userId。"
        footer={
          <Text className="auth-footer-text">
            已有账号？<Link to="/login">返回登录</Link>
          </Text>
        }
      >
        <Form layout="vertical" size="large" onFinish={(values) => void handleFinish(values)}>
          <Form.Item
            label="userId"
            name="userId"
            rules={[
              { required: true, message: "请输入 userId" },
              { min: 3, message: "userId 至少 3 个字符" },
            ]}
          >
            <Input prefix={<UserOutlined />} placeholder="用于登录与身份识别" />
          </Form.Item>
          <Form.Item label="昵称" name="nickname">
            <Input prefix={<SolutionOutlined />} placeholder="选填，默认与 userId 一致" />
          </Form.Item>
          <Form.Item
            label="密码"
            name="password"
            rules={[
              { required: true, message: "请输入密码" },
              { min: 6, message: "密码至少 6 位" },
            ]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="请输入密码" />
          </Form.Item>
          <Button
            type="primary"
            htmlType="submit"
            icon={<UserAddOutlined />}
            block
            className="auth-submit-button"
          >
            注册并前往登录
          </Button>
        </Form>
      </AuthLayout>
    </>
  );
}
