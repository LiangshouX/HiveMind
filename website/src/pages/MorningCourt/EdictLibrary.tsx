import React, { useState } from 'react';
import { Card, Tag, Button, Modal, Form, Input, Select, Typography, Space, Row, Col, message } from 'antd';
import { useStore, TEMPLATES, TPL_CATS, type Template } from '../../store';
import { api } from '../../api';

const { Title, Text, Paragraph } = Typography;
const { TextArea } = Input;

const EdictLibrary: React.FC = () => {
  const tplCatFilter = useStore((s) => s.tplCatFilter);
  const setTplCatFilter = useStore((s) => s.setTplCatFilter);
  const loadAll = useStore((s) => s.loadAll);

  const [formTpl, setFormTpl] = useState<Template | null>(null);
  const [formVals, setFormVals] = useState<Record<string, string>>({});
  const [previewCmd, setPreviewCmd] = useState('');
  const [form] = Form.useForm();

  let tpls = TEMPLATES;
  if (tplCatFilter !== '全部') tpls = tpls.filter((t) => t.cat === tplCatFilter);

  const openForm = (tpl: Template) => {
    const vals: Record<string, string> = {};
    tpl.params.forEach((p) => {
      vals[p.key] = p.default || '';
    });
    setFormVals(vals);
    form.setFieldsValue(vals);
    setFormTpl(tpl);
    setPreviewCmd('');
  };

  const buildCmd = (tpl: Template, values: Record<string, string>) => {
    let cmd = tpl.command;
    for (const p of tpl.params) {
      cmd = cmd.replace(new RegExp('\\{' + p.key + '\\}', 'g'), values[p.key] || p.default || '');
    }
    return cmd;
  };

  const preview = async () => {
    try {
      const values = await form.validateFields();
      if (!formTpl) return;
      setPreviewCmd(buildCmd(formTpl, values));
    } catch (error) {
      message.error('请填写必填参数');
    }
  };

  const execute = async (values: any) => {
    if (!formTpl) return;
    const cmd = buildCmd(formTpl, values);
    if (!cmd.trim()) {
      message.error('请填写必填参数');
      return;
    }

    try {
      const st = await api.agentsStatus();
      if (st.ok && st.gateway && !st.gateway.alive) {
        message.warning('⚠️ Gateway 未启动，任务将无法派发！');
      }
    } catch {
      /* ignore */
    }

    Modal.confirm({
      title: <span className="imperial-heading" style={{ color: 'var(--td-highlight)', fontSize: '20px' }}>确认下旨？</span>,
      content: <div style={{ 
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
      }}>{cmd}</div>,
      okText: '奉天承运',
      cancelText: '暂缓',
      okButtonProps: { style: { background: 'var(--td-primary)', borderColor: 'var(--td-primary)' } },
      onOk: async () => {
        try {
          const params: Record<string, string> = {};
          for (const p of formTpl.params) {
            params[p.key] = values[p.key] || p.default || '';
          }
          const r = await api.createTask({
            title: cmd.substring(0, 120),
            org: '中书省',
            targetDept: formTpl.depts[0] || '',
            priority: 'normal',
            templateId: formTpl.id,
            params,
          });
          if (r.ok) {
            message.success(`📜 ${r.taskId || ''} 旨意已下达`);
            setFormTpl(null);
            loadAll();
          } else {
            message.error(r.error || '下旨失败');
          }
        } catch {
          message.error('⚠️ 服务器连接失败');
        }
      }
    });
  };

  return (
    <div style={{ padding: '24px', background: 'var(--td-bg-container)', borderRadius: '16px', minHeight: '100%', border: '1px solid var(--td-border-light)' }}>
      <div style={{ marginBottom: 32, borderBottom: '1px solid var(--td-border-light)', paddingBottom: '24px' }}>
        <Title level={3} className="imperial-heading" style={{ color: 'var(--td-highlight)', margin: 0 }}>
          早朝 · 旨意库
        </Title>
        <Text type="secondary" style={{ marginTop: '8px', display: 'block', color: 'var(--td-text-secondary)' }}>
          预设圣旨模板，分类筛选 · 参数表单 · 预估时间和费用。
        </Text>
      </div>

      {/* Category filter */}
      <Space size={[12, 12]} wrap style={{ marginBottom: 32 }}>
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

      {/* Grid */}
      <Row gutter={[24, 24]}>
        {tpls.map((t) => (
          <Col xs={24} sm={12} md={8} lg={8} xl={6} key={t.id}>
            <Card 
              hoverable 
              onClick={() => openForm(t)}
              style={{ 
                height: '100%', 
                display: 'flex', 
                flexDirection: 'column',
                background: 'var(--td-bg-elevated)',
                border: '1px solid var(--td-border-light)',
                borderRadius: '12px',
                overflow: 'hidden',
                boxShadow: 'var(--td-shadow-base)'
              }}
              styles={{ body: { flex: 1, display: 'flex', flexDirection: 'column', padding: '20px' } }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
                <span style={{ fontSize: 28, filter: 'drop-shadow(0 2px 4px rgba(0,0,0,0.1))' }}>{t.icon}</span>
                <Text strong style={{ fontSize: 18, color: 'var(--td-text-base)' }} className="imperial-heading">{t.name}</Text>
              </div>
              <Paragraph type="secondary" style={{ flex: 1, marginBottom: 24, fontSize: 14, lineHeight: 1.6, color: 'var(--td-text-secondary)' }}>
                {t.desc}
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
            </Card>
          </Col>
        ))}
      </Row>

      {/* Template Form Modal */}
      <Modal
        title={
          <Space align="center" style={{ paddingBottom: '12px', borderBottom: '1px solid var(--td-border-light)', width: '100%' }}>
            <span style={{ fontSize: 24 }}>{formTpl?.icon}</span>
            <span className="imperial-heading" style={{ fontSize: 20, color: 'var(--td-highlight)' }}>{formTpl?.name}</span>
          </Space>
        }
        open={!!formTpl}
        onCancel={() => setFormTpl(null)}
        footer={null}
        width={650}
        destroyOnClose
        styles={{
          body: {
            background: 'var(--td-bg-container)',
            borderRadius: '0 0 12px 12px',
          },
          header: {
            background: 'transparent'
          }
        }}
      >
        {formTpl && (
          <div style={{ paddingTop: '16px' }}>
            <Paragraph type="secondary" style={{ fontSize: '15px', color: 'var(--td-text-secondary)', marginBottom: '24px' }}>
              {formTpl.desc}
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
                  <Text strong style={{ color: 'var(--td-primary)', fontSize: '16px' }} className="imperial-heading">📜 拟定圣旨：</Text>
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
    </div>
  );
};

export default EdictLibrary;
