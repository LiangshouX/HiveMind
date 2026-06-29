import React, { useState } from 'react';
import { Typography, Card, Button, Row, Col, Switch, Modal, Form, Input, message, Badge } from 'antd';
import { ApiOutlined } from '@ant-design/icons';

const { Title, Text } = Typography;
const { TextArea } = Input;

interface MCPClient {
  id: string;
  name: string;
  type: 'local' | 'remote';
  config: string;
  enabled: boolean;
}

const initialClients: MCPClient[] = [
  { 
    id: '1', 
    name: 'tavily_mcp', 
    type: 'local', 
    config: JSON.stringify({
      "type": "tavily-search",
      "api_key": "tvly-****************",
      "max_results": 5
    }, null, 2),
    enabled: true 
  },
];

const MCP: React.FC = () => {
  const [clients, setClients] = useState<MCPClient[]>(initialClients);
  const [modalVisible, setModalVisible] = useState(false);
  const [currentClient, setCurrentClient] = useState<MCPClient | null>(null);
  const [form] = Form.useForm();

  const handleEdit = (client: MCPClient) => {
    setCurrentClient(client);
    form.setFieldsValue({
      ...client,
      config: client.config
    });
    setModalVisible(true);
  };

  const handleSave = () => {
    form.validateFields().then(values => {
      if (currentClient) {
        setClients(clients.map(c => c.id === currentClient.id ? { ...c, ...values } : c));
        message.success('配置已更新');
      } else {
        const newClient = {
          ...values,
          id: Math.random().toString(36).substr(2, 9),
          enabled: true,
          type: 'local'
        };
        setClients([...clients, newClient]);
        message.success('创建成功');
      }
      setModalVisible(false);
    });
  };

  const toggleEnable = (id: string, checked: boolean) => {
    setClients(clients.map(c => c.id === id ? { ...c, enabled: checked } : c));
  };

  return (
    <div style={{ height: 'calc(100vh - 120px)', display: 'flex', flexDirection: 'column' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <div>
          <Title level={4} style={{ margin: 0 }}>MCP 客户端</Title>
          <Text type="secondary">管理模型上下文协议 (MCP) 客户端以扩展智能体能力。</Text>
        </div>
        <Button type="primary" onClick={() => { setCurrentClient(null); form.resetFields(); setModalVisible(true); }}>
          创建客户端
        </Button>
      </div>

      <div style={{ flex: 1, overflowY: 'auto' }}>
        <Row gutter={[16, 16]}>
          {clients.map(client => (
            <Col xs={24} sm={12} md={8} lg={6} key={client.id}>
              <Card 
                hoverable
                style={{ height: 180, display: 'flex', flexDirection: 'column', borderColor: '#1890ff' }}
                bodyStyle={{ flex: 1, display: 'flex', flexDirection: 'column', padding: 24 }}
                onClick={() => handleEdit(client)}
              >
                <div style={{ display: 'flex', alignItems: 'center', marginBottom: 12 }}>
                  <ApiOutlined style={{ fontSize: 20, color: '#1890ff', marginRight: 8 }} />
                  <Text strong style={{ fontSize: 16 }}>{client.name}</Text>
                  <Tag color="blue" style={{ marginLeft: 8 }}>{client.type}</Tag>
                </div>
                
                <div style={{ flex: 1 }} />

                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }} onClick={e => e.stopPropagation()}>
                   <Badge status={client.enabled ? 'success' : 'default'} text={client.enabled ? '已启用' : '已禁用'} />
                   <Switch size="small" checked={client.enabled} onChange={checked => toggleEnable(client.id, checked)} />
                </div>
              </Card>
            </Col>
          ))}
        </Row>
      </div>

      <Modal
        title={currentClient ? `${currentClient.name} - Configuration` : "New MCP Client"}
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        footer={
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
             <Button danger onClick={() => setModalVisible(false)}>取消</Button>
             <Button type="primary" onClick={handleSave}>保存</Button>
          </div>
        }
        width={600}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="Name" rules={[{ required: true }]}>
            <Input disabled={!!currentClient} />
          </Form.Item>
          <Form.Item name="config" rules={[{ required: true }]}>
            <TextArea 
              rows={15} 
              style={{ fontFamily: 'monospace', backgroundColor: '#f5f5f5' }} 
              placeholder="{ ... }"
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

// Add Tag import
import { Tag } from 'antd';

export default MCP;