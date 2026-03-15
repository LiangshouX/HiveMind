import React, { useState } from 'react';
import { Table, Button, Typography, Space, Modal, Form, Input, message } from 'antd';
import { PlusOutlined, DeleteOutlined, EditOutlined, EyeInvisibleOutlined, EyeTwoTone } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';

const { Title, Text } = Typography;

interface EnvVar {
  id: string;
  key: string;
  value: string;
  description: string;
}

const initialEnvVars: EnvVar[] = [
  { id: '1', key: 'TAVILY_API_KEY', value: 'tvly-****************', description: 'Tavily 搜索 API 密钥' },
  { id: '2', key: 'GITHUB_TOKEN', value: 'ghp_****************', description: 'GitHub 访问令牌' },
];

const EnvVars: React.FC = () => {
  const [envVars, setEnvVars] = useState<EnvVar[]>(initialEnvVars);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [form] = Form.useForm();
  const [editingId, setEditingId] = useState<string | null>(null);

  const handleDelete = (id: string) => {
    setEnvVars(envVars.filter(e => e.id !== id));
    message.success('已删除环境变量');
  };

  const handleEdit = (record: EnvVar) => {
    setEditingId(record.id);
    form.setFieldsValue(record);
    setIsModalOpen(true);
  };

  const handleModalOk = () => {
    form.validateFields().then(values => {
      if (editingId) {
        setEnvVars(envVars.map(e => e.id === editingId ? { ...e, ...values } : e));
        message.success('环境变量已更新');
      } else {
        const newEnv = { ...values, id: Math.random().toString(36).substr(2, 9) };
        setEnvVars([...envVars, newEnv]);
        message.success('环境变量已添加');
      }
      setIsModalOpen(false);
      form.resetFields();
      setEditingId(null);
    });
  };

  const columns: ColumnsType<EnvVar> = [
    { title: '变量名 (Key)', dataIndex: 'key', key: 'key', render: text => <Text code>{text}</Text> },
    { 
      title: '变量值 (Value)', 
      dataIndex: 'value', 
      key: 'value',
      render: (text) => (
        <Input.Password 
          value={text} 
          readOnly 
          bordered={false} 
          iconRender={visible => (visible ? <EyeTwoTone /> : <EyeInvisibleOutlined />)}
        />
      )
    },
    { title: '描述', dataIndex: 'description', key: 'description' },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space size="middle">
          <Button type="link" icon={<EditOutlined />} onClick={() => handleEdit(record)}>编辑</Button>
          <Button type="link" danger icon={<DeleteOutlined />} onClick={() => handleDelete(record.id)}>删除</Button>
        </Space>
      )
    }
  ];

  return (
    <div style={{ padding: 24, background: '#fff', minHeight: '100%', borderRadius: 8 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <div>
          <Title level={4} style={{ margin: 0 }}>环境变量</Title>
          <Text type="secondary">管理 AI 助手的工具和技能在运行时需要的环境变量及 API 密钥。</Text>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => { setIsModalOpen(true); form.resetFields(); setEditingId(null); }}>
          添加变量
        </Button>
      </div>

      <Table columns={columns} dataSource={envVars} rowKey="id" pagination={false} />

      <Modal
        title={editingId ? "编辑环境变量" : "添加环境变量"}
        open={isModalOpen}
        onOk={handleModalOk}
        onCancel={() => { setIsModalOpen(false); form.resetFields(); setEditingId(null); }}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="key" label="变量名 (Key)" rules={[{ required: true }]}>
            <Input placeholder="例如：API_KEY" />
          </Form.Item>
          <Form.Item name="value" label="变量值 (Value)" rules={[{ required: true }]}>
            <Input.Password placeholder="输入敏感信息或密钥" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input placeholder="用途说明" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default EnvVars;
