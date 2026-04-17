import React, { useEffect } from 'react';
import { Card, Row, Col, Typography, Statistic, Space, List, Tag, Progress, Avatar, Empty, Divider } from 'antd';
import { TrophyOutlined } from '@ant-design/icons';
import { useStore, STATE_LABEL } from '../../store';

const { Title, Text } = Typography;
const MEDALS = ['🥇', '🥈', '🥉'];

const OfficialManagement: React.FC = () => {
  const officialsData = useStore((s) => s.officialsData);
  const selectedOfficial = useStore((s) => s.selectedOfficial);
  const setSelectedOfficial = useStore((s) => s.setSelectedOfficial);
  const loadOfficials = useStore((s) => s.loadOfficials);
  const setModalTaskId = useStore((s) => s.setModalTaskId);

  useEffect(() => {
    loadOfficials();
  }, [loadOfficials]);

  if (!officialsData?.officials) {
    return <Empty description="⚠️ 请确保本地服务器已启动" style={{ marginTop: 60 }} />;
  }

  const offs = officialsData.officials;
  const totals = officialsData.totals || { tasks_done: 0, cost_cny: 0 };
  const maxTk = Math.max(...offs.map((o) => o.tokens_in + o.tokens_out + o.cache_read + o.cache_write), 1);

  const alive = offs.filter((o) => o.heartbeat?.status === 'active');
  const sel = offs.find((o) => o.id === (selectedOfficial || offs[0]?.id));
  const selId = sel?.id || offs[0]?.id;

  return (
    <div>
      <div style={{ marginBottom: 24 }}>
        <Title level={4}>御史台 - 官员管理</Title>
        <Text type="secondary">配置三省六部各个部门官员的权限、模型、功绩统计等。</Text>
      </div>

      {alive.length > 0 && (
        <Card size="small" style={{ marginBottom: 16, background: '#f6ffed', borderColor: '#b7eb8f' }}>
          <Space wrap>
            <Text type="success" strong>🟢 当前活跃：</Text>
            {alive.map((o) => (
              <Tag color="success" key={o.id}>{o.emoji} {o.role}</Tag>
            ))}
            <Text type="secondary" style={{ fontSize: 12, marginLeft: 16 }}>其余官员待命</Text>
          </Space>
        </Card>
      )}

      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={12} sm={12} md={6}>
          <Card>
            <Statistic title="在职官员" value={offs.length} valueStyle={{ color: '#1677ff' }} />
          </Card>
        </Col>
        <Col xs={12} sm={12} md={6}>
          <Card>
            <Statistic title="累计完成旨意" value={totals.tasks_done || 0} valueStyle={{ color: '#faad14' }} />
          </Card>
        </Col>
        <Col xs={12} sm={12} md={6}>
          <Card>
            <Statistic title="累计费用（含缓存）" value={`¥${totals.cost_cny || 0}`} valueStyle={{ color: (totals.cost_cny || 0) > 20 ? '#cf1322' : '#3f8600' }} />
          </Card>
        </Col>
        <Col xs={12} sm={12} md={6}>
          <Card>
            <Statistic title="功绩最高" value={officialsData.top_official || '—'} prefix={<TrophyOutlined />} valueStyle={{ color: '#faad14' }} />
          </Card>
        </Col>
      </Row>

      <Row gutter={16}>
        <Col xs={24} md={8}>
          <Card title="功绩排行" bodyStyle={{ padding: 0 }} style={{ height: '100%' }}>
            <List
              dataSource={offs}
              renderItem={o => {
                const hb = o.heartbeat || { status: 'idle' };
                const isSelected = selId === o.id;
                return (
                  <List.Item
                    onClick={() => setSelectedOfficial(o.id)}
                    style={{ 
                      padding: '12px 16px', 
                      cursor: 'pointer',
                      background: isSelected ? '#e6f4ff' : 'transparent',
                      borderLeft: isSelected ? '3px solid #1677ff' : '3px solid transparent'
                    }}
                  >
                    <List.Item.Meta
                      avatar={
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                          <div style={{ width: 24, textAlign: 'center', fontSize: 16 }}>
                            {o.merit_rank <= 3 ? MEDALS[o.merit_rank - 1] : <Text type="secondary">#{o.merit_rank}</Text>}
                          </div>
                          <Avatar size="large" style={{ backgroundColor: '#f0f2f5', fontSize: 20 }}>{o.emoji}</Avatar>
                        </div>
                      }
                      title={<Text strong>{o.role}</Text>}
                      description={<Text type="secondary" style={{ fontSize: 12 }}>{o.label}</Text>}
                    />
                    <div style={{ textAlign: 'right' }}>
                      <div style={{ color: '#1677ff', fontWeight: 'bold' }}>{o.merit_score}分</div>
                      <Tag color={hb.status === 'active' ? 'success' : hb.status === 'warn' ? 'warning' : 'default'} style={{ margin: '4px 0 0 0' }}>
                        {hb.status === 'active' ? '活跃' : '待命'}
                      </Tag>
                    </div>
                  </List.Item>
                );
              }}
            />
          </Card>
        </Col>

        <Col xs={24} md={16}>
          <Card style={{ height: '100%' }}>
            {sel ? (
              <div>
                <div style={{ display: 'flex', alignItems: 'flex-start', marginBottom: 24 }}>
                  <Avatar size={64} style={{ backgroundColor: '#f0f2f5', fontSize: 32, marginRight: 16 }}>{sel.emoji}</Avatar>
                  <div style={{ flex: 1 }}>
                    <Title level={4} style={{ margin: 0 }}>{sel.role}</Title>
                    <Space size={8} style={{ marginTop: 4 }}>
                      <Text type="secondary">{sel.label}</Text>
                      <Tag color="blue">{sel.model_short || sel.model}</Tag>
                    </Space>
                    <div style={{ marginTop: 8 }}>
                      <Text type="secondary">🏅 {sel.rank} · 功绩分 {sel.merit_score}</Text>
                    </div>
                  </div>
                  <div style={{ textAlign: 'right' }}>
                    <Tag color={sel.heartbeat?.status === 'active' ? 'success' : 'default'}>
                      {sel.heartbeat?.label || '⚪ 待命'}
                    </Tag>
                    <div style={{ fontSize: 12, color: '#999', marginTop: 8 }}>
                      {sel.last_active && <div>活跃 {sel.last_active}</div>}
                      <div>{sel.sessions} 个会话 · {sel.messages} 条消息</div>
                    </div>
                  </div>
                </div>

                <Divider plain>功绩统计</Divider>
                <Row gutter={16} style={{ marginBottom: 24, textAlign: 'center' }}>
                  <Col span={8}>
                    <Statistic title="完成旨意" value={sel.tasks_done} valueStyle={{ color: '#52c41a' }} />
                  </Col>
                  <Col span={8}>
                    <Statistic title="执行中" value={sel.tasks_active} valueStyle={{ color: '#faad14' }} />
                  </Col>
                  <Col span={8}>
                    <Statistic title="流转参与" value={sel.flow_participations} valueStyle={{ color: '#1677ff' }} />
                  </Col>
                </Row>

                <Divider plain>Token 消耗</Divider>
                <div style={{ marginBottom: 24 }}>
                  <div style={{ marginBottom: 12 }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                      <Text type="secondary">输入 ({sel.tokens_in.toLocaleString()})</Text>
                    </div>
                    <Progress percent={Math.round((sel.tokens_in / maxTk) * 100)} showInfo={false} strokeColor="#1677ff" />
                  </div>
                  <div style={{ marginBottom: 12 }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                      <Text type="secondary">输出 ({sel.tokens_out.toLocaleString()})</Text>
                    </div>
                    <Progress percent={Math.round((sel.tokens_out / maxTk) * 100)} showInfo={false} strokeColor="#722ed1" />
                  </div>
                  <div style={{ marginBottom: 12 }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                      <Text type="secondary">缓存读 ({sel.cache_read.toLocaleString()})</Text>
                    </div>
                    <Progress percent={Math.round((sel.cache_read / maxTk) * 100)} showInfo={false} strokeColor="#52c41a" />
                  </div>
                  <div style={{ marginBottom: 12 }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                      <Text type="secondary">缓存写 ({sel.cache_write.toLocaleString()})</Text>
                    </div>
                    <Progress percent={Math.round((sel.cache_write / maxTk) * 100)} showInfo={false} strokeColor="#faad14" />
                  </div>
                </div>

                <Divider plain>累计费用</Divider>
                <div style={{ marginBottom: 24 }}>
                  <Space size="large">
                    <Text strong style={{ color: sel.cost_cny > 10 ? '#cf1322' : sel.cost_cny > 3 ? '#faad14' : '#52c41a', fontSize: 16 }}>
                      ¥{sel.cost_cny}
                    </Text>
                    <Text type="secondary">${sel.cost_usd}</Text>
                    <Text type="secondary">总计 {(sel.tokens_in + sel.tokens_out + sel.cache_read + sel.cache_write).toLocaleString()} tokens</Text>
                  </Space>
                </div>

                <Divider plain>参与旨意 ({sel.participated_edicts?.length || 0} 道)</Divider>
                <List
                  size="small"
                  dataSource={sel.participated_edicts || []}
                  renderItem={(e: Record<string, string>) => (
                    <List.Item 
                      style={{ cursor: 'pointer' }}
                      onClick={() => setModalTaskId(e.id)}
                    >
                      <Space style={{ width: '100%', justifyContent: 'space-between' }}>
                        <Space>
                          <Text strong style={{ color: '#1677ff' }}>{e.id}</Text>
                          <Text>{e.title}</Text>
                        </Space>
                        <Tag>{STATE_LABEL[e.state] || e.state}</Tag>
                      </Space>
                    </List.Item>
                  )}
                  locale={{ emptyText: '暂无旨意记录' }}
                />
              </div>
            ) : (
              <Empty description="选择左侧官员查看详情" style={{ marginTop: 100 }} />
            )}
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default OfficialManagement;
