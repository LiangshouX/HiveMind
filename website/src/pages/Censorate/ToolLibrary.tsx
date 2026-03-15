import React, { useState } from 'react';
import { Table, Typography, Switch, Tag, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';

const { Title, Text } = Typography;

interface Tool {
  id: string;
  name: string;
  description: string;
  enabled: boolean;
  category: string;
}

const initialTools: Tool[] = [
  { id: 't1', name: 'WebSearch', description: '进行浏览器Web搜索，获取实时信息', enabled: true, category: '搜索' },
  { id: 't2', name: 'ExecuteCommand', description: '在安全沙箱内执行命令行指令', enabled: false, category: '系统' },
  { id: 't3', name: 'ReadFile', description: '读取本地文件内容', enabled: true, category: '文件系统' },
  { id: 't4', name: 'WriteFile', description: '写入或覆盖本地文件', enabled: false, category: '文件系统' },
];

const ToolLibrary: React.FC = () => {
  const [tools, setTools] = useState<Tool[]>(initialTools);

  const handleToggle = (id: string, checked: boolean) => {
    setTools(tools.map(t => t.id === id ? { ...t, enabled: checked } : t));
    const tool = tools.find(t => t.id === id);
    message.success(`${tool?.name} 已${checked ? '启用' : '禁用'}`);
  };

  const columns: ColumnsType<Tool> = [
    { title: '工具名称', dataIndex: 'name', key: 'name', render: (text) => <Text strong>{text}</Text> },
    { title: '描述', dataIndex: 'description', key: 'description' },
    { title: '分类', dataIndex: 'category', key: 'category', render: (text) => <Tag color="blue">{text}</Tag> },
    { 
      title: '状态', 
      dataIndex: 'enabled', 
      key: 'enabled',
      render: (enabled: boolean, record) => (
        <Switch checked={enabled} onChange={(checked) => handleToggle(record.id, checked)} />
      )
    },
  ];

  return (
    <div style={{ padding: 24, background: '#fff', minHeight: '100%', borderRadius: 8 }}>
      <div style={{ marginBottom: 24 }}>
        <Title level={4} style={{ margin: 0 }}>工具库 (Tool Library)</Title>
        <Text type="secondary">管理 AI 助手使用的系统工具，开启或关闭特定的能力。</Text>
      </div>
      <Table columns={columns} dataSource={tools} rowKey="id" pagination={false} />
    </div>
  );
};

export default ToolLibrary;
