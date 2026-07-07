import React, { useState, useEffect } from 'react';
import { Typography, Card, Row, Col, Switch, Space, message, Badge, Button, Select, Tag, Collapse, Input } from 'antd';
import { SyncOutlined, SearchOutlined } from '@ant-design/icons';
import { api } from '../../api';

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

const riskLevelColors = {
  LOW: 'green',
  MEDIUM: 'orange',
  HIGH: 'red',
  CRITICAL: 'magenta',
};

const riskLevelLabels = {
  LOW: '低风险',
  MEDIUM: '中风险',
  HIGH: '高风险',
  CRITICAL: '严重风险',
};

const categoryLabels = {
  builtin: '内置工具',
  sandbox: '沙盒工具',
  browser: '浏览器工具',
  custom: '自定义工具',
};

const ToolLibrary: React.FC = () => {
  const [tools, setTools] = useState<ToolConfig[]>([]);
  const [loading, setLoading] = useState(false);
  const [syncing, setSyncing] = useState(false);
  const [filter, setFilter] = useState<string>('all');
  const [search, setSearch] = useState('');

  // 加载工具列表
  const loadTools = async () => {
    setLoading(true);
    try {
      const data = await api.listToolConfigs();
      setTools(data);
    } catch (error) {
      message.error('加载工具配置失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadTools();
  }, []);

  // 同步系统工具
  const handleSync = async () => {
    setSyncing(true);
    try {
      const result = await api.syncSystemTools();
      message.success(result.message);
      await loadTools();
    } catch (error) {
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
    } catch (error) {
      message.error('更新失败');
    }
  };

  // 修改风险等级
  const handleRiskChange = async (toolName: string, newLevel: string) => {
    try {
      await api.updateToolConfig(toolName, { riskLevel: newLevel as any });
      message.success('风险等级已更新');
      await loadTools();
    } catch (error) {
      message.error('更新失败');
    }
  };

  // 切换审批要求
  const toggleApproval = async (toolName: string, checked: boolean) => {
    try {
      await api.updateToolConfig(toolName, { approvalRequired: checked });
      message.success(`审批要求已${checked ? '开启' : '关闭'}`);
      await loadTools();
    } catch (error) {
      message.error('更新失败');
    }
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

      {/* 工具列表 */}
      <div style={{ flex: 1, overflowY: 'auto' }}>
        {loading ? (
          <div style={{ textAlign: 'center', padding: '40px 0' }}>
            <Text>加载中...</Text>
          </div>
        ) : filteredTools.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '40px 0' }}>
            <Text type="secondary">暂无工具</Text>
          </div>
        ) : (
          <Row gutter={[16, 16]}>
            {filteredTools.map(tool => (
              <Col xs={24} sm={12} md={8} lg={6} key={tool.toolName}>
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

                  {/* 风险等级 */}
                  <div style={{ marginBottom: 8 }}>
                    <Space wrap>
                      <Tag color={riskLevelColors[tool.riskLevel]}>
                        {riskLevelLabels[tool.riskLevel]}
                      </Tag>
                      <Tag color="blue">
                        {categoryLabels[tool.category]}
                      </Tag>
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
    </div>
  );
};

export default ToolLibrary;
