import React, { useEffect } from 'react';
import { Card, Tag, Button, Typography, Space, Row, Col, Progress, message, Empty, Segmented } from 'antd';
import { 
  PauseCircleOutlined, 
  StopOutlined, 
  PlayCircleOutlined, 
  InboxOutlined,
  CompassOutlined,
  ExclamationCircleOutlined
} from '@ant-design/icons';
import { useStore, isTask, isArchived, getPipeStatus, stateLabel, deptColor, PIPE } from '../../store';
import { api, type Task } from '../../api';

const { Title, Text, Paragraph } = Typography;

const STATE_ORDER: Record<string, number> = {
  Doing: 0, Review: 1, Assigned: 2, Menxia: 3, Zhongshu: 4,
  Taizi: 5, Inbox: 6, Blocked: 7, Next: 8, Done: 9, Cancelled: 10,
};

const TaskCard: React.FC<{ task: Task }> = ({ task }) => {
  const setModalTaskId = useStore((s) => s.setModalTaskId);
  const loadAll = useStore((s) => s.loadAll);

  const hb = task.heartbeat || { status: 'unknown', label: '⚪' };
  const curStage = PIPE.find((_, i) => getPipeStatus(task)[i].status === 'active');
  const todos = task.todos || [];
  const todoDone = todos.filter((x) => x.status === 'completed').length;
  const todoTotal = todos.length;
  const canStop = !['Done', 'Blocked', 'Cancelled'].includes(task.state);
  const canResume = ['Blocked', 'Cancelled'].includes(task.state);
  const archived = isArchived(task);
  const isBlocked = task.block && task.block !== '无' && task.block !== '-';

  const handleAction = async (action: string, e: React.MouseEvent) => {
    e.stopPropagation();
    if (action === 'stop' || action === 'cancel') {
      const reason = window.prompt(action === 'stop' ? '请输入叫停原因：' : '请输入取消原因：');
      if (reason === null) return;
      try {
        const r = await api.taskAction(task.id, action, reason);
        if (r.ok) { message.success(r.message || '操作成功'); loadAll(); }
        else message.error(r.error || '操作失败');
      } catch { message.error('服务器连接失败'); }
    } else if (action === 'resume') {
      try {
        const r = await api.taskAction(task.id, 'resume', '恢复执行');
        if (r.ok) { message.success(r.message || '已恢复'); loadAll(); }
        else message.error(r.error || '操作失败');
      } catch { message.error('服务器连接失败'); }
    }
  };

  const handleArchive = async (e: React.MouseEvent) => {
    e.stopPropagation();
    try {
      const r = await api.archiveTask(task.id, !task.archived);
      if (r.ok) { message.success(r.message || '操作成功'); loadAll(); }
      else message.error(r.error || '操作失败');
    } catch { message.error('服务器连接失败'); }
  };

  return (
    <Card 
      hoverable 
      style={{ opacity: archived ? 0.7 : 1, borderStyle: archived ? 'dashed' : 'solid' }}
      onClick={() => setModalTaskId(task.id)}
      bodyStyle={{ padding: 16 }}
    >
      <div style={{ display: 'flex', alignItems: 'center', marginBottom: 12, overflowX: 'auto', paddingBottom: 4 }}>
        {getPipeStatus(task).map((s, i, arr) => (
          <React.Fragment key={s.key}>
            <div style={{ 
              display: 'flex', flexDirection: 'column', alignItems: 'center', minWidth: 50,
              opacity: s.status === 'pending' ? 0.3 : 1,
              background: s.status === 'active' ? '#e6f4ff' : s.status === 'done' ? '#f6ffed' : 'transparent',
              border: s.status === 'active' ? '1px solid #1677ff' : '1px solid transparent',
              borderRadius: 6, padding: '4px 8px'
            }}>
              <span style={{ fontSize: 16 }}>{s.icon}</span>
              <span style={{ fontSize: 10, color: s.status === 'done' ? '#52c41a' : s.status === 'active' ? '#1677ff' : '#999' }}>{s.dept}</span>
            </div>
            {i < arr.length - 1 && <span style={{ color: '#ccc', margin: '0 4px' }}>›</span>}
          </React.Fragment>
        ))}
      </div>

      <div style={{ color: '#1677ff', fontSize: 12, fontWeight: 'bold', marginBottom: 4 }}>{task.id}</div>
      <div style={{ fontSize: 16, fontWeight: 'bold', marginBottom: 8, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
        {task.title || '(无标题)'}
      </div>

      <Space size={4} wrap style={{ marginBottom: 8 }}>
        <Tag color="blue">{stateLabel(task)}</Tag>
        {task.org && <Tag color="geekblue">{task.org}</Tag>}
        {curStage && (
          <Text type="secondary" style={{ fontSize: 12 }}>
            当前: <span style={{ color: deptColor(curStage.dept), fontWeight: 'bold' }}>{curStage.dept} · {curStage.action}</span>
          </Text>
        )}
      </Space>

      {task.now && task.now !== '-' && (
        <Paragraph type="secondary" ellipsis={{ rows: 2 }} style={{ fontSize: 12, marginBottom: 8 }}>
          {task.now}
        </Paragraph>
      )}

      {todoTotal > 0 && (
        <div style={{ marginBottom: 12 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, marginBottom: 4 }}>
            <span>📋 {todoDone}/{todoTotal}</span>
            <Text type="secondary">{todoDone === todoTotal ? '✅ 全部完成' : '🔄 进行中'}</Text>
          </div>
          <Progress percent={Math.round((todoDone / todoTotal) * 100)} size="small" showInfo={false} status={todoDone === todoTotal ? "success" : "active"} />
        </div>
      )}

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12, fontSize: 12 }}>
        <Tag color={hb.status === 'active' ? 'success' : hb.status === 'warn' ? 'warning' : hb.status === 'stalled' ? 'error' : 'default'}>
          {hb.label}
        </Tag>
        {isBlocked && (
          <Tag icon={<ExclamationCircleOutlined />} color="error">
            {task.block}
          </Tag>
        )}
        {task.eta && task.eta !== '-' && (
          <Text type="secondary">📅 {task.eta}</Text>
        )}
      </div>

      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }} onClick={e => e.stopPropagation()}>
        {canStop && (
          <>
            <Button size="small" danger icon={<PauseCircleOutlined />} onClick={e => handleAction('stop', e)}>叫停</Button>
            <Button size="small" danger type="primary" icon={<StopOutlined />} onClick={e => handleAction('cancel', e)}>取消</Button>
          </>
        )}
        {canResume && (
          <Button size="small" type="primary" icon={<PlayCircleOutlined />} onClick={e => handleAction('resume', e)}>恢复</Button>
        )}
        {archived && !task.archived && (
          <Button size="small" icon={<InboxOutlined />} onClick={handleArchive}>归档</Button>
        )}
        {task.archived && (
          <Button size="small" icon={<InboxOutlined />} onClick={handleArchive}>取消归档</Button>
        )}
      </div>
    </Card>
  );
};

const TaskBoard: React.FC = () => {
  const liveStatus = useStore((s) => s.liveStatus);
  const taskFilter = useStore((s) => s.taskFilter);
  const setTaskFilter = useStore((s) => s.setTaskFilter);
  const loadAll = useStore((s) => s.loadAll);

  useEffect(() => {
    loadAll();
    const timer = setInterval(() => {
      loadAll();
    }, 5000);
    return () => clearInterval(timer);
  }, [loadAll]);

  const tasks = liveStatus?.tasks || [];
  const allTasks = tasks.filter(isTask);
  const activeTasks = allTasks.filter((t) => !isArchived(t));
  const archivedTasks = allTasks.filter((t) => isArchived(t));

  let edicts: Task[];
  if (taskFilter === 'active') tasks = activeTasks;
  else if (taskFilter === 'archived') tasks = archivedTasks;
  else tasks = allTasks;

  edicts.sort((a, b) => (STATE_ORDER[a.state] ?? 9) - (STATE_ORDER[b.state] ?? 9));

  const unArchivedDone = allTasks.filter((t) => !t.archived && ['Done', 'Cancelled'].includes(t.state));

  const handleArchiveAll = async () => {
    if (!window.confirm('将所有已完成/已取消的任务移入归档？')) return;
    try {
      const r = await api.archiveAllDone();
      if (r.ok) { message.success(`📦 ${r.count || 0} 个任务已归档`); loadAll(); }
      else message.error(r.error || '批量归档失败');
    } catch { message.error('服务器连接失败'); }
  };

  const handleScan = async () => {
    try {
      const r = await api.schedulerScan();
      if (r.ok) message.success(`🧭 助理巡检完成：${r.count || 0} 个动作`);
      else message.error(r.error || '巡检失败');
      loadAll();
    } catch { message.error('服务器连接失败'); }
  };

  return (
    <div>
      <div style={{ marginBottom: 24 }}>
        <Title level={4}>任务中心 - 任务看板</Title>
        <Text type="secondary">查看、筛选和管理所有任务，支持叫停、恢复、取消和归档操作。</Text>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginBottom: 24, flexWrap: 'wrap' }}>
        <Segmented
          options={[
            { label: '活跃', value: 'active' },
            { label: '归档', value: 'archived' },
            { label: '全部', value: 'all' }
          ]}
          value={taskFilter}
          onChange={(val) => setTaskFilter(val as 'active' | 'archived' | 'all')}
        />
        
        {unArchivedDone.length > 0 && (
          <Button icon={<InboxOutlined />} onClick={handleArchiveAll}>一键归档</Button>
        )}
        
        <Text type="secondary" style={{ marginLeft: 'auto' }}>
          活跃 {activeTasks.length} · 归档 {archivedTasks.length} · 共 {allTasks.length}
        </Text>
        
        <Button type="primary" ghost icon={<CompassOutlined />} onClick={handleScan}>助理巡检</Button>
      </div>

      {edicts.length === 0 ? (
        <Empty 
          description={
            <span>
              暂无任务<br/>
              <Text type="secondary" style={{ fontSize: 12 }}>通过消息渠道发送任务，助理路由后转规划组处理</Text>
            </span>
          } 
          style={{ margin: '40px 0' }}
        />
      ) : (
        <Row gutter={[16, 16]}>
          {edicts.map((t) => (
            <Col xs={24} sm={12} lg={8} xl={6} key={t.id}>
              <TaskCard task={t} />
            </Col>
          ))}
        </Row>
      )}
    </div>
  );
};

export default TaskBoard;
