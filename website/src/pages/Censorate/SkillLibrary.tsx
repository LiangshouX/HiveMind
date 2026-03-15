import React, { useEffect, useState } from 'react';
import { Card, Button, Typography, Space, Row, Col, Empty, Modal, Form, Input, Select, Tabs, Tag, message } from 'antd';
import { PlusOutlined, SyncOutlined, DeleteOutlined, FileTextOutlined } from '@ant-design/icons';
import { useStore } from '../../store';
import { api, type RemoteSkillItem } from '../../api';

const { Title, Text, Paragraph } = Typography;

const COMMUNITY_SOURCES = [
  {
    label: 'obra/superpowers',
    emoji: '⚡',
    stars: '66.9k',
    desc: '完整开发工作流技能集',
    skills: [
      { name: 'brainstorming', url: 'https://raw.githubusercontent.com/obra/superpowers/refs/heads/main/skills/brainstorming/SKILL.md' },
      { name: 'test-driven-development', url: 'https://raw.githubusercontent.com/obra/superpowers/refs/heads/main/skills/test-driven-development/SKILL.md' },
      { name: 'systematic-debugging', url: 'https://raw.githubusercontent.com/obra/superpowers/refs/heads/main/skills/systematic-debugging/SKILL.md' },
    ],
  },
  {
    label: 'anthropics/skills',
    emoji: '🏛️',
    stars: '官方',
    desc: 'Anthropic 官方技能库',
    skills: [
      { name: 'docx', url: 'https://raw.githubusercontent.com/anthropics/skills/main/skills/docx/SKILL.md' },
      { name: 'pdf', url: 'https://raw.githubusercontent.com/anthropics/skills/main/skills/pdf/SKILL.md' },
      { name: 'xlsx', url: 'https://raw.githubusercontent.com/anthropics/skills/main/skills/xlsx/SKILL.md' },
    ],
  },
];

const SkillLibrary: React.FC = () => {
  const agentConfig = useStore((s) => s.agentConfig);
  const loadAgentConfig = useStore((s) => s.loadAgentConfig);

  const [activeTab, setActiveTab] = useState('local');
  const [skillModal, setSkillModal] = useState<{ agentId: string; name: string; content: string; path: string } | null>(null);
  
  const [addFormVisible, setAddFormVisible] = useState(false);
  const [addFormAgentId, setAddFormAgentId] = useState('');
  const [addFormAgentLabel, setAddFormAgentLabel] = useState('');
  const [form] = Form.useForm();
  const [submitting, setSubmitting] = useState(false);

  const [remoteSkills, setRemoteSkills] = useState<RemoteSkillItem[]>([]);
  const [remoteLoading, setRemoteLoading] = useState(false);
  
  const [addRemoteFormVisible, setAddRemoteFormVisible] = useState(false);
  const [remoteForm] = Form.useForm();
  const [remoteSubmitting, setRemoteSubmitting] = useState(false);
  
  const [quickPickSource, setQuickPickSource] = useState<(typeof COMMUNITY_SOURCES)[0] | null>(null);
  const [quickPickAgent, setQuickPickAgent] = useState('');

  useEffect(() => {
    loadAgentConfig();
  }, [loadAgentConfig]);

  useEffect(() => {
    if (activeTab === 'remote') loadRemoteSkills();
  }, [activeTab]);

  const loadRemoteSkills = async () => {
    setRemoteLoading(true);
    try {
      const r = await api.remoteSkillsList();
      if (r.ok) setRemoteSkills(r.remoteSkills || []);
    } catch {
      message.error('远程技能列表加载失败');
    }
    setRemoteLoading(false);
  };

  const openSkill = async (agentId: string, skillName: string) => {
    setSkillModal({ agentId, name: skillName, content: '⟳ 加载中…', path: '' });
    try {
      const r = await api.skillContent(agentId, skillName);
      if (r.ok) {
        setSkillModal({ agentId, name: skillName, content: r.content || '', path: r.path || '' });
      } else {
        setSkillModal({ agentId, name: skillName, content: '❌ ' + (r.error || '无法读取'), path: '' });
      }
    } catch {
      setSkillModal({ agentId, name: skillName, content: '❌ 服务器连接失败', path: '' });
    }
  };

  const openAddForm = (agentId: string, agentLabel: string) => {
    setAddFormAgentId(agentId);
    setAddFormAgentLabel(agentLabel);
    form.resetFields();
    setAddFormVisible(true);
  };

  const submitAdd = async (values: any) => {
    setSubmitting(true);
    try {
      const r = await api.addSkill(addFormAgentId, values.name, values.desc, values.trigger);
      if (r.ok) {
        message.success(`✅ 技能 ${values.name} 已添加到 ${addFormAgentLabel}`);
        setAddFormVisible(false);
        loadAgentConfig();
      } else {
        message.error(r.error || '添加失败');
      }
    } catch {
      message.error('服务器连接失败');
    }
    setSubmitting(false);
  };

  const submitAddRemote = async (values: any) => {
    setRemoteSubmitting(true);
    try {
      const r = await api.addRemoteSkill(values.agentId, values.skillName, values.sourceUrl, values.description);
      if (r.ok) {
        message.success(`✅ 远程技能 ${values.skillName} 已添加到 ${values.agentId}`);
        setAddRemoteFormVisible(false);
        loadRemoteSkills();
        loadAgentConfig();
      } else {
        message.error(r.error || '添加失败');
      }
    } catch {
      message.error('服务器连接失败');
    }
    setRemoteSubmitting(false);
  };

  const handleUpdate = async (skill: RemoteSkillItem) => {
    try {
      const r = await api.updateRemoteSkill(skill.agentId, skill.skillName);
      if (r.ok) {
        message.success(`✅ 技能 ${skill.skillName} 已更新`);
        loadRemoteSkills();
      } else {
        message.error(r.error || '更新失败');
      }
    } catch {
      message.error('服务器连接失败');
    }
  };

  const handleRemove = async (skill: RemoteSkillItem) => {
    Modal.confirm({
      title: '确认删除',
      content: `是否删除远程技能 ${skill.skillName}？`,
      onOk: async () => {
        try {
          const r = await api.removeRemoteSkill(skill.agentId, skill.skillName);
          if (r.ok) {
            message.success(`🗑️ 技能 ${skill.skillName} 已移除`);
            loadRemoteSkills();
            loadAgentConfig();
          } else {
            message.error(r.error || '移除失败');
          }
        } catch {
          message.error('服务器连接失败');
        }
      }
    });
  };

  const handleQuickImport = async (skillUrl: string, skillName: string) => {
    if (!quickPickAgent) { message.error('请先选择目标 Agent'); return; }
    try {
      const r = await api.addRemoteSkill(quickPickAgent, skillName, skillUrl, '');
      if (r.ok) {
        message.success(`✅ ${skillName} → ${quickPickAgent}`);
        loadRemoteSkills();
        loadAgentConfig();
      } else {
        message.error(r.error || '导入失败');
      }
    } catch {
      message.error('服务器连接失败');
    }
  };

  if (!agentConfig?.agents) {
    return <Empty description="无法加载" style={{ marginTop: 60 }} />;
  }

  const localPanel = (
    <Row gutter={[16, 16]}>
      {agentConfig.agents.map((ag) => (
        <Col xs={24} sm={12} md={8} lg={8} xl={6} key={ag.id}>
          <Card 
            title={
              <Space>
                <span style={{ fontSize: 20 }}>{ag.emoji || '🏛️'}</span>
                <span>{ag.label}</span>
                <Tag style={{ marginLeft: 8 }}>{(ag.skills || []).length}</Tag>
              </Space>
            }
            bodyStyle={{ padding: 0 }}
            actions={[
              <Button type="link" icon={<PlusOutlined />} onClick={() => openAddForm(ag.id, ag.label)}>
                添加技能
              </Button>
            ]}
          >
            {!(ag.skills || []).length ? (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无 Skills" style={{ margin: '20px 0' }} />
            ) : (
              <div style={{ maxHeight: 250, overflowY: 'auto' }}>
                {(ag.skills || []).map((sk: any) => (
                  <div 
                    key={sk.name} 
                    onClick={() => openSkill(ag.id, sk.name)}
                    style={{ 
                      padding: '12px 16px', 
                      borderBottom: '1px solid #f0f0f0',
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center'
                    }}
                  >
                    <div style={{ flex: 1 }}>
                      <Text strong><FileTextOutlined /> {sk.name}</Text>
                      <div style={{ fontSize: 12, color: '#999', marginTop: 4 }}>
                        {sk.description || '无描述'}
                      </div>
                    </div>
                    <Text type="secondary">›</Text>
                  </div>
                ))}
              </div>
            )}
          </Card>
        </Col>
      ))}
    </Row>
  );

  const remotePanel = (
    <div>
      <Space style={{ marginBottom: 24 }} wrap>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => { setAddRemoteFormVisible(true); setQuickPickSource(null); }}>
          添加远程 Skill
        </Button>
        <Button icon={<SyncOutlined />} onClick={loadRemoteSkills} loading={remoteLoading}>
          刷新列表
        </Button>
        <Text type="secondary">共 {remoteSkills.length} 个远程技能</Text>
      </Space>

      <Card title="🌐 社区技能源 — 一键导入" style={{ marginBottom: 24 }}>
        <Space wrap style={{ marginBottom: quickPickSource ? 16 : 0 }}>
          {COMMUNITY_SOURCES.map((src) => (
            <Card.Grid 
              key={src.label}
              style={{ 
                width: 'auto', 
                padding: '8px 16px', 
                cursor: 'pointer',
                borderRadius: 8,
                background: quickPickSource?.label === src.label ? '#e6f4ff' : 'transparent',
                borderColor: quickPickSource?.label === src.label ? '#1677ff' : '#f0f0f0'
              }}
              onClick={() => setQuickPickSource(quickPickSource?.label === src.label ? null : src)}
            >
              <Space>
                <span>{src.emoji}</span>
                <Text strong>{src.label}</Text>
                <Tag color="gold">★ {src.stars}</Tag>
                <Text type="secondary">{src.desc}</Text>
              </Space>
            </Card.Grid>
          ))}
        </Space>

        {quickPickSource && (
          <div style={{ background: '#fafafa', padding: 16, borderRadius: 8 }}>
            <Space style={{ marginBottom: 16 }}>
              <Text strong>目标 Agent：</Text>
              <Select
                style={{ width: 200 }}
                placeholder="— 选择 Agent —"
                value={quickPickAgent}
                onChange={setQuickPickAgent}
                options={agentConfig.agents.map(ag => ({ label: `${ag.emoji} ${ag.label} (${ag.id})`, value: ag.id }))}
              />
            </Space>
            <Row gutter={[16, 16]}>
              {quickPickSource.skills.map((sk) => {
                const alreadyAdded = remoteSkills.some((r) => r.skillName === sk.name && r.agentId === quickPickAgent);
                return (
                  <Col xs={24} sm={12} md={8} key={sk.name}>
                    <Card size="small" style={{ background: '#fff' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <div>
                          <Text strong>📦 {sk.name}</Text>
                          <div style={{ fontSize: 10, color: '#999' }}>{sk.url.split('/').slice(-2).join('/')}</div>
                        </div>
                        {alreadyAdded ? (
                          <Text type="success" strong>✓ 已导入</Text>
                        ) : (
                          <Button size="small" type="primary" onClick={() => handleQuickImport(sk.url, sk.name)}>导入</Button>
                        )}
                      </div>
                    </Card>
                  </Col>
                );
              })}
            </Row>
          </div>
        )}
      </Card>

      {remoteLoading ? (
        <div style={{ textAlign: 'center', padding: '40px 0' }}><SyncOutlined spin /> 加载中…</div>
      ) : remoteSkills.length === 0 ? (
        <Empty 
          description={
            <span>
              尚无远程技能<br/>
              <Text type="secondary" style={{ fontSize: 12 }}>从社区技能源快速导入，或手动添加 URL</Text>
            </span>
          } 
          style={{ margin: '60px 0' }}
        />
      ) : (
        <Space direction="vertical" style={{ width: '100%' }}>
          {remoteSkills.map((sk) => {
            const key = `${sk.agentId}/${sk.skillName}`;
            const agInfo = agentConfig.agents.find((a) => a.id === sk.agentId);
            return (
              <Card key={key} size="small">
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <Space style={{ marginBottom: 8 }}>
                      <Text strong style={{ fontSize: 16 }}>📦 {sk.skillName}</Text>
                      <Tag color={sk.status === 'valid' ? 'success' : 'error'}>
                        {sk.status === 'valid' ? '✓ 有效' : '✗ 文件丢失'}
                      </Tag>
                      <Tag color="blue">{agInfo?.emoji} {agInfo?.label || sk.agentId}</Tag>
                    </Space>
                    {sk.description && (
                      <Paragraph type="secondary" style={{ marginBottom: 4 }}>{sk.description}</Paragraph>
                    )}
                    <Space size="large">
                      <Text type="secondary" style={{ fontSize: 12 }}>
                        🔗 <a href={sk.sourceUrl} target="_blank" rel="noreferrer">{sk.sourceUrl.length > 60 ? sk.sourceUrl.slice(0, 60) + '…' : sk.sourceUrl}</a>
                      </Text>
                      <Text type="secondary" style={{ fontSize: 12 }}>
                        📅 {sk.lastUpdated ? sk.lastUpdated.slice(0, 10) : sk.addedAt?.slice(0, 10)}
                      </Text>
                    </Space>
                  </div>
                  <Space>
                    <Button onClick={() => openSkill(sk.agentId, sk.skillName)}>查看</Button>
                    <Button type="primary" ghost icon={<SyncOutlined />} onClick={() => handleUpdate(sk)}>更新</Button>
                    <Button danger icon={<DeleteOutlined />} onClick={() => handleRemove(sk)}>删除</Button>
                  </Space>
                </div>
              </Card>
            );
          })}
        </Space>
      )}
    </div>
  );

  return (
    <div>
      <div style={{ marginBottom: 24 }}>
        <Title level={4}>御史台 - 技能库</Title>
        <Text type="secondary">管理扩展 AI 助手能力的技能（如读取 PDF、获取新闻等）。</Text>
      </div>

      <Tabs 
        activeKey={activeTab} 
        onChange={setActiveTab}
        items={[
          { key: 'local', label: `🏛️ 本地技能 (${agentConfig.agents.reduce((n, a) => n + (a.skills?.length || 0), 0)})`, children: localPanel },
          { key: 'remote', label: `🌐 远程技能 (${remoteSkills.length})`, children: remotePanel }
        ]}
      />

      {/* Skill Content Modal */}
      <Modal
        title={<Space><Text type="secondary">{skillModal?.agentId.toUpperCase()}</Text><Text strong>📦 {skillModal?.name}</Text></Space>}
        open={!!skillModal}
        onCancel={() => setSkillModal(null)}
        footer={null}
        width={800}
      >
        <div style={{ maxHeight: '60vh', overflowY: 'auto', background: '#f5f5f5', padding: 16, borderRadius: 8 }}>
          <pre style={{ whiteSpace: 'pre-wrap', fontSize: 12, margin: 0, fontFamily: 'monospace' }}>
            {skillModal?.content}
          </pre>
        </div>
        {skillModal?.path && (
          <div style={{ marginTop: 12 }}>
            <Text type="secondary" style={{ fontSize: 12 }}>📂 {skillModal.path}</Text>
          </div>
        )}
      </Modal>

      {/* Add Local Skill Modal */}
      <Modal
        title={`为 ${addFormAgentLabel} 添加技能`}
        open={addFormVisible}
        onCancel={() => setAddFormVisible(false)}
        footer={null}
      >
        <div style={{ background: '#e6f4ff', padding: 12, borderRadius: 8, marginBottom: 16 }}>
          <Text strong>📋 Skill 规范说明</Text><br/>
          <Text type="secondary">• 技能名称使用小写英文 + 连字符</Text><br/>
          <Text type="secondary">• 创建后会生成模板文件 SKILL.md</Text><br/>
          <Text type="secondary">• 技能会在 agent 收到相关任务时自动激活</Text>
        </div>
        
        <Form form={form} layout="vertical" onFinish={submitAdd}>
          <Form.Item name="name" label="技能名称" rules={[{ required: true, message: '请输入技能名称' }]}>
            <Input placeholder="如 data-analysis, code-review" />
          </Form.Item>
          <Form.Item name="desc" label="技能描述">
            <Input placeholder="一句话说明用途" />
          </Form.Item>
          <Form.Item name="trigger" label="触发条件（可选）">
            <Input placeholder="何时激活此技能" />
          </Form.Item>
          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Space>
              <Button onClick={() => setAddFormVisible(false)}>取消</Button>
              <Button type="primary" htmlType="submit" loading={submitting}>创建技能</Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      {/* Add Remote Skill Modal */}
      <Modal
        title="🌐 添加远程 Skill"
        open={addRemoteFormVisible}
        onCancel={() => setAddRemoteFormVisible(false)}
        footer={null}
      >
        <div style={{ background: '#f6ffed', padding: 12, borderRadius: 8, marginBottom: 16 }}>
          <Text type="secondary">支持 GitHub Raw URL，如：</Text><br/>
          <Text code style={{ fontSize: 10 }}>https://raw.githubusercontent.com/obra/superpowers/refs/heads/main/skills/brainstorming/SKILL.md</Text>
        </div>

        <Form form={remoteForm} layout="vertical" onFinish={submitAddRemote}>
          <Form.Item name="agentId" label="目标 Agent" rules={[{ required: true, message: '请选择目标 Agent' }]}>
            <Select options={agentConfig.agents.map((ag) => ({ label: `${ag.emoji} ${ag.label} (${ag.id})`, value: ag.id }))} />
          </Form.Item>
          <Form.Item name="skillName" label="技能名称" rules={[{ required: true, message: '请输入技能名称' }]}>
            <Input placeholder="如 brainstorming, code-review" />
          </Form.Item>
          <Form.Item name="sourceUrl" label="源 URL" rules={[{ required: true, message: '请输入源 URL' }]}>
            <Input placeholder="https://raw.githubusercontent.com/..." />
          </Form.Item>
          <Form.Item name="description" label="描述（可选）">
            <Input placeholder="一句话说明用途" />
          </Form.Item>
          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Space>
              <Button onClick={() => setAddRemoteFormVisible(false)}>取消</Button>
              <Button type="primary" htmlType="submit" loading={remoteSubmitting}>添加远程技能</Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default SkillLibrary;
