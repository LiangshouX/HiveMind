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
        // We still proceed if user wants, but here we can just warn
      }
    } catch {
      /* ignore */
    }

    Modal.confirm({
      title: '确认下旨？',
      content: <div style={{ whiteSpace: 'pre-wrap', maxHeight: 200, overflowY: 'auto' }}>{cmd}</div>,
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
    <div>
      <div style={{ marginBottom: 24 }}>
        <Title level={4}>早朝 - 旨意库</Title>
        <Text type="secondary">预设圣旨模板，分类筛选 · 参数表单 · 预估时间和费用。</Text>
      </div>

      {/* Category filter */}
      <Space size={[8, 8]} wrap style={{ marginBottom: 24 }}>
        {TPL_CATS.map((c) => (
          <Tag.CheckableTag
            key={c.name}
            checked={tplCatFilter === c.name}
            onChange={() => setTplCatFilter(c.name)}
            style={{ fontSize: 14, padding: '4px 12px' }}
          >
            {c.icon} {c.name}
          </Tag.CheckableTag>
        ))}
      </Space>

      {/* Grid */}
      <Row gutter={[16, 16]}>
        {tpls.map((t) => (
          <Col xs={24} sm={12} md={8} lg={8} xl={6} key={t.id}>
            <Card 
              hoverable 
              onClick={() => openForm(t)}
              style={{ height: '100%', display: 'flex', flexDirection: 'column' }}
              bodyStyle={{ flex: 1, display: 'flex', flexDirection: 'column', padding: 16 }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12 }}>
                <span style={{ fontSize: 24 }}>{t.icon}</span>
                <Text strong style={{ fontSize: 16 }}>{t.name}</Text>
              </div>
              <Paragraph type="secondary" style={{ flex: 1, marginBottom: 16, fontSize: 13 }}>
                {t.desc}
              </Paragraph>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 'auto' }}>
                <Space size={4}>
                  {t.depts.map((d) => (
                    <Tag color="blue" key={d} bordered={false}>{d}</Tag>
                  ))}
                </Space>
                <Text type="secondary" style={{ fontSize: 12 }}>
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
          <Space>
            <span>{formTpl?.icon}</span>
            <span>{formTpl?.name}</span>
          </Space>
        }
        open={!!formTpl}
        onCancel={() => setFormTpl(null)}
        footer={null}
        width={600}
        destroyOnClose
      >
        {formTpl && (
          <div>
            <Paragraph type="secondary">{formTpl.desc}</Paragraph>
            <div style={{ marginBottom: 24 }}>
              <Space>
                {formTpl.depts.map((d) => (
                  <Tag color="blue" key={d}>{d}</Tag>
                ))}
              </Space>
              <Text type="secondary" style={{ float: 'right' }}>
                {formTpl.est} · {formTpl.cost}
              </Text>
            </div>

            <Form
              form={form}
              layout="vertical"
              onFinish={execute}
              initialValues={formVals}
            >
              {formTpl.params.map((p) => (
                <Form.Item
                  key={p.key}
                  name={p.key}
                  label={p.label}
                  rules={[{ required: p.required, message: `请输入${p.label}` }]}
                >
                  {p.type === 'textarea' ? (
                    <TextArea rows={4} />
                  ) : p.type === 'select' ? (
                    <Select>
                      {(p.options || []).map((o) => (
                        <Select.Option key={o} value={o}>{o}</Select.Option>
                      ))}
                    </Select>
                  ) : (
                    <Input />
                  )}
                </Form.Item>
              ))}

              {previewCmd && (
                <div style={{ background: '#f5f5f5', padding: 12, borderRadius: 8, marginBottom: 24 }}>
                  <Text strong>📜 将发送给中书省的旨意：</Text>
                  <Paragraph style={{ whiteSpace: 'pre-wrap', marginTop: 8, marginBottom: 0 }}>
                    {previewCmd}
                  </Paragraph>
                </div>
              )}

              <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
                <Space>
                  <Button onClick={preview}>👁 预览旨意</Button>
                  <Button type="primary" htmlType="submit">📜 下旨</Button>
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
