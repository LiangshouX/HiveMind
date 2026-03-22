import React from 'react';
import { Typography, Card, Switch, Select, Divider, Form, Button, message } from 'antd';
import { ToolOutlined, LockOutlined, GlobalOutlined } from '@ant-design/icons';

const { Title, Text } = Typography;

const Security: React.FC = () => {
  const handleSave = () => {
    message.success('安全配置已保存');
  };

  return (
    <div style={{ padding: 24, background: '#fff', minHeight: '100%', borderRadius: 8 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <div>
          <Title level={4} style={{ margin: 0 }}>卫尉寺 (Security)</Title>
          <Text type="secondary">负责系统安全相关的配置，如访问控制、工具权限管理等。</Text>
        </div>
        <Button type="primary" onClick={handleSave}>保存配置</Button>
      </div>

      <Form layout="vertical">
        <Card title={<span><LockOutlined /> 访问控制</span>} bordered={false} style={{ marginBottom: 16, background: '#fafafa' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
            <div>
              <div style={{ fontWeight: 'bold' }}>启用身份验证 (Auth)</div>
              <Text type="secondary">要求所有 API 请求提供有效的 Bearer Token。</Text>
            </div>
            <Form.Item name="authEnabled" valuePropName="checked" initialValue={true} style={{ margin: 0 }}>
              <Switch />
            </Form.Item>
          </div>
          <Divider style={{ margin: '12px 0' }} />
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <div style={{ fontWeight: 'bold' }}>CORS 跨域访问</div>
              <Text type="secondary">允许的跨域请求来源域名配置。</Text>
            </div>
            <Form.Item name="cors" initialValue="strict" style={{ margin: 0, width: 200 }}>
              <Select>
                <Select.Option value="strict">严格模式 (仅同源)</Select.Option>
                <Select.Option value="custom">自定义域名</Select.Option>
                <Select.Option value="all">允许所有 (*)</Select.Option>
              </Select>
            </Form.Item>
          </div>
        </Card>

        <Card title={<span><ToolOutlined /> 工具权限管理 (Sandboxing)</span>} bordered={false} style={{ marginBottom: 16, background: '#fafafa' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
            <div>
              <div style={{ fontWeight: 'bold' }}>高危命令拦截</div>
              <Text type="secondary">自动拦截如 rm -rf, mkfs 等高危系统命令。</Text>
            </div>
            <Form.Item name="blockDangerous" valuePropName="checked" initialValue={true} style={{ margin: 0 }}>
              <Switch />
            </Form.Item>
          </div>
          <Divider style={{ margin: '12px 0' }} />
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <div style={{ fontWeight: 'bold' }}>容器化执行 (Docker)</div>
              <Text type="secondary">在隔离的 Docker 容器中执行代码和命令。</Text>
            </div>
            <Form.Item name="useDocker" valuePropName="checked" initialValue={false} style={{ margin: 0 }}>
              <Switch />
            </Form.Item>
          </div>
        </Card>

        <Card title={<span><GlobalOutlined /> 网络与暴露面</span>} bordered={false} style={{ background: '#fafafa' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <div style={{ fontWeight: 'bold' }}>限制外网访问</div>
              <Text type="secondary">Agent 执行网络请求时，限制仅可访问内网白名单。</Text>
            </div>
            <Form.Item name="restrictNetwork" valuePropName="checked" initialValue={false} style={{ margin: 0 }}>
              <Switch />
            </Form.Item>
          </div>
        </Card>
      </Form>
    </div>
  );
};

export default Security;
