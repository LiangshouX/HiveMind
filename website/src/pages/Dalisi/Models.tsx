import React, { useState } from 'react';
import { Typography, Card, Button, Row, Col, Modal, Form, Input, Tag, message, Badge, Space } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, SettingOutlined } from '@ant-design/icons';

const { Title, Text } = Typography;

interface Provider {
  id: string;
  name: string;
  type: 'builtin' | 'custom';
  baseUrl: string;
  apiKey?: string;
  models: Model[];
  status: 'active' | 'inactive' | 'unconfigured';
}

interface Model {
  id: string;
  name: string;
  type: 'builtin' | 'custom';
}

const initialProviders: Provider[] = [
  { 
    id: 'dashscope', 
    name: 'DashScope', 
    type: 'builtin', 
    baseUrl: 'https://dashscope.aliyuncs.com/api/v1', 
    models: [
      { id: 'qwen-max', name: 'Qwen Max', type: 'builtin' },
      { id: 'qwen-plus', name: 'Qwen Plus', type: 'builtin' },
    ],
    status: 'active'
  },
  { 
    id: 'openai', 
    name: 'OpenAI', 
    type: 'builtin', 
    baseUrl: 'https://api.openai.com/v1', 
    models: [
      { id: 'gpt-4o', name: 'GPT-4o', type: 'builtin' },
      { id: 'gpt-3.5-turbo', name: 'GPT-3.5 Turbo', type: 'builtin' },
    ],
    status: 'unconfigured'
  },
  { 
    id: 'azure', 
    name: 'Azure OpenAI', 
    type: 'builtin', 
    baseUrl: '', 
    models: [],
    status: 'unconfigured'
  },
  { 
    id: 'anthropic', 
    name: 'Anthropic', 
    type: 'builtin', 
    baseUrl: 'https://api.anthropic.com', 
    models: [],
    status: 'unconfigured'
  },
];

const Models: React.FC = () => {
  const [providers, setProviders] = useState<Provider[]>(initialProviders);
  const [settingModalVisible, setSettingModalVisible] = useState(false);
  const [modelModalVisible, setModelModalVisible] = useState(false);
  const [currentProvider, setCurrentProvider] = useState<Provider | null>(null);
  const [form] = Form.useForm();
  const [modelForm] = Form.useForm();

  const handleOpenSettings = (provider: Provider) => {
    setCurrentProvider(provider);
    form.setFieldsValue(provider);
    setSettingModalVisible(true);
  };

  const handleOpenModels = (provider: Provider) => {
    setCurrentProvider(provider);
    setModelModalVisible(true);
  };

  const saveSettings = () => {
    form.validateFields().then(values => {
      if (currentProvider) {
        setProviders(providers.map(p => p.id === currentProvider.id ? { ...p, ...values, status: 'active' } : p));
        message.success(`${currentProvider.name} 配置已保存`);
        setSettingModalVisible(false);
      }
    });
  };

  const addModel = () => {
    modelForm.validateFields().then(values => {
      if (currentProvider) {
        const newModel = { ...values, type: 'custom' };
        const updatedProvider = {
          ...currentProvider,
          models: [...currentProvider.models, newModel]
        };
        setProviders(providers.map(p => p.id === currentProvider.id ? updatedProvider : p));
        setCurrentProvider(updatedProvider); // Update local state for modal re-render
        modelForm.resetFields();
        message.success('模型已添加');
      }
    });
  };

  return (
    <div style={{ height: 'calc(100vh - 120px)', display: 'flex', flexDirection: 'column' }}>
      <div style={{ marginBottom: 24 }}>
        <Title level={4} style={{ margin: 0 }}>LLM</Title>
        <Text type="secondary">从已授权的提供商中选择活动的 LLM 模型。</Text>
      </div>

      <div style={{ flex: 1, overflowY: 'auto' }}>
        <Row gutter={[16, 16]}>
          {providers.map(provider => (
            <Col xs={24} sm={12} md={12} lg={12} xl={8} key={provider.id}>
              <Card 
                hoverable 
                style={{ height: '100%' }}
                bodyStyle={{ height: '100%', display: 'flex', flexDirection: 'column' }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
                  <Space>
                    <Title level={5} style={{ margin: 0 }}>{provider.name}</Title>
                    {provider.type === 'builtin' && <Tag color="green">内置</Tag>}
                  </Space>
                  <Badge status={provider.status === 'active' ? 'success' : 'default'} text={provider.status === 'active' ? '已就绪' : '未配置'} />
                </div>

                <div style={{ flex: 1 }}>
                  <div style={{ marginBottom: 8 }}>
                    <Text type="secondary" style={{ fontSize: 12 }}>基础 URL:</Text>
                    <div style={{ fontSize: 12, color: '#666' }}>{provider.baseUrl || '未设置'}</div>
                  </div>
                  <div style={{ marginBottom: 8 }}>
                    <Text type="secondary" style={{ fontSize: 12 }}>API 密钥:</Text>
                    <div style={{ fontSize: 12, color: '#666' }}>{provider.apiKey ? '****************' : '未设置'}</div>
                  </div>
                  <div>
                    <Text type="secondary" style={{ fontSize: 12 }}>模型: </Text>
                    <Text style={{ fontSize: 12 }}>{provider.models.length} 个模型</Text>
                  </div>
                </div>

                <div style={{ marginTop: 16, display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
                   <Button size="small" icon={<SettingOutlined />} onClick={() => handleOpenModels(provider)}>模型</Button>
                   <Button size="small" type="primary" icon={<EditOutlined />} onClick={() => handleOpenSettings(provider)}>设置</Button>
                </div>
              </Card>
            </Col>
          ))}
          <Col xs={24} sm={12} md={12} lg={12} xl={8}>
            <Button type="dashed" style={{ width: '100%', height: '100%', minHeight: 200 }} icon={<PlusOutlined />}>
              添加提供商
            </Button>
          </Col>
        </Row>
      </div>

      {/* Settings Modal */}
      <Modal
        title={`${currentProvider?.name} - 配置`}
        open={settingModalVisible}
        onCancel={() => setSettingModalVisible(false)}
        onOk={saveSettings}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="baseUrl" label="Base URL">
            <Input placeholder="https://api.example.com/v1" />
          </Form.Item>
          <Form.Item name="apiKey" label="API Key">
            <Input.Password placeholder="sk-..." />
          </Form.Item>
        </Form>
      </Modal>

      {/* Models Management Modal */}
      <Modal
        title={`${currentProvider?.name} - 模型管理`}
        open={modelModalVisible}
        onCancel={() => setModelModalVisible(false)}
        footer={null}
        width={700}
      >
        <div style={{ maxHeight: 400, overflowY: 'auto', marginBottom: 24 }}>
          {currentProvider?.models.map(model => (
             <div key={model.id} style={{ display: 'flex', justifyContent: 'space-between', padding: '12px 0', borderBottom: '1px solid #f0f0f0' }}>
               <div>
                 <div style={{ fontWeight: 'bold' }}>{model.name}</div>
                 <Text type="secondary" style={{ fontSize: 12 }}>{model.id}</Text>
               </div>
               <Space>
                 {model.type === 'builtin' ? <Tag color="green">内置</Tag> : <Tag color="blue">自定义</Tag>}
                 <Button type="text" danger icon={<DeleteOutlined />} disabled={model.type === 'builtin'} />
               </Space>
             </div>
          ))}
        </div>

        <div style={{ background: '#f9f9f9', padding: 16, borderRadius: 8 }}>
           <Form form={modelForm} layout="inline" onFinish={addModel}>
             <Form.Item name="id" rules={[{ required: true, message: 'ID必填' }]} style={{ flex: 1 }}>
               <Input placeholder="模型 ID (e.g. gpt-4)" />
             </Form.Item>
             <Form.Item name="name" rules={[{ required: true, message: '名称必填' }]} style={{ flex: 1 }}>
               <Input placeholder="模型名称" />
             </Form.Item>
             <Button type="primary" htmlType="submit">添加模型</Button>
           </Form>
        </div>
      </Modal>
    </div>
  );
};

export default Models;