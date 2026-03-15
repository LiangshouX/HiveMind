import React, { useState } from 'react';
import { Table, Button, Typography, Switch, Space, Modal, Form, Input, Select, message } from 'antd';
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';

const { Title, Text } = Typography;

interface MCPClient {
  id: string;
  name: string;
  url: string;
  type: string;
  enabled: boolean;
}

const initialClients: MCPClient[] = [
  { id: '1', name: '本地文件系统', url: 'stdio://local/fs', type: 'stdio', enabled: true },
  { id: '2', name: 'GitHub 仓库', url: 'https://api.github.com/mcp', type: 'sse', enabled: false },
];

const MCP: React.FC = () => {
  const [clients, setClients] = useState<MCPClient[]>(initialClients);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [form] = Form.useForm();

  const handleToggle = (id: string, checked: boolean) => {
    setClients(clients.map(c => c.id === id ? { ...c, enabled: checked } : c));
    message.success(`客户端状态已更新`);
  };

  const handleDelete = (id: string) => {
    setClients(clients.filter(c => c.id !== id));
    message.success('已删除 MCP 客户端');
  };

  const handleAdd = () => {
    form.validateFields().then(values => {
      const newClient = { ...values, id: Math.random().toString(36).substr(2, 9), enabled: true };
      setClients([...clients, newClient]);
      message.success('成功添加 MCP 客户端');
      setIsModalOpen(false);
      form.resetFields();
    });
  };

  const columns: ColumnsType<MCPClient> = [
    { title: '客户端名称', dataIndex: 'name', key: 'name', render: text => <Text strong>{text}</Text> },
    { title: '连接地址', dataIndex: 'url', key: 'url', render: text => <Text code>{text}</Text> },
    { title: '连接类型', dataIndex: 'type', key: 'type' },
    { 
      title: '启用状态', 
      dataIndex: 'enabled', 
      key: 'enabled',
      render: (enabled: boolean, record) => (
        <Switch checked={enabled} onChange={checked => handleToggle(record.id, checked)} />
      )
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space size="middle">
          <Button type="link" danger icon={<DeleteOutlined />} onClick={() => handleDelete(record.id)}>删除</Button>
        </Space>
      )
    }
  ];

  return (
    <div style={{ padding: 24, background: '#fff', minHeight: '100%', borderRadius: 8 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <div>
          <Title level={4} style={{ margin: 0 }}>MCP 客户端管理</Title>
          <Text type="secondary">配置 Model Context Protocol 客户端，连接外部数据源和工具。</Text>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setIsModalOpen(true)}>添加客户端</Button>
      </div>

      <Table columns={columns} dataSource={clients} rowKey="id" pagination={false} />

      <Modal
        title="添加 MCP 客户端"
        open={isModalOpen}
        onOk={handleAdd}
        onCancel={() => { setIsModalOpen(false); form.resetFields(); }}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="客户端名称" rules={[{ required: true }]}>
            <Input placeholder="例如：本地数据库" />
          </Form.Item>
          <Form.Item name="type" label="连接类型" rules={[{ required: true }]} initialValue="stdio">
            <Select>
              <Select.Option value="stdio">标准输入输出 (stdio)</Select.Option>
              <Select.Option value="sse">Server-Sent Events (SSE)</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item name="url" label="连接地址 / 命令" rules={[{ required: true }]}>
            <Input placeholder="输入 URL 或启动命令" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default MCP;
