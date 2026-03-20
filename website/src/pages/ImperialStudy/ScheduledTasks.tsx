import React, { useState } from 'react';
import { Table, Button, Typography, Tag, Space, Modal, Form, Input, Select, Switch, message } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';

const { Title, Text } = Typography;

interface Task {
  id: string;
  name: string;
  status: boolean;
  scheduleType: string;
  cron: string;
  timezone: string;
  taskType: string;
}

const initialTasks: Task[] = [
  { id: '1', name: '每日站会总结', status: true, scheduleType: 'cron', cron: '0 0 9 * * *', timezone: 'Asia/Shanghai', taskType: '站会摘要' },
  { id: '2', name: '系统心跳检测', status: false, scheduleType: 'interval', cron: 'every 5 mins', timezone: 'UTC', taskType: '心跳' },
];

const ScheduledTasks: React.FC = () => {
  const [tasks, setTasks] = useState<Task[]>(initialTasks);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [form] = Form.useForm();
  const [editingId, setEditingId] = useState<string | null>(null);

  const columns: ColumnsType<Task> = [
    { title: '任务ID', dataIndex: 'id', key: 'id' },
    { title: '任务名称', dataIndex: 'name', key: 'name' },
    { 
      title: '启用状态', 
      dataIndex: 'status', 
      key: 'status',
      render: (status: boolean, record) => (
        <Switch 
          checked={status} 
          onChange={(checked) => {
            setTasks(tasks.map(t => t.id === record.id ? { ...t, status: checked } : t));
            message.success(`${record.name} 已${checked ? '启用' : '禁用'}`);
          }} 
        />
      )
    },
    { title: '调度类型', dataIndex: 'scheduleType', key: 'scheduleType', render: (text) => <Tag color="blue">{text}</Tag> },
    { title: '执行时间', dataIndex: 'cron', key: 'cron', render: (text) => <Text code>{text}</Text> },
    { title: '时区', dataIndex: 'timezone', key: 'timezone' },
    { title: 'TaskType', dataIndex: 'taskType', key: 'taskType' },
    { 
      title: '操作', 
      key: 'action', 
      render: (_, record) => (
        <Space size="middle">
          <Button type="link" icon={<EditOutlined />} onClick={() => handleEdit(record)}>编辑</Button>
          <Button type="link" danger icon={<DeleteOutlined />} onClick={() => handleDelete(record.id)}>删除</Button>
        </Space>
      ) 
    },
  ];

  const handleEdit = (record: Task) => {
    setEditingId(record.id);
    form.setFieldsValue(record);
    setIsModalOpen(true);
  };

  const handleDelete = (id: string) => {
    setTasks(tasks.filter(t => t.id !== id));
    message.success('删除成功');
  };

  const handleModalOk = () => {
    form.validateFields().then(values => {
      if (editingId) {
        setTasks(tasks.map(t => t.id === editingId ? { ...t, ...values } : t));
        message.success('更新成功');
      } else {
        const newTask = { ...values, id: Math.random().toString(36).substr(2, 9), status: true };
        setTasks([...tasks, newTask]);
        message.success('创建成功');
      }
      setIsModalOpen(false);
      form.resetFields();
      setEditingId(null);
    });
  };

  return (
    <div style={{ padding: 24, background: '#fff', minHeight: '100%', borderRadius: 8 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <div>
          <Title level={4} style={{ margin: 0 }}>司天台</Title>
          <Text type="secondary">创建和管理在指定时间自动执行的定时任务。</Text>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => { form.resetFields(); setEditingId(null); setIsModalOpen(true); }}>
          创建任务
        </Button>
      </div>

      <Table columns={columns} dataSource={tasks} rowKey="id" pagination={{ pageSize: 10 }} />

      <Modal 
        title={editingId ? '编辑任务' : '创建任务'} 
        open={isModalOpen} 
        onOk={handleModalOk} 
        onCancel={() => { setIsModalOpen(false); form.resetFields(); setEditingId(null); }}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="任务名称" rules={[{ required: true }]}>
            <Input placeholder="输入任务名称" />
          </Form.Item>
          <Form.Item name="scheduleType" label="调度类型" rules={[{ required: true }]}>
            <Select>
              <Select.Option value="cron">Cron 表达式</Select.Option>
              <Select.Option value="interval">固定间隔</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item name="cron" label="执行时间配置" rules={[{ required: true }]}>
            <Input placeholder="例如：0 0 9 * * *" />
          </Form.Item>
          <Form.Item name="timezone" label="时区" initialValue="Asia/Shanghai">
            <Input />
          </Form.Item>
          <Form.Item name="taskType" label="任务类型" rules={[{ required: true }]}>
            <Input placeholder="例如：站会摘要" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default ScheduledTasks;
