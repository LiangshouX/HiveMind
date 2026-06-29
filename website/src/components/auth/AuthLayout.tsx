import { LockOutlined, SafetyCertificateOutlined, UserOutlined } from "@ant-design/icons";
import { Card, Space, Tag, Typography } from "antd";

const { Paragraph, Text, Title } = Typography;

interface AuthLayoutProps {
  title: string;
  subtitle: string;
  children: React.ReactNode;
  footer?: React.ReactNode;
}

export function AuthLayout({ title, subtitle, children, footer }: AuthLayoutProps) {
  return (
    <div className="auth-shell">
      <div className="auth-hero">
        <div className="auth-hero-badge">AI Assistant</div>
        <Title className="auth-hero-title">
          让 AI Agent 的操作台具备稳定的身份、会话与权限边界
        </Title>
        <Paragraph className="auth-hero-description">
          以登录态统一驱动对话、审批和资料配置，让每个 session、工具调用与个人空间都绑定到真实用户。
        </Paragraph>

        <Space wrap size={12}>
          <Tag className="auth-hero-tag" icon={<SafetyCertificateOutlined />}>
            JWT 24 小时登录态
          </Tag>
          <Tag className="auth-hero-tag" icon={<LockOutlined />}>
            Spring Security 鉴权
          </Tag>
          <Tag className="auth-hero-tag" icon={<UserOutlined />}>
            用户资料联动
          </Tag>
        </Space>
      </div>

      <Card className="auth-card" bordered={false}>
        <div className="auth-card-header">
          <Text className="auth-card-kicker">Console Access</Text>
          <Title level={2} className="auth-card-title">
            {title}
          </Title>
          <Paragraph className="auth-card-subtitle">{subtitle}</Paragraph>
        </div>

        <div className="auth-card-body">{children}</div>
        {footer ? <div className="auth-card-footer">{footer}</div> : null}
      </Card>
    </div>
  );
}
