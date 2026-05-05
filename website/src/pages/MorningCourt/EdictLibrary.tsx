import React, { useState, useEffect } from 'react';
import { Card, Tag, Button, Modal, Form, Input, Select, Typography, Space, Row, Col, message, Divider, Popconfirm } from 'antd';
import { SyncOutlined, SearchOutlined, PlusOutlined, EditOutlined, DeleteOutlined, EyeOutlined } from '@ant-design/icons';
import { api } from '../../api';
import { useStore, TPL_CATS } from '../../store';
import { useNavigate } from 'react-router-dom';

const { Title, Text, Paragraph } = Typography;
const { TextArea } = Input;

// ── 本地类型定义 ──

interface TemplateParam {
  key: string;
  label: string;
  type: 'text' | 'textarea' | 'select' | 'input';
  required?: boolean;
  defaultValue?: string;
  options?: string[];
}

interface Template {
  templateId: string;
  name: string;
  description: string;
  category: string;
  icon: string;
  command: string;
  params: TemplateParam[];
  depts: string[];
  est: string;
  cost: string;
  type: 'SYSTEM' | 'USER';
  userId: string | null;
  createdAt: string | null;
  updatedAt: string | null;
}

interface ExecuteResult {
  ok: boolean;
  sessionId: string;
  title: string;
  message: string;
}

const EdictLibrary: React.FC = () => {
  const navigate = useNavigate();
  const tplCatFilter = useStore((s) => s.tplCatFilter);
  const setTplCatFilter = useStore((s) => s.setTplCatFilter);

  const [templates, setTemplates] = useState<Template[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchText, setSearchText] = useState('');

  // 模板表单状态
  const [formTpl, setFormTpl] = useState<Template | null>(null);
  const [formVals, setFormVals] = useState<Record<string, string>>({});
  const [previewCmd, setPreviewCmd] = useState('');
  const [form] = Form.useForm();

  // 创建/编辑模板状态
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [editingTemplate, setEditingTemplate] = useState<Template | null>(null);
  const [editForm] = Form.useForm();

  // 下旨成功 Modal
  const [executeResult, setExecuteResult] = useState<ExecuteResult | null>(null);
  const [executeModalOpen, setExecuteModalOpen] = useState(false);

  // 加载模板列表
  const loadTemplates = async () => {
    setLoading(true);
    try {
      const data = await api.listEdictTemplates();
      setTemplates(data);
    } catch (error) {
      message.error('加载模板列表失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadTemplates();
  }, []);

  // 过滤模板
  let filteredTemplates = templates;
  if (tplCatFilter !== '全部') {
    filteredTemplates = filteredTemplates.filter((t) => t.category === tplCatFilter);
  }
  if (searchText) {
    const lower = searchText.toLowerCase();
    filteredTemplates = filteredTemplates.filter(
      (t) => t.name.toLowerCase().includes(lower) || t.description.toLowerCase().includes(lower)
    );
  }
  // 系统内置排在前面
  filteredTemplates = filteredTemplates.sort((a, b) => {
    if (a.type === 'SYSTEM' && b.type !== 'SYSTEM') return -1;
    if (a.type !== 'SYSTEM' && b.type === 'SYSTEM') return 1;
    return 0;
  });

  // 打开模板表单（填写参数）
  const openForm = (tpl: Template) => {
    const vals: Record<string, string> = {};
    tpl.params.forEach((p) => {
      vals[p.key] = p.defaultValue || '';
    });
    setFormVals(vals);
    form.setFieldsValue(vals);
    setFormTpl(tpl);
    setPreviewCmd('');
  };

  // 构建最终命令
  const buildCmd = (tpl: Template, values: Record<string, string>) => {
    let cmd = tpl.command;
    for (const p of tpl.params) {
      cmd = cmd.replace(new RegExp('\\{' + p.key + '\\}', 'g'), values[p.key] || p.defaultValue || '');
    }
    return cmd;
  };

  // 预览命令
  const preview = async () => {
    try {
      const values = await form.validateFields();
      if (!formTpl) return;
      setPreviewCmd(buildCmd(formTpl, values));
    } catch {
      message.error('请填写必填参数');
    }
  };

  // 下旨执行
  const execute = async (values: Record<string, string>) => {
    if (!formTpl) return;
    const cmd = buildCmd(formTpl, values);
    if (!cmd.trim()) {
      message.error('请填写必填参数');
      return;
    }

    Modal.confirm({
      title: <span style={{ color: 'var(--td-highlight)', fontSize: '20px' }}>确认下旨？</span>,
      content: (
        <div style={{
          whiteSpace: 'pre-wrap',
          maxHeight: 200,
          overflowY: 'auto',
          background: 'var(--td-bg-elevated)',
          padding: '16px',
          borderRadius: '8px',
          border: '1px solid var(--td-border-light)',
          color: 'var(--td-text-base)',
          marginTop: '16px',
          fontFamily: '"Noto Serif SC", serif'
        }}>{cmd}</div>
      ),
      okText: '奉天承运',
      cancelText: '暂缓',
      okButtonProps: { style: { background: 'var(--td-primary)', borderColor: 'var(--td-primary)' } },
      onOk: async () => {
        try {
          const params: Record<string, string> = {};
          for (const p of formTpl.params) {
            params[p.key] = values[p.key] || p.defaultValue || '';
          }
          const result = await api.executeEdictTemplate(formTpl.templateId, params);
          if (result.ok) {
            setExecuteResult(result);
            setExecuteModalOpen(true);
            setFormTpl(null);
          } else {
            message.error('下旨失败');
          }
        } catch {
          message.error('⚠️ 服务器连接失败');
        }
      }
    });
  };

  // 打开创建/编辑模板 Modal
  const openEditModal = (tpl: Template | null = null) => {
    setEditingTemplate(tpl);
    if (tpl) {
      editForm.setFieldsValue({
        name: tpl.name,
        description: tpl.description,
        category: tpl.category,
        icon: tpl.icon,
        command: tpl.command,
        depts: tpl.depts.join(','),
        est: tpl.est,
        cost: tpl.cost,
        paramsJson: JSON.stringify(tpl.params, null, 2),
      });
    } else {
      editForm.resetFields();
    }
    setEditModalOpen(true);
  };

  // 保存模板（创建或更新）
  const handleSaveTemplate = async (values: any) => {
    try {
      const params: TemplateParam[] = JSON.parse(values.paramsJson || '[]');
      const depts = values.depts ? values.depts.split(/[,，]/).map((s: string) => s.trim()).filter(Boolean) : [];

      const tplData = {
        name: values.name,
        description: values.description,
        category: values.category,
        icon: values.icon,
        command: values.command,
        params,
        depts,
        est: values.est,
        cost: values.cost,
      };

      if (editingTemplate) {
        await api.updateEdictTemplate(editingTemplate.templateId, tplData);
        message.success('模板已更新');
      } else {
        await api.createEdictTemplate(tplData);
        message.success('模板已创建');
      }

      setEditModalOpen(false);
      await loadTemplates();
    } catch (e: any) {
      message.error(e.message || '保存失败');
    }
  };

  // 删除模板
  const handleDeleteTemplate = async (templateId: string) => {
    try {
      await api.deleteEdictTemplate(templateId);
      message.success('模板已删除');
      await loadTemplates();
    } catch {
      message.error('删除失败');
    }
  };

  // 跳转到会话
  const handleNavigateToChat = (sessionId: string) => {
    setExecuteModalOpen(false);
    navigate(`/chat/${sessionId}`);
  };

  return (
    <div style={{ padding: '24px', background: 'var(--td-bg-container)', borderRadius: '16px', minHeight: '100%', border: '1px solid var(--td-border-light)' }}>
      {/* Header */}
      <div style={{ marginBottom: 32, borderBottom: '1px solid var(--td-border-light)', paddingBottom: '24px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <Title level={3} style={{ color: 'var(--td-highlight)', margin: 0 }}>
              早朝 · 旨意库
            </Title>
            <Text type="secondary" style={{ marginTop: '8px', display: 'block', color: 'var(--td-text-secondary)' }}>
              预设圣旨模板，分类筛选 · 参数表单 · 预估时间和费用。
            </Text>
          </div>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => openEditModal()}
            style={{ background: 'var(--td-primary)', borderColor: 'var(--td-primary)' }}
          >
            新建模板
          </Button>
        </div>
      </div>

      {/* Search & Category filter */}
      <Row gutter={[16, 16]} style={{ marginBottom: 32 }}>
        <Col span={8}>
          <Input
            prefix={<SearchOutlined />}
            placeholder="搜索模板名称或描述..."
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
            allowClear
            style={{ background: 'var(--td-input-bg)', color: 'var(--td-text-base)' }}
          />
        </Col>
        <Col span={16}>
          <Space size={[12, 12]} wrap>
            {TPL_CATS.map((c) => (
              <Tag.CheckableTag
                key={c.name}
                checked={tplCatFilter === c.name}
                onChange={() => setTplCatFilter(c.name)}
                style={{
                  fontSize: 14,
                  padding: '6px 16px',
                  background: tplCatFilter === c.name ? 'var(--td-primary-bg)' : 'var(--td-bg-elevated)',
                  border: `1px solid ${tplCatFilter === c.name ? 'var(--td-primary)' : 'var(--td-border-light)'}`,
                  borderRadius: '20px',
                  transition: 'all 0.3s',
                  color: tplCatFilter === c.name ? 'var(--td-primary)' : 'var(--td-text-secondary)'
                }}
              >
                {c.icon} {c.name}
              </Tag.CheckableTag>
            ))}
          </Space>
        </Col>
      </Row>

      {/* Template Grid */}
      <Row gutter={[24, 24]}>
        {filteredTemplates.map((t) => (
          <Col xs={24} sm={12} md={8} lg={8} xl={6} key={t.templateId}>
            <Card
              hoverable
              style={{
                height: '100%',
                display: 'flex',
                flexDirection: 'column',
                background: 'var(--td-bg-elevated)',
                border: '1px solid var(--td-border-light)',
                borderRadius: '12px',
                overflow: 'hidden',
                boxShadow: 'var(--td-shadow-base)',
                position: 'relative',
              }}
              styles={{ body: { flex: 1, display: 'flex', flexDirection: 'column', padding: '20px' } }}
            >
              {/* 系统内置标签 */}
              {t.type === 'SYSTEM' && (
                <div style={{
                  position: 'absolute',
                  top: 12,
                  right: 12,
                  zIndex: 1,
                }}>
                  <Tag color="gold" style={{ borderRadius: '4px' }}>系统内置</Tag>
                </div>
              )}

              {/* 用户模板的操作按钮 */}
              {t.type === 'USER' && (
                <div style={{
                  position: 'absolute',
                  top: 8,
                  right: 8,
                  zIndex: 1,
                  display: 'flex',
                  gap: 4,
                }}>
                  <Button
                    size="small"
                    icon={<EditOutlined />}
                    onClick={(e) => { e.stopPropagation(); openEditModal(t); }}
                    style={{ background: 'var(--td-bg-container)', border: '1px solid var(--td-border-light)' }}
                  />
                  <Popconfirm
                    title="确定删除此模板？"
                    onConfirm={(e) => { e?.stopPropagation(); handleDeleteTemplate(t.templateId); }}
                    okText="确定"
                    cancelText="取消"
                  >
                    <Button
                      size="small"
                      danger
                      icon={<DeleteOutlined />}
                      onClick={(e) => e.stopPropagation()}
                      style={{ background: 'var(--td-bg-container)', border: '1px solid var(--td-border-light)' }}
                    />
                  </Popconfirm>
                </div>
              )}

              <div
                style={{ cursor: 'pointer' }}
                onClick={() => openForm(t)}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
                  <span style={{ fontSize: 28, filter: 'drop-shadow(0 2px 4px rgba(0,0,0,0.1))' }}>{t.icon}</span>
                  <Text strong style={{ fontSize: 18, color: 'var(--td-text-base)' }}>{t.name}</Text>
                </div>
                <Paragraph type="secondary" style={{ flex: 1, marginBottom: 24, fontSize: 14, lineHeight: 1.6, color: 'var(--td-text-secondary)' }}>
                  {t.description}
                </Paragraph>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 'auto', borderTop: '1px solid var(--td-border-light)', paddingTop: '16px' }}>
                  <Space size={6} wrap>
                    {t.depts.map((d) => (
                      <Tag
                        key={d}
                        style={{
                          background: 'var(--td-highlight-bg)',
                          color: 'var(--td-highlight)',
                          border: '1px solid var(--td-border-color)',
                          borderRadius: '4px'
                        }}
                      >
                        {d}
                      </Tag>
                    ))}
                  </Space>
                  <Text type="secondary" style={{ fontSize: 12, color: 'var(--td-text-tertiary)' }}>
                    {t.est} · {t.cost}
                  </Text>
                </div>
              </div>
            </Card>
          </Col>
        ))}
      </Row>

      {filteredTemplates.length === 0 && !loading && (
        <div style={{ textAlign: 'center', padding: '60px 0', color: 'var(--td-text-secondary)' }}>
          <Empty description="暂无模板" />
        </div>
      )}

      {/* Template Form Modal */}
      <Modal
        title={
          <Space align="center" style={{ paddingBottom: '12px', borderBottom: '1px solid var(--td-border-light)', width: '100%' }}>
            <span style={{ fontSize: 24 }}>{formTpl?.icon}</span>
            <span style={{ fontSize: 20, color: 'var(--td-highlight)' }}>{formTpl?.name}</span>
          </Space>
        }
        open={!!formTpl}
        onCancel={() => setFormTpl(null)}
        footer={null}
        width={650}
        destroyOnClose
        styles={{
          body: { background: 'var(--td-bg-container)', borderRadius: '0 0 12px 12px' },
          header: { background: 'transparent' }
        }}
      >
        {formTpl && (
          <div style={{ paddingTop: '16px' }}>
            <Paragraph type="secondary" style={{ fontSize: '15px', color: 'var(--td-text-secondary)', marginBottom: '24px' }}>
              {formTpl.description}
            </Paragraph>
            <div style={{ marginBottom: 32, background: 'var(--td-bg-base)', padding: '12px 16px', borderRadius: '8px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', border: '1px solid var(--td-border-light)' }}>
              <Space>
                <Text style={{ color: 'var(--td-text-tertiary)' }}>承办：</Text>
                {formTpl.depts.map((d) => (
                  <Tag key={d} style={{ background: 'var(--td-highlight-bg)', color: 'var(--td-highlight)', border: '1px solid var(--td-border-color)' }}>{d}</Tag>
                ))}
              </Space>
              <Text type="secondary" style={{ color: 'var(--td-text-tertiary)' }}>
                预计：<span style={{ color: 'var(--td-text-base)' }}>{formTpl.est}</span> &nbsp;|&nbsp; 耗资：<span style={{ color: 'var(--td-text-base)' }}>{formTpl.cost}</span>
              </Text>
            </div>

            <Form
              form={form}
              layout="vertical"
              onFinish={execute}
              initialValues={formVals}
              requiredMark="optional"
            >
              {formTpl.params.map((p) => (
                <Form.Item
                  key={p.key}
                  name={p.key}
                  label={<span style={{ color: 'var(--td-highlight)' }}>{p.label}</span>}
                  rules={[{ required: p.required, message: `朕需知晓${p.label}` }]}
                >
                  {p.type === 'textarea' ? (
                    <TextArea rows={4} style={{ background: 'var(--td-input-bg)', color: 'var(--td-text-base)', border: '1px solid var(--td-border-color)' }} />
                  ) : p.type === 'select' ? (
                    <Select dropdownStyle={{ background: 'var(--td-bg-elevated)' }}>
                      {(p.options || []).map((o) => (
                        <Select.Option key={o} value={o}>
                          <span style={{ color: 'var(--td-text-base)' }}>{o}</span>
                        </Select.Option>
                      ))}
                    </Select>
                  ) : (
                    <Input style={{ background: 'var(--td-input-bg)', color: 'var(--td-text-base)', border: '1px solid var(--td-border-color)' }} />
                  )}
                </Form.Item>
              ))}

              {previewCmd && (
                <div style={{
                  background: 'var(--td-primary-bg)',
                  padding: '20px',
                  borderRadius: '8px',
                  marginBottom: '24px',
                  border: '1px dashed var(--td-primary)'
                }}>
                  <Text strong style={{ color: 'var(--td-primary)', fontSize: '16px' }}>📜 拟定圣旨：</Text>
                  <Paragraph style={{
                    whiteSpace: 'pre-wrap',
                    marginTop: 16,
                    marginBottom: 0,
                    color: 'var(--td-text-base)',
                    fontFamily: '"Noto Serif SC", serif',
                    fontSize: '15px',
                    lineHeight: 1.8
                  }}>
                    {previewCmd}
                  </Paragraph>
                </div>
              )}

              <Form.Item style={{ marginBottom: 0, marginTop: '32px', textAlign: 'right' }}>
                <Space size={16}>
                  <Button onClick={preview} style={{ borderColor: 'var(--td-highlight)', color: 'var(--td-highlight)' }}>👁 预览</Button>
                  <Button type="primary" htmlType="submit" size="large" style={{ background: 'var(--td-primary)', border: 'none', padding: '0 32px', color: 'var(--td-text-inverse)' }}>
                    📜 下旨
                  </Button>
                </Space>
              </Form.Item>
            </Form>
          </div>
        )}
      </Modal>

      {/* Edit/Create Template Modal */}
      <Modal
        title={editingTemplate ? '编辑模板' : '新建模板'}
        open={editModalOpen}
        onCancel={() => setEditModalOpen(false)}
        onOk={() => editForm.submit()}
        width={700}
        okText="保存"
        cancelText="取消"
      >
        <Form
          form={editForm}
          layout="vertical"
          onFinish={handleSaveTemplate}
          style={{ paddingTop: '16px' }}
        >
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="name" label="模板名称" rules={[{ required: true, message: '请输入名称' }]}>
                <Input style={{ background: 'var(--td-input-bg)', color: 'var(--td-text-base)' }} />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item name="icon" label="图标">
                <Input style={{ background: 'var(--td-input-bg)', color: 'var(--td-text-base)' }} placeholder="📝" />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item name="category" label="分类" rules={[{ required: true, message: '请选择分类' }]}>
                <Select dropdownStyle={{ background: 'var(--td-bg-elevated)' }}>
                  {TPL_CATS.filter(c => c.name !== '全部').map(c => (
                    <Select.Option key={c.name} value={c.name}>{c.icon} {c.name}</Select.Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="description" label="描述">
            <TextArea rows={2} style={{ background: 'var(--td-input-bg)', color: 'var(--td-text-base)' }} />
          </Form.Item>
          <Form.Item name="command" label="命令模板" rules={[{ required: true, message: '请输入命令' }]}>
            <TextArea rows={3} style={{ background: 'var(--td-input-bg)', color: 'var(--td-text-base)' }} placeholder="使用 {paramKey} 表示参数占位符" />
          </Form.Item>
          <Form.Item name="paramsJson" label="参数定义 (JSON)" rules={[{ required: true, message: '请输入参数定义' }]}>
            <TextArea
              rows={6}
              style={{ background: 'var(--td-input-bg)', color: 'var(--td-text-base)', fontFamily: 'monospace' }}
              placeholder={`[
  {"key": "repo", "label": "仓库路径", "type": "text", "required": true},
  {"key": "format", "label": "格式", "type": "select", "options": ["MD", "PDF"]}
]`}
            />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="depts" label="承办部门（逗号分隔）">
                <Input style={{ background: 'var(--td-input-bg)', color: 'var(--td-text-base)' }} placeholder="吏部, 户部" />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item name="est" label="预计时间">
                <Input style={{ background: 'var(--td-input-bg)', color: 'var(--td-text-base)' }} placeholder="~30分钟" />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item name="cost" label="预计耗资">
                <Input style={{ background: 'var(--td-input-bg)', color: 'var(--td-text-base)' }} placeholder="¥2" />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>

      {/* Execute Result Modal */}
      <Modal
        title={<span style={{ color: 'var(--td-highlight)', fontSize: '20px' }}>📜 旨意已下达</span>}
        open={executeModalOpen}
        onCancel={() => setExecuteModalOpen(false)}
        footer={[
          <Button key="close" onClick={() => setExecuteModalOpen(false)}>
            关闭
          </Button>,
          <Button
            key="view"
            type="primary"
            onClick={() => executeResult && handleNavigateToChat(executeResult.sessionId)}
            style={{ background: 'var(--td-primary)', border: 'none' }}
          >
            查看对话
          </Button>
        ]}
      >
        <div style={{ padding: '16px 0' }}>
          <Paragraph style={{ fontSize: '16px', color: 'var(--td-text-base)' }}>
            {executeResult?.message}
          </Paragraph>
          <div style={{
            background: 'var(--td-bg-elevated)',
            padding: '12px 16px',
            borderRadius: '8px',
            marginTop: '16px',
            border: '1px solid var(--td-border-light)'
          }}>
            <Text type="secondary">Session ID: </Text>
            <Text code style={{ color: 'var(--td-primary)' }}>{executeResult?.sessionId}</Text>
          </div>
        </div>
      </Modal>
    </div>
  );
};

// 简单的 Empty 组件（如果 antd 没有导入）
const Empty: React.FC<{ description?: string }> = ({ description }) => (
  <div style={{ textAlign: 'center', color: 'inherit' }}>
    <div style={{ fontSize: 48, marginBottom: 16 }}>📭</div>
    <div>{description}</div>
  </div>
);

export default EdictLibrary;
