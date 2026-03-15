import React, { useEffect, useState } from 'react';
import { Card, Select, Button, Typography, Space, Row, Col, Empty, List, Tag, message } from 'antd';
import { useStore } from '../../store';
import { api } from '../../api';

const { Title, Text } = Typography;

const FALLBACK_MODELS = [
  { id: 'anthropic/claude-sonnet-4-6', l: 'Claude Sonnet 4.6', p: 'Anthropic' },
  { id: 'anthropic/claude-opus-4-5', l: 'Claude Opus 4.5', p: 'Anthropic' },
  { id: 'anthropic/claude-haiku-3-5', l: 'Claude Haiku 3.5', p: 'Anthropic' },
  { id: 'openai/gpt-4o', l: 'GPT-4o', p: 'OpenAI' },
  { id: 'openai/gpt-4o-mini', l: 'GPT-4o Mini', p: 'OpenAI' },
  { id: 'google/gemini-2.5-pro', l: 'Gemini 2.5 Pro', p: 'Google' },
  { id: 'copilot/claude-sonnet-4', l: 'Claude Sonnet 4', p: 'Copilot' },
  { id: 'copilot/claude-opus-4.5', l: 'Claude Opus 4.5', p: 'Copilot' },
  { id: 'copilot/gpt-4o', l: 'GPT-4o', p: 'Copilot' },
  { id: 'copilot/gemini-2.5-pro', l: 'Gemini 2.5 Pro', p: 'Copilot' },
];

const Models: React.FC = () => {
  const agentConfig = useStore((s) => s.agentConfig);
  const changeLog = useStore((s) => s.changeLog);
  const loadAgentConfig = useStore((s) => s.loadAgentConfig);

  const [selMap, setSelMap] = useState<Record<string, string>>({});
  const [loadingMap, setLoadingMap] = useState<Record<string, boolean>>({});

  useEffect(() => {
    loadAgentConfig();
  }, [loadAgentConfig]);

  useEffect(() => {
    if (agentConfig?.agents) {
      const m: Record<string, string> = {};
      agentConfig.agents.forEach((ag) => {
        m[ag.id] = ag.model;
      });
      setSelMap(m);
    }
  }, [agentConfig]);

  if (!agentConfig?.agents) {
    return <Empty description="⚠️ 请先启动本地服务器" style={{ marginTop: 60 }} />;
  }

  const models = agentConfig.knownModels?.length
    ? agentConfig.knownModels.map((m) => ({ id: m.id, l: m.label, p: m.provider }))
    : FALLBACK_MODELS;

  const handleSelect = (agentId: string, val: string) => {
    setSelMap((p) => ({ ...p, [agentId]: val }));
  };

  const resetMC = (agentId: string) => {
    const ag = agentConfig.agents.find((a) => a.id === agentId);
    if (ag) setSelMap((p) => ({ ...p, [agentId]: ag.model }));
  };

  const applyModel = async (agentId: string) => {
    const model = selMap[agentId];
    if (!model) return;
    setLoadingMap(p => ({ ...p, [agentId]: true }));
    try {
      const r = await api.setModel(agentId, model);
      if (r.ok) {
        message.success(`${agentId} 模型已更改，Gateway 重启中`);
        setTimeout(() => loadAgentConfig(), 5500);
      } else {
        message.error('❌ ' + (r.error || '错误'));
      }
    } catch {
      message.error('❌ 无法连接服务器');
    } finally {
      setLoadingMap(p => ({ ...p, [agentId]: false }));
    }
  };

  return (
    <div>
      <div style={{ marginBottom: 24 }}>
        <Title level={4}>大理寺 - 模型</Title>
        <Text type="secondary">配置 LLM 提供商并选择各部门 AI 助手使用的模型。</Text>
      </div>

      <Row gutter={[16, 16]}>
        {agentConfig.agents.map((ag) => {
          const sel = selMap[ag.id] || ag.model;
          const changed = sel !== ag.model;
          const isLoading = loadingMap[ag.id];

          return (
            <Col xs={24} sm={12} lg={8} xl={6} key={ag.id}>
              <Card>
                <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
                  <div style={{ fontSize: 32 }}>{ag.emoji || '🏛️'}</div>
                  <div>
                    <div style={{ fontSize: 16, fontWeight: 'bold' }}>
                      {ag.label} <Text type="secondary" style={{ fontSize: 12, fontWeight: 'normal' }}>{ag.id}</Text>
                    </div>
                    <Text type="secondary" style={{ fontSize: 12 }}>{ag.role}</Text>
                  </div>
                </div>

                <div style={{ marginBottom: 8 }}>
                  <Text type="secondary">当前模型: </Text>
                  <Text strong>{ag.model}</Text>
                </div>

                <Select
                  style={{ width: '100%', marginBottom: 16 }}
                  value={sel}
                  onChange={(val) => handleSelect(ag.id, val)}
                  options={models.map(m => ({ label: `${m.l} (${m.p})`, value: m.id }))}
                />

                <Space>
                  <Button 
                    type="primary" 
                    disabled={!changed} 
                    loading={isLoading}
                    onClick={() => applyModel(ag.id)}
                  >
                    应用
                  </Button>
                  <Button onClick={() => resetMC(ag.id)}>
                    重置
                  </Button>
                </Space>
              </Card>
            </Col>
          );
        })}
      </Row>

      <div style={{ marginTop: 32 }}>
        <Title level={5}>变更日志</Title>
        <Card bodyStyle={{ padding: 0 }}>
          <List
            dataSource={[...(changeLog || [])].reverse().slice(0, 15)}
            locale={{ emptyText: '暂无变更' }}
            renderItem={e => (
              <List.Item style={{ padding: '12px 24px' }}>
                <Row style={{ width: '100%' }} align="middle">
                  <Col span={6}>
                    <Text type="secondary">{(e.at || '').substring(0, 16).replace('T', ' ')}</Text>
                  </Col>
                  <Col span={4}>
                    <Text strong style={{ color: '#1677ff' }}>{e.agentId}</Text>
                  </Col>
                  <Col span={14}>
                    <Space>
                      <Text strong>{e.oldModel}</Text>
                      <Text type="secondary">→</Text>
                      <Text strong>{e.newModel}</Text>
                      {e.rolledBack && <Tag color="error">⚠ 已回滚</Tag>}
                    </Space>
                  </Col>
                </Row>
              </List.Item>
            )}
          />
        </Card>
      </div>
    </div>
  );
};

export default Models;
