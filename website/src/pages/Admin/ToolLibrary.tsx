import React, { useState, useEffect, useCallback } from 'react';
import { Typography, Card, Row, Col, Switch, Space, message, Badge, Button, Select, Tag, Collapse, Input, Drawer, Tooltip, Popconfirm, Spin, Empty } from 'antd';
import { SyncOutlined, SearchOutlined, CloudServerOutlined, StopOutlined, DeleteOutlined, ClearOutlined, ReloadOutlined, DesktopOutlined, GlobalOutlined, FileOutlined, MobileOutlined, CodeOutlined, QuestionCircleOutlined } from '@ant-design/icons';
import { api } from '../../api';
import type { SandboxInfo, SandboxHealth } from '../../api';

// ── 本地类型定义（避免跨模块 export interface 的 ESM 运行时问题） ──

interface ToolExample {
  title: string;
  input: string;
}

interface ToolConfig {
  toolName: string;
  description: string;
  category: 'builtin' | 'sandbox' | 'browser' | 'custom';
  runEnvironment: 'system' | 'sandbox';
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  enabled: boolean;
  approvalRequired: boolean;
  denyPatterns: string[];
  examples: ToolExample[];
  customized: boolean;
  updatedAt: string | null;
}

const { Title, Text, Paragraph } = Typography;
const { Option } = Select;
const { Panel } = Collapse;

const riskLevelColors: Record<string, string> = {
  LOW: 'green',
  MEDIUM: 'orange',
  HIGH: 'red',
  CRITICAL: 'magenta',
};

const riskLevelLabels: Record<string, string> = {
  LOW: '低风险',
  MEDIUM: '中风险',
  HIGH: '高风险',
  CRITICAL: '严重风险',
};

const categoryLabels: Record<string, string> = {
  builtin: '内置工具',
  sandbox: '沙盒工具',
  browser: '浏览器工具',
  custom: '自定义工具',
};

// 沙箱类型配置
const sandboxTypeConfig: Record<string, { icon: React.ReactNode; label: string; color: string }> = {
  base: { icon: <CodeOutlined />, label: '基础沙箱', color: 'blue' },
  browser: { icon: <GlobalOutlined />, label: '浏览器沙箱', color: 'purple' },
  filesystem: { icon: <FileOutlined />, label: '文件系统沙箱', color: 'cyan' },
  gui: { icon: <DesktopOutlined />, label: 'GUI 沙箱', color: 'geekblue' },
  mobile: { icon: <MobileOutlined />, label: '移动端沙箱', color: 'volcano' },
  training: { icon: <CodeOutlined />, label: '训练沙箱', color: 'gold' },
  unknown: { icon: <QuestionCircleOutlined />, label: '未知类型', color: 'default' },
};

// 沙箱状态判断
const isRunningStatus = (status: string): boolean => {
  if (!status) return false;
  const s = status.toLowerCase();
  return s === 'running' || s === 'created' || s === 'partiallyready' || s === 'pending' || s === 'starting';
};

const statusConfig: Record<string, { color: string; text: string }> = {
  running: { color: 'success', text: '运行中' },
  created: { color: 'processing', text: '已创建' },
  starting: { color: 'processing', text: '启动中' },
  pending: { color: 'processing', text: '等待中' },
  stopped: { color: 'default', text: '已停止' },
  unknown: { color: 'warning', text: '未知' },
};

const ToolLibrary: React.FC = () => {
  const [tools, setTools] = useState<ToolConfig[]>([]);
  const [loading, setLoading] = useState(false);
  const [syncing, setSyncing] = useState(false);
  const [filter, setFilter] = useState<string>('all');
  const [search, setSearch] = useState('');

  // 沙箱相关状态
  const [sandboxes, setSandboxes] = useState<SandboxInfo[]>([]);
  const [health, setHealth] = useState<SandboxHealth | null>(null);
  const [sandboxLoading, setSandboxLoading] = useState(false);
  const [autoRefresh, setAutoRefresh] = useState(true);
  const [drawerVisible, setDrawerVisible] = useState(false);
  const [selectedSandbox, setSelectedSandbox] = useState<SandboxInfo | null>(null);

  // 加载工具列表
  const loadTools = async () => {
    setLoading(true);
    try {
      const data = await api.listToolConfigs();
      setTools(data);
    } catch {
      message.error('加载工具配置失败');
    } finally {
      setLoading(false);
    }
  };

  // 加载沙箱列表
  const loadSandboxes = useCallback(async () => {
    try {
      const data = await api.listSandboxes();
      setSandboxes(data);
    } catch {
      // 静默失败，不打扰用户
    }
  }, []);

  // 加载沙箱健康状态
  const loadHealth = useCallback(async () => {
    try {
      const data = await api.getSandboxHealth();
      setHealth(data);
    } catch {
      // 静默失败
    }
  }, []);

  // 刷新沙箱数据
  const refreshSandboxData = useCallback(async () => {
    setSandboxLoading(true);
    await Promise.all([loadSandboxes(), loadHealth()]);
    setSandboxLoading(false);
  }, [loadSandboxes, loadHealth]);

  useEffect(() => {
    loadTools();
    refreshSandboxData();
  }, []);

  // 自动刷新
  useEffect(() => {
    if (!autoRefresh) return;
    const timer = setInterval(() => {
      loadSandboxes();
      loadHealth();
    }, 10000);
    return () => clearInterval(timer);
  }, [autoRefresh, loadSandboxes, loadHealth]);

  // 同步系统工具
  const handleSync = async () => {
    setSyncing(true);
    try {
      const result = await api.syncSystemTools();
      message.success(result.message);
      await loadTools();
    } catch {
      message.error('同步失败');
    } finally {
      setSyncing(false);
    }
  };

  // 切换启用状态
  const toggleEnable = async (toolName: string, checked: boolean) => {
    try {
      await api.updateToolConfig(toolName, { enabled: checked });
      message.success(`工具已${checked ? '启用' : '禁用'}`);
      await loadTools();
    } catch {
      message.error('更新失败');
    }
  };

  // 修改风险等级
  const handleRiskChange = async (toolName: string, newLevel: string) => {
    try {
      await api.updateToolConfig(toolName, { riskLevel: newLevel as any });
      message.success('风险等级已更新');
      await loadTools();
    } catch {
      message.error('更新失败');
    }
  };

  // 切换审批要求
  const toggleApproval = async (toolName: string, checked: boolean) => {
    try {
      await api.updateToolConfig(toolName, { approvalRequired: checked });
      message.success(`审批要求已${checked ? '开启' : '关闭'}`);
      await loadTools();
    } catch {
      message.error('更新失败');
    }
  };

  // 停止沙箱
  const handleStopSandbox = async (containerId: string) => {
    try {
      const result = await api.stopSandbox(containerId);
      if (result.success) {
        message.success('沙箱已停止');
        await refreshSandboxData();
      } else {
        message.error('停止沙箱失败');
      }
    } catch {
      message.error('停止沙箱失败');
    }
  };

  // 删除沙箱
  const handleRemoveSandbox = async (containerId: string) => {
    try {
      const result = await api.removeSandbox(containerId);
      if (result.success) {
        message.success('沙箱已删除');
        setDrawerVisible(false);
        await refreshSandboxData();
      } else {
        message.error('删除沙箱失败');
      }
    } catch {
      message.error('删除沙箱失败');
    }
  };

  // 清理所有沙箱
  const handleCleanupAll = async () => {
    try {
      const result = await api.cleanupSandboxes();
      message.success(`已清理 ${result.removed} 个沙箱`);
      await refreshSandboxData();
    } catch {
      message.error('清理失败');
    }
  };

  // 打开沙箱详情
  const openSandboxDetail = (sandbox: SandboxInfo) => {
    setSelectedSandbox(sandbox);
    setDrawerVisible(true);
  };

  // 判断工具是否为沙箱类工具
  const isSandboxTool = (tool: ToolConfig): boolean => {
    return tool.category === 'sandbox' || tool.category === 'browser';
  };

  // 获取沙箱类工具对应的沙箱运行状态
  const getSandboxStatusForTool = (tool: ToolConfig): boolean => {
    if (sandboxes.length === 0) return false;
    if (tool.category === 'browser') {
      return sandboxes.some(s => s.sandboxType === 'browser' && isRunningStatus(s.status));
    }
    if (tool.category === 'sandbox') {
      return sandboxes.some(s => (s.sandboxType === 'base' || s.sandboxType === 'filesystem') && isRunningStatus(s.status));
    }
    return false;
  };

  // 过滤工具
  const filteredTools = tools.filter(tool => {
    const matchCategory = filter === 'all' || tool.category === filter;
    const matchSearch = !search ||
      tool.toolName.toLowerCase().includes(search.toLowerCase()) ||
      tool.description.toLowerCase().includes(search.toLowerCase());
    return matchCategory && matchSearch;
  });

  return (
    <div style={{ height: 'calc(100vh - 120px)', display: 'flex', flexDirection: 'column' }}>
      {/* 顶部操作栏 */}
      <div style={{ marginBottom: 16 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
          <div>
            <Title level={4} style={{ margin: 0 }}>工具库</Title>
            <Text type="secondary">
              管理智能体可用工具、风险等级和启用状态。禁用的工具将不会提供给智能体使用。
            </Text>
          </div>
          <Button
            icon={<SyncOutlined />}
            onClick={handleSync}
            loading={syncing}
          >
            同步系统工具
          </Button>
        </div>

        {/* 筛选栏 */}
        <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
          <Select
            value={filter}
            onChange={setFilter}
            style={{ width: 140 }}
          >
            <Option value="all">全部分类</Option>
            <Option value="builtin">内置工具</Option>
            <Option value="sandbox">沙盒工具</Option>
            <Option value="browser">浏览器工具</Option>
          </Select>

          <Input
            placeholder="搜索工具名称或描述..."
            prefix={<SearchOutlined />}
            value={search}
            onChange={e => setSearch(e.target.value)}
            style={{ width: 280 }}
          />

          <Text type="secondary">共 {filteredTools.length} 个工具</Text>
        </div>
      </div>

      {/* 主内容区：左侧工具列表 + 右侧沙箱面板 */}
      <div style={{ flex: 1, display: 'flex', gap: 16, overflow: 'hidden' }}>

        {/* 左侧：工具卡片列表 */}
        <div style={{ flex: 1, overflowY: 'auto' }}>
          {loading ? (
            <div style={{ textAlign: 'center', padding: '40px 0' }}>
              <Spin tip="加载中..." />
            </div>
          ) : filteredTools.length === 0 ? (
            <Empty description="暂无工具" />
          ) : (
            <Row gutter={[16, 16]}>
              {filteredTools.map(tool => (
                <Col xs={24} sm={12} md={12} lg={8} xl={6} key={tool.toolName}>
                  <Card
                    hoverable
                    style={{
                      height: '100%',
                      display: 'flex',
                      flexDirection: 'column',
                      opacity: tool.enabled ? 1 : 0.6
                    }}
                    bodyStyle={{ flex: 1, display: 'flex', flexDirection: 'column' }}
                  >
                    {/* 工具名称和状态 */}
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 12 }}>
                      <Text strong style={{ fontSize: 14, flex: 1 }}>{tool.toolName}</Text>
                      <Space>
                        <Badge
                          status={tool.enabled ? 'success' : 'default'}
                          text={tool.enabled ? '已启用' : '已禁用'}
                        />
                      </Space>
                    </div>

                    {/* 风险等级 + 分类 + 沙箱状态 */}
                    <div style={{ marginBottom: 8 }}>
                      <Space wrap>
                        <Tag color={riskLevelColors[tool.riskLevel]}>
                          {riskLevelLabels[tool.riskLevel]}
                        </Tag>
                        <Tag color="blue">
                          {categoryLabels[tool.category]}
                        </Tag>
                        {isSandboxTool(tool) && (
                          <Tooltip title={getSandboxStatusForTool(tool) ? '沙箱运行中' : '沙箱未运行'}>
                            <Badge
                              status={getSandboxStatusForTool(tool) ? 'success' : 'default'}
                              style={{ marginLeft: 4 }}
                            />
                          </Tooltip>
                        )}
                      </Space>
                    </div>

                    {/* 描述 */}
                    <div style={{ flex: 1, marginBottom: 12 }}>
                      <Paragraph type="secondary" ellipsis={{ rows: 2 }} style={{ marginBottom: 0 }}>
                        {tool.description}
                      </Paragraph>
                    </div>

                    {/* 使用示例 */}
                    {tool.examples && tool.examples.length > 0 && (
                      <Collapse
                        size="small"
                        style={{ marginBottom: 12, background: 'transparent' }}
                      >
                        <Panel header="使用示例" key="examples">
                          {tool.examples.slice(0, 2).map((ex, idx) => (
                            <div key={idx} style={{ marginBottom: 8 }}>
                              <Text strong>{ex.title}</Text>
                              <pre style={{
                                background: 'var(--td-code-bg)',
                                padding: '4px 8px',
                                borderRadius: 4,
                                fontSize: 12,
                                overflow: 'auto',
                                maxHeight: 60,
                                color: 'var(--td-code-text)',
                                border: '1px solid var(--td-code-border)'
                              }}>
                                {ex.input}
                              </pre>
                            </div>
                          ))}
                        </Panel>
                      </Collapse>
                    )}

                    {/* 控制区 */}
                    <div style={{
                      marginTop: 'auto',
                      borderTop: '1px solid var(--td-border-light)',
                      paddingTop: 12,
                      display: 'flex',
                      flexDirection: 'column',
                      gap: 8
                    }}>
                      {/* 风险等级选择 */}
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <Text style={{ fontSize: 12 }}>风险等级:</Text>
                        <Select
                          value={tool.riskLevel}
                          onChange={val => handleRiskChange(tool.toolName, val)}
                          size="small"
                          style={{ width: 120 }}
                        >
                          <Option value="LOW">低风险</Option>
                          <Option value="MEDIUM">中风险</Option>
                          <Option value="HIGH">高风险</Option>
                          <Option value="CRITICAL">严重风险</Option>
                        </Select>
                      </div>

                      {/* 审批开关 */}
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <Text style={{ fontSize: 12 }}>需要审批:</Text>
                        <Switch
                          checked={tool.approvalRequired}
                          onChange={val => toggleApproval(tool.toolName, val)}
                          size="small"
                        />
                      </div>

                      {/* 启用开关 */}
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <Text style={{ fontSize: 12 }}>启用工具:</Text>
                        <Switch
                          checked={tool.enabled}
                          onChange={val => toggleEnable(tool.toolName, val)}
                        />
                      </div>
                    </div>
                  </Card>
                </Col>
              ))}
            </Row>
          )}
        </div>

        {/* 右侧：沙箱服务面板 */}
        <div style={{
          width: 300,
          flexShrink: 0,
          borderLeft: '1px solid var(--td-border-light)',
          paddingLeft: 16,
          display: 'flex',
          flexDirection: 'column',
          overflow: 'hidden'
        }}>
          {/* 面板标题 */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
            <Space>
              <CloudServerOutlined />
              <Text strong>沙箱服务</Text>
            </Space>
            <Tooltip title={autoRefresh ? '关闭自动刷新' : '开启自动刷新 (10s)'}>
              <Switch
                checked={autoRefresh}
                onChange={setAutoRefresh}
                checkedChildren={<ReloadOutlined />}
                unCheckedChildren={<ReloadOutlined />}
                size="small"
              />
            </Tooltip>
          </div>

          {/* 健康摘要 */}
          {health && (
            <Card size="small" style={{ marginBottom: 12 }}>
              {!health.sandboxEnabled ? (
                <Text type="secondary" style={{ fontSize: 12 }}>沙箱功能未启用</Text>
              ) : !health.dockerConnected ? (
                <div>
                  <Badge status="error" text={<Text style={{ fontSize: 12, color: 'var(--td-error-color)' }}>Docker 未连接</Text>} />
                  <div style={{ marginTop: 6 }}>
                    <Text type="secondary" style={{ fontSize: 11 }}>
                      {health.errorMessage || '请先启动 Docker Desktop 或 Docker daemon'}
                    </Text>
                  </div>
                </div>
              ) : (
                <div style={{ display: 'flex', justifyContent: 'space-around', textAlign: 'center' }}>
                  <div>
                    <div style={{ fontSize: 20, fontWeight: 700 }}>{health.totalSandboxes}</div>
                    <Text type="secondary" style={{ fontSize: 11 }}>总计</Text>
                  </div>
                  <div>
                    <div style={{ fontSize: 20, fontWeight: 700, color: 'var(--td-success-color)' }}>{health.runningCount}</div>
                    <Text type="secondary" style={{ fontSize: 11 }}>运行中</Text>
                  </div>
                  <div>
                    <div style={{ fontSize: 20, fontWeight: 700, color: 'var(--td-text-tertiary)' }}>{health.stoppedCount}</div>
                    <Text type="secondary" style={{ fontSize: 11 }}>已停止</Text>
                  </div>
                </div>
              )}
            </Card>
          )}

          {/* 沙箱列表 */}
          <div style={{ flex: 1, overflowY: 'auto', marginBottom: 12 }}>
            {sandboxLoading && sandboxes.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '20px 0' }}><Spin size="small" /></div>
            ) : sandboxes.length === 0 ? (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description="暂无活跃沙箱"
                style={{ marginTop: 40 }}
              />
            ) : (
              <Space direction="vertical" style={{ width: '100%' }} size={8}>
                {sandboxes.map(sandbox => {
                  const typeConf = sandboxTypeConfig[sandbox.sandboxType] || sandboxTypeConfig.unknown;
                  const stConf = statusConfig[sandbox.status] || statusConfig.unknown;
                  const running = isRunningStatus(sandbox.status);
                  return (
                    <Card
                      key={sandbox.containerId}
                      size="small"
                      hoverable
                      onClick={() => openSandboxDetail(sandbox)}
                      style={{ borderLeft: `3px solid var(--td-${stConf.color === 'success' ? 'success' : stConf.color === 'processing' ? 'primary' : 'text-quaternary'}-color)` }}
                    >
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
                        <Space size={4}>
                          <span style={{ color: `var(--td-${typeConf.color === 'purple' ? 'primary' : typeConf.color}-color)` }}>{typeConf.icon}</span>
                          <Text strong style={{ fontSize: 13 }}>{typeConf.label}</Text>
                        </Space>
                        <Badge status={stConf.color as any} text={<Text style={{ fontSize: 11 }}>{stConf.text}</Text>} />
                      </div>

                      {/* 端口 */}
                      {sandbox.ports && sandbox.ports.length > 0 && (
                        <div style={{ marginBottom: 6 }}>
                          <Text type="secondary" style={{ fontSize: 11 }}>端口: </Text>
                          {sandbox.ports.map((p, i) => (
                            <Tag key={i} style={{ fontSize: 11, margin: '0 2px' }}>{p}</Tag>
                          ))}
                        </div>
                      )}

                      {/* 提供的工具数 */}
                      <div style={{ marginBottom: 8 }}>
                        <Text type="secondary" style={{ fontSize: 11 }}>
                          提供 {sandbox.providedTools?.length || 0} 个工具
                        </Text>
                      </div>

                      {/* 操作按钮 */}
                      <div style={{ display: 'flex', gap: 8 }}>
                        <Button
                          size="small"
                          icon={<StopOutlined />}
                          disabled={!running}
                          onClick={e => { e.stopPropagation(); handleStopSandbox(sandbox.containerId); }}
                        >
                          停止
                        </Button>
                        <Popconfirm
                          title="确定删除此沙箱？"
                          description="删除后容器将被销毁，不可恢复。"
                          onConfirm={e => { e?.stopPropagation(); handleRemoveSandbox(sandbox.containerId); }}
                          onCancel={e => e?.stopPropagation()}
                        >
                          <Button
                            size="small"
                            danger
                            icon={<DeleteOutlined />}
                            onClick={e => e.stopPropagation()}
                          >
                            删除
                          </Button>
                        </Popconfirm>
                      </div>
                    </Card>
                  );
                })}
              </Space>
            )}
          </div>

          {/* 底部操作 */}
          <div style={{ borderTop: '1px solid var(--td-border-light)', paddingTop: 12 }}>
            <Popconfirm
              title="确定清理所有沙箱？"
              description="将停止并删除所有沙箱容器。"
              onConfirm={handleCleanupAll}
            >
              <Button
                block
                icon={<ClearOutlined />}
                disabled={!health?.sandboxEnabled || health?.totalSandboxes === 0}
              >
                清理所有沙箱
              </Button>
            </Popconfirm>
          </div>
        </div>
      </div>

      {/* 沙箱详情抽屉 */}
      <Drawer
        title="沙箱详情"
        placement="right"
        width={480}
        open={drawerVisible}
        onClose={() => setDrawerVisible(false)}
      >
        {selectedSandbox && (() => {
          const typeConf = sandboxTypeConfig[selectedSandbox.sandboxType] || sandboxTypeConfig.unknown;
          const stConf = statusConfig[selectedSandbox.status] || statusConfig.unknown;
          const running = isRunningStatus(selectedSandbox.status);
          return (
            <Space direction="vertical" style={{ width: '100%' }} size={16}>
              {/* 状态概览 */}
              <Card size="small">
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <Space>
                    <span style={{ fontSize: 24 }}>{typeConf.icon}</span>
                    <div>
                      <Text strong style={{ fontSize: 16 }}>{typeConf.label}</Text>
                      <br />
                      <Badge status={stConf.color as any} text={stConf.text} />
                    </div>
                  </Space>
                  <Space>
                    <Button
                      icon={<StopOutlined />}
                      disabled={!running}
                      onClick={() => handleStopSandbox(selectedSandbox.containerId)}
                    >
                      停止
                    </Button>
                    <Popconfirm
                      title="确定删除此沙箱？"
                      onConfirm={() => handleRemoveSandbox(selectedSandbox.containerId)}
                    >
                      <Button danger icon={<DeleteOutlined />}>删除</Button>
                    </Popconfirm>
                  </Space>
                </div>
              </Card>

              {/* 基本信息 */}
              <Card size="small" title="容器信息">
                <table style={{ width: '100%', fontSize: 13 }}>
                  <tbody>
                    <tr>
                      <td style={{ color: 'var(--td-text-secondary)', padding: '4px 0', width: 100 }}>容器 ID</td>
                      <td><Text code copyable style={{ fontSize: 12 }}>{selectedSandbox.containerId}</Text></td>
                    </tr>
                    <tr>
                      <td style={{ color: 'var(--td-text-secondary)', padding: '4px 0' }}>容器名称</td>
                      <td><Text code style={{ fontSize: 12 }}>{selectedSandbox.containerName}</Text></td>
                    </tr>
                    <tr>
                      <td style={{ color: 'var(--td-text-secondary)', padding: '4px 0' }}>Docker 镜像</td>
                      <td><Text code copyable style={{ fontSize: 11 }} ellipsis>{selectedSandbox.version}</Text></td>
                    </tr>
                    <tr>
                      <td style={{ color: 'var(--td-text-secondary)', padding: '4px 0' }}>引用计数</td>
                      <td><Tag>{selectedSandbox.refCount}</Tag></td>
                    </tr>
                  </tbody>
                </table>
              </Card>

              {/* 网络端点 */}
              <Card size="small" title="网络端点">
                <table style={{ width: '100%', fontSize: 13 }}>
                  <tbody>
                    {selectedSandbox.ports && selectedSandbox.ports.length > 0 && (
                      <tr>
                        <td style={{ color: 'var(--td-text-secondary)', padding: '4px 0', width: 100 }}>端口映射</td>
                        <td>
                          <Space wrap>
                            {selectedSandbox.ports.map((p, i) => <Tag key={i}>{p}</Tag>)}
                          </Space>
                        </td>
                      </tr>
                    )}
                    {selectedSandbox.baseUrl && (
                      <tr>
                        <td style={{ color: 'var(--td-text-secondary)', padding: '4px 0' }}>HTTP 端点</td>
                        <td>
                          <a href={selectedSandbox.baseUrl} target="_blank" rel="noopener noreferrer" style={{ fontSize: 12 }}>
                            {selectedSandbox.baseUrl}
                          </a>
                        </td>
                      </tr>
                    )}
                    {selectedSandbox.browserUrl && (
                      <tr>
                        <td style={{ color: 'var(--td-text-secondary)', padding: '4px 0' }}>浏览器 URL</td>
                        <td>
                          <a href={selectedSandbox.browserUrl} target="_blank" rel="noopener noreferrer" style={{ fontSize: 12 }}>
                            {selectedSandbox.browserUrl}
                          </a>
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </Card>

              {/* 挂载目录 */}
              {selectedSandbox.mountDir && (
                <Card size="small" title="文件系统">
                  <table style={{ width: '100%', fontSize: 13 }}>
                    <tbody>
                      <tr>
                        <td style={{ color: 'var(--td-text-secondary)', padding: '4px 0', width: 100 }}>挂载目录</td>
                        <td><Text code copyable style={{ fontSize: 11 }}>{selectedSandbox.mountDir}</Text></td>
                      </tr>
                    </tbody>
                  </table>
                </Card>
              )}

              {/* 提供的工具 */}
              {selectedSandbox.providedTools && selectedSandbox.providedTools.length > 0 && (
                <Card size="small" title={`提供的工具 (${selectedSandbox.providedTools.length})`}>
                  <Space wrap>
                    {selectedSandbox.providedTools.map(t => (
                      <Tag key={t} color="blue">{t}</Tag>
                    ))}
                  </Space>
                </Card>
              )}
            </Space>
          );
        })()}
      </Drawer>
    </div>
  );
};

export default ToolLibrary;
