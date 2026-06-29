import React, { useState } from 'react';
import { Card, Tag, Typography, Space, Button, Modal, Timeline, Empty, Segmented, message } from 'antd';
import { useStore, isTask, STATE_LABEL } from '../../store';
import { type Task, type FlowEntry } from '../../api';

const { Title, Text, Paragraph } = Typography;

const ReportDetailModal: React.FC<{ task: Task; onClose: () => void; onExport: (t: Task) => void }> = ({ task: t, onClose, onExport }) => {
  const fl = t.flow_log || [];
  const st = t.state || 'Unknown';
  const stIcon = st === 'Done' ? '✅' : st === 'Cancelled' ? '🚫' : '🔄';
  const depts = [...new Set(fl.map((f) => f.from).concat(fl.map((f) => f.to)).filter((x) => x && x !== '用户'))];

  // Reconstruct phases
  const originLog: FlowEntry[] = [];
  const planLog: FlowEntry[] = [];
  const reviewLog: FlowEntry[] = [];
  const execLog: FlowEntry[] = [];
  const resultLog: FlowEntry[] = [];
  
  for (const f of fl) {
    if (f.from === '用户') originLog.push(f);
    else if (f.to === '规划组' || f.from === '规划组') planLog.push(f);
    else if (f.to === '审查组' || f.from === '审查组') reviewLog.push(f);
    else if (f.remark && (f.remark.includes('完成') || f.remark.includes('响应'))) resultLog.push(f);
    else execLog.push(f);
  }

  const renderPhase = (title: string, icon: string, items: FlowEntry[]) => {
    if (!items.length) return null;
    return (
      <div style={{ marginBottom: 24 }}>
        <Title level={5} style={{ marginBottom: 16 }}>{icon} {title}</Title>
        <Timeline
          items={items.map((f) => ({
            color: f.remark?.includes('✅') ? 'green' : f.remark?.includes('驳') ? 'red' : 'blue',
            children: (
              <>
                <div style={{ marginBottom: 4 }}>
                  <Text strong style={{ color: '#1677ff' }}>{f.from}</Text>
                  <Text type="secondary" style={{ margin: '0 8px' }}>→</Text>
                  <Text>{f.to}</Text>
                </div>
                <Paragraph style={{ marginBottom: 4, fontSize: 13 }}>{f.remark}</Paragraph>
                <Text type="secondary" style={{ fontSize: 12 }}>{(f.at || '').substring(0, 19).replace('T', ' ')}</Text>
              </>
            ),
          }))}
        />
      </div>
    );
  };

  return (
    <Modal
      title={null}
      open={true}
      onCancel={onClose}
      footer={[
        <Button key="export" onClick={() => onExport(t)}>📋 复制报告</Button>
      ]}
      width={700}
    >
      <div style={{ marginBottom: 16 }}>
        <Text style={{ color: '#1677ff', fontWeight: 'bold', fontSize: 12 }}>{t.id}</Text>
        <Title level={4} style={{ marginTop: 4, marginBottom: 12 }}>{stIcon} {t.title || t.id}</Title>
        <Space wrap size={[8, 8]}>
          <Tag color={st === 'Done' ? 'success' : st === 'Cancelled' ? 'default' : 'processing'}>
            {STATE_LABEL[st] || st}
          </Tag>
          <Text type="secondary">{t.org}</Text>
          <Text type="secondary">流转 {fl.length} 步</Text>
          {depts.map((d) => (
            <Tag key={d}>{d}</Tag>
          ))}
        </Space>
      </div>

      {t.now && (
        <div style={{ background: '#fafafa', padding: 12, borderRadius: 8, marginBottom: 24 }}>
          <Text type="secondary">{t.now}</Text>
        </div>
      )}

      {renderPhase('任务原文', '👑', originLog)}
      {renderPhase('任务规划', '📋', planLog)}
      {renderPhase('方案审查', '🔍', reviewLog)}
      {renderPhase('Agent执行', '⚔️', execLog)}
      {renderPhase('汇总响应', '📨', resultLog)}

      {t.output && t.output !== '-' && (
        <div style={{ marginTop: 24, paddingTop: 16, borderTop: '1px solid #f0f0f0' }}>
          <Title level={5}>📦 产出物</Title>
          <div style={{ background: '#f5f5f5', padding: 12, borderRadius: 8, wordBreak: 'break-all' }}>
            <code>{t.output}</code>
          </div>
        </div>
      )}
    </Modal>
  );
};

const Reports: React.FC = () => {
  const liveStatus = useStore((s) => s.liveStatus);
  const [filter, setFilter] = useState('all');
  const [detailTask, setDetailTask] = useState<Task | null>(null);

  const tasks = liveStatus?.tasks || [];
  let mems = tasks.filter((t) => isTask(t) && ['Done', 'Cancelled'].includes(t.state));
  if (filter !== 'all') mems = mems.filter((t) => t.state === filter);

  const exportReport = (t: Task) => {
    const fl = t.flow_log || [];
    let md = `# 📋 报告 · ${t.title}\n\n`;
    md += `- **任务编号**: ${t.id}\n`;
    md += `- **状态**: ${t.state}\n`;
    md += `- **负责部门**: ${t.org}\n`;
    if (fl.length) {
      const startAt = fl[0].at ? fl[0].at.substring(0, 19).replace('T', ' ') : '未知';
      const endAt = fl[fl.length - 1].at ? fl[fl.length - 1].at.substring(0, 19).replace('T', ' ') : '未知';
      md += `- **开始时间**: ${startAt}\n`;
      md += `- **完成时间**: ${endAt}\n`;
    }
    md += `\n## 流转记录\n\n`;
    for (const f of fl) {
      md += `- **${f.from}** → **${f.to}**  \n  ${f.remark}  \n  _${(f.at || '').substring(0, 19)}_\n\n`;
    }
    if (t.output && t.output !== '-') md += `## 产出物\n\n\`${t.output}\`\n`;
    
    navigator.clipboard.writeText(md).then(
      () => message.success('✅ 报告已复制为 Markdown'),
      () => message.error('复制失败')
    );
  };

  return (
    <div>
      <div style={{ marginBottom: 24 }}>
        <Title level={4}>任务中心 - 报告</Title>
        <Text type="secondary">管理任务完成后形成的结果，供用户审阅。</Text>
      </div>

      <div style={{ marginBottom: 24 }}>
        <Space align="center">
          <Text type="secondary">筛选：</Text>
          <Segmented
            options={[
              { label: '全部', value: 'all' },
              { label: '✅ 已完成', value: 'Done' },
              { label: '🚫 已取消', value: 'Cancelled' }
            ]}
            value={filter}
            onChange={(val) => setFilter(val as string)}
          />
        </Space>
      </div>

      {!mems.length ? (
        <Empty 
          description="暂无报告 — 任务完成后自动生成" 
          style={{ margin: '60px 0' }}
        />
      ) : (
        <Space direction="vertical" size="middle" style={{ display: 'flex' }}>
          {mems.map((t) => {
            const fl = t.flow_log || [];
            const depts = [...new Set(fl.map((f) => f.from).concat(fl.map((f) => f.to)).filter((x) => x && x !== '用户'))];
            const firstAt = fl.length ? (fl[0].at || '').substring(0, 16).replace('T', ' ') : '';
            const lastAt = fl.length ? (fl[fl.length - 1].at || '').substring(0, 16).replace('T', ' ') : '';
            const stIcon = t.state === 'Done' ? '✅' : '🚫';

            return (
              <Card 
                key={t.id} 
                hoverable 
                onClick={() => setDetailTask(t)}
                bodyStyle={{ padding: 16, display: 'flex', gap: 16 }}
              >
                <div style={{ fontSize: 32, lineHeight: 1 }}>📜</div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <Title level={5} style={{ margin: '0 0 4px 0' }}>
                    {stIcon} {t.title || t.id}
                  </Title>
                  <Text type="secondary" style={{ fontSize: 13 }}>
                    {t.id} · {t.org || ''} · 流转 {fl.length} 步
                  </Text>
                  <div style={{ marginTop: 8 }}>
                    <Space size={4} wrap>
                      {depts.slice(0, 5).map((d) => (
                        <Tag key={d}>{d}</Tag>
                      ))}
                    </Space>
                  </div>
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', justifyContent: 'center', minWidth: 120 }}>
                  <Text type="secondary" style={{ fontSize: 12 }}>{firstAt}</Text>
                  {lastAt !== firstAt && <Text type="secondary" style={{ fontSize: 12 }}>{lastAt}</Text>}
                </div>
              </Card>
            );
          })}
        </Space>
      )}

      {detailTask && (
        <ReportDetailModal 
          task={detailTask} 
          onClose={() => setDetailTask(null)} 
          onExport={exportReport} 
        />
      )}
    </div>
  );
};

export default Reports;
