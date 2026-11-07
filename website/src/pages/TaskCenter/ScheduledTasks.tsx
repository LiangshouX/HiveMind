import React, { useCallback, useEffect, useState } from 'react';
import { Table, Button, Typography, Space, Modal, Form, Input, Switch, message, Popconfirm, Empty } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, ReloadOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { api, type ScheduledJob } from '../../api';

const { Title, Text } = Typography;

const ScheduledTasks: React.FC = () => {
  const [tasks, setTasks] = useState<ScheduledJob[]>([]);
  const [loading, setLoading] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [form] = Form.useForm();
  const [editingId, setEditingId] = useState<number | null>(null);

  const loadTasks = useCallback(async () => {
    setLoading(true);
    try {
      const page = await api.listScheduledJobs(1, 50);
      setTasks(page.records ?? []);
    } catch (error) {
      message.error(error instanceof Error ? error.message : '加载定时任务失败');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadTasks();
  }, [loadTasks]);

  const columns: ColumnsType<ScheduledJob> = [
    {
      title: '任务名称',
      dataIndex: 'jobName',
      key: 'jobName',
      width: 220,
      render: (name: string, record) => (
        <Space direction="vertical" size={2} style={{ maxWidth: 220 }}>
          <Text strong style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
            {name || '未命名任务'}
          </Text>
          {record.jobDescription && (
            <Text type="secondary" style={{ fontSize: 12 }} ellipsis={{ tooltip: record.jobDescription }}>
              {record.jobDescription}
            </Text>
          )}
        </Space>
      ),
    },
    {
      title: '启用状态',
      dataIndex: 'isActivated',
      key: 'isActivated',
      width: 120,
      render: (activated: number, record) => (
        <Switch
          checked={activated === 1}
          onChange={async checked => {
            try {
              await api.updateScheduledJob(record.id, {
                jobName: record.jobName,
                isActivated: checked ? 1 : 0,
                cronConfig: record.cronConfig ?? '0 0 9 * * *',
                timeZone: record.timeZone ?? 'Asia/Shanghai',
                jobDescription: record.jobDescription ?? undefined,
              });
              message.success(`${record.jobName} 已${checked ? '启用' : '停用'}`);
              void loadTasks();
            } catch (error) {
              message.error(error instanceof Error ? error.message : '更新失败');
            }
          }}
        />
      ),
    },
    {
      title: 'Cron 表达式',
      dataIndex: 'cronConfig',
      key: 'cronConfig',
      width: 160,
      render: (cron: string | null) => (cron ? <Text code style={{ whiteSpace: 'nowrap' }}>{cron}</Text> : <Text type="secondary">—</Text>),
    },
    {
      title: '时区',
      dataIndex: 'timeZone',
      key: 'timeZone',
      width: 140,
      render: (tz: string | null) => <span style={{ whiteSpace: 'nowrap' }}>{tz ?? '—'}</span>,
    },
    {
      title: '最近更新',
      dataIndex: 'updateTime',
      key: 'updateTime',
      width: 180,
      render: (time: string | null) => <Text type="secondary" style={{ whiteSpace: 'nowrap' }}>{time ?? '—'}</Text>,
    },
    {
      title: '操作',
      key: 'action',
      width: 160,
      render: (_, record) => (
        <Space size="small">
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => {
              setEditingId(record.id);
              form.setFieldsValue({
                jobName: record.jobName,
                cronConfig: record.cronConfig,
                timeZone: record.timeZone,
                jobDescription: record.jobDescription,
              });
              setIsModalOpen(true);
            }}
          >
            编辑
          </Button>
          <Popconfirm
            title="确定要删除该定时任务吗？"
            onConfirm={async () => {
              try {
                await api.deleteScheduledJob(record.id);
                message.success('删除成功');
                void loadTasks();
              } catch (error) {
                message.error(error instanceof Error ? error.message : '删除失败');
              }
            }}
            okText="确定"
            cancelText="取消"
          >
            <Button type="link" danger icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const handleModalOk = async () => {
    try {
      const values = await form.validateFields();
      const payload = {
        jobName: values.jobName,
        cronConfig: values.cronConfig,
        timeZone: values.timeZone ?? 'Asia/Shanghai',
        jobDescription: values.jobDescription,
        isActivated: editingId ? 1 : 1,
      };
      if (editingId) {
        await api.updateScheduledJob(editingId, payload);
        message.success('更新成功');
      } else {
        await api.createScheduledJob(payload);
        message.success('创建成功');
      }
      setIsModalOpen(false);
      form.resetFields();
      setEditingId(null);
      void loadTasks();
    } catch (error) {
      if (error instanceof Error) {
        message.error(error.message);
      }
    }
  };

  return (
    <div style={{ padding: 24, background: 'var(--td-bg-container)', minHeight: '100%', borderRadius: 8 }}>
      <div className="hm-page-head">
        <div className="hm-page-head-text">
          <Title level={4} style={{ margin: 0 }}>定时任务</Title>
          <Text type="secondary">创建和管理在指定时间自动执行的定时任务，数据持久化存储在 MySQL。</Text>
        </div>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={() => void loadTasks()}>刷新</Button>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => { form.resetFields(); setEditingId(null); setIsModalOpen(true); }}
          >
            创建任务
          </Button>
        </Space>
      </div>

      <Table
        columns={columns}
        dataSource={tasks}
        rowKey="id"
        loading={loading}
        locale={{ emptyText: <Empty description="暂无定时任务，点击「创建任务」新增" /> }}
        scroll={{ x: 960 }}
        pagination={{ pageSize: 10 }}
      />

      <Modal
        title={editingId ? '编辑任务' : '创建任务'}
        open={isModalOpen}
        onOk={() => void handleModalOk()}
        onCancel={() => { setIsModalOpen(false); form.resetFields(); setEditingId(null); }}
        okText="保存"
        cancelText="取消"
      >
        <Form form={form} layout="vertical">
          <Form.Item name="jobName" label="任务名称" rules={[{ required: true, message: '请输入任务名称' }]}>
            <Input placeholder="例如：每日站会总结" />
          </Form.Item>
          <Form.Item name="cronConfig" label="Cron 表达式" rules={[{ required: true, message: '请输入 Cron 表达式' }]}>
            <Input placeholder="例如：0 0 9 * * *" />
          </Form.Item>
          <Form.Item name="timeZone" label="时区" initialValue="Asia/Shanghai">
            <Input placeholder="Asia/Shanghai" />
          </Form.Item>
          <Form.Item name="jobDescription" label="任务描述">
            <Input placeholder="简要说明该任务做什么" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default ScheduledTasks;
