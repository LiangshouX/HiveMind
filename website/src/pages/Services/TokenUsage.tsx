import React, { useState, useEffect, useCallback } from 'react';
import {
  Typography, Row, Col, Card, Table, Statistic, DatePicker, Spin, Empty,
  Segmented, Progress, Tag, Space,
} from 'antd';
import {
  FireOutlined, DollarOutlined, CodeSandboxOutlined, MessageOutlined,
  LineChartOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs, { Dayjs } from 'dayjs';
import { api, type TokenUsageByModel, type TokenUsageSummary } from '../../api';

const { Title, Text } = Typography;
const { RangePicker } = DatePicker;

/** 默认查询窗口：最近 30 天（覆盖演示环境的种子数据区间）。 */
const DEFAULT_RANGE_DAYS = 30;

const TokenUsage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [summary, setSummary] = useState<TokenUsageSummary | null>(null);
  const [byModelData, setByModelData] = useState<TokenUsageByModel[]>([]);
  const [byDateData, setByDateData] = useState<Array<{ usageDate?: string; totalTokens?: number; callCount?: number }>>([]);
  const [view, setView] = useState<'model' | 'trend'>('model');
  const [dateRange, setDateRange] = useState<[Dayjs, Dayjs]>([
    dayjs().subtract(DEFAULT_RANGE_DAYS - 1, 'day'),
    dayjs(),
  ]);

  const loadData = useCallback(async (start: string, end: string) => {
    setLoading(true);
    try {
      const [summaryRes, modelRes, dateRes] = await Promise.all([
        api.tokenUsageSummary(start, end),
        api.tokenUsageByModel(start, end),
        api.tokenUsageByDate(start, end),
      ]);
      setSummary(summaryRes as TokenUsageSummary);
      setByModelData((modelRes as TokenUsageByModel[]) ?? []);
      setByDateData((dateRes as any[]) ?? []);
    } catch (error) {
      console.error('加载 Token Usage 数据失败:', error);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (dateRange?.[0] && dateRange?.[1]) {
      loadData(
        dateRange[0].format('YYYY-MM-DD'),
        dateRange[1].format('YYYY-MM-DD'),
      );
    }
  }, [dateRange, loadData]);

  const columns: ColumnsType<TokenUsageByModel> = [
    {
      title: '模型名称',
      dataIndex: 'modelName',
      key: 'modelName',
      width: 170,
      render: (text) => (
        <Text strong ellipsis={{ tooltip: text }} style={{ whiteSpace: 'nowrap' }}>
          {text ?? '-'}
        </Text>
      ),
    },
    {
      title: '供应商',
      dataIndex: 'modelProvider',
      key: 'modelProvider',
      width: 120,
      render: (text) => (
        <Text type="secondary" style={{ whiteSpace: 'nowrap' }}>
          {text ?? '-'}
        </Text>
      ),
    },
    {
      title: '输入 Tokens',
      dataIndex: 'inputTokens',
      key: 'inputTokens',
      render: (val) => val?.toLocaleString() || '0',
    },
    {
      title: '输出 Tokens',
      dataIndex: 'outputTokens',
      key: 'outputTokens',
      render: (val) => val?.toLocaleString() || '0',
    },
    {
      title: '总计 Tokens',
      dataIndex: 'totalTokens',
      key: 'totalTokens',
      render: (val) => <Text type="warning">{val?.toLocaleString() || '0'}</Text>,
    },
    {
      title: '调用次数',
      dataIndex: 'callCount',
      key: 'callCount',
      render: (val) => val?.toLocaleString() || '0',
    },
    {
      title: '估算费用 (¥)',
      dataIndex: 'estimatedCost',
      key: 'estimatedCost',
      render: (val) => (
        <Text strong type="success">
          ¥{Number(val ?? 0).toFixed(2)}
        </Text>
      ),
    },
  ];

  const trendColumns: ColumnsType<any> = [
    {
      title: '日期',
      dataIndex: 'usageDate',
      key: 'usageDate',
      render: (text) => (text ? dayjs(text).format('YYYY-MM-DD') : '-'),
    },
    {
      title: '调用次数',
      dataIndex: 'callCount',
      key: 'callCount',
      render: (val) => val?.toLocaleString() || '0',
    },
    {
      title: 'Tokens 消耗',
      dataIndex: 'totalTokens',
      key: 'totalTokens',
      render: (val) => <Text type="warning">{val?.toLocaleString() || '0'}</Text>,
    },
  ];

  const maxDailyTokens = byDateData.reduce((max, item) => Math.max(max, item.totalTokens ?? 0), 0);

  return (
    <div style={{ padding: 24, background: 'var(--td-bg-container)', minHeight: '100%', borderRadius: 8 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24, flexWrap: 'wrap', gap: 12 }}>
        <div style={{ minWidth: 0 }}>
          <Title level={4} style={{ margin: 0 }}>用量统计</Title>
          <Text type="secondary">查看一段时间内的 LLM Token 消耗，按日期和模型统计。</Text>
        </div>
        <RangePicker
          value={dateRange}
          allowClear={false}
          onChange={(dates) => {
            if (dates?.[0] && dates?.[1]) {
              setDateRange([dates[0], dates[1]]);
            }
          }}
        />
      </div>

      <Spin spinning={loading}>
        {summary ? (
          <>
            <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
              <Col xs={24} sm={12} lg={6}>
                <Card bordered={false} style={{ background: 'var(--td-stat-green-bg)' }}>
                  <Statistic
                    title="总估算费用"
                    value={summary.estimatedCost || 0}
                    precision={2}
                    prefix={<DollarOutlined />}
                    suffix="¥"
                  />
                </Card>
              </Col>
              <Col xs={24} sm={12} lg={6}>
                <Card bordered={false} style={{ background: 'var(--td-stat-yellow-bg)' }}>
                  <Statistic
                    title="总 Token 消耗"
                    value={summary.totalTokens || 0}
                    prefix={<FireOutlined />}
                    suffix="Tokens"
                  />
                </Card>
              </Col>
              <Col xs={24} sm={12} lg={6}>
                <Card bordered={false} style={{ background: 'var(--td-stat-blue-bg)' }}>
                  <Statistic
                    title="调用次数"
                    value={summary.totalCalls || 0}
                    prefix={<MessageOutlined />}
                    suffix="次"
                  />
                </Card>
              </Col>
              <Col xs={24} sm={12} lg={6}>
                <Card bordered={false} style={{ background: 'var(--td-stat-purple-bg)' }}>
                  <Statistic
                    title="活跃模型数"
                    value={byModelData.length}
                    prefix={<CodeSandboxOutlined />}
                    suffix="个"
                  />
                </Card>
              </Col>
            </Row>

            <Card
              title={
                <Space>
                  <Segmented
                    size="small"
                    value={view}
                    onChange={(v) => setView(v as 'model' | 'trend')}
                    options={[
                      { label: '按模型', value: 'model', icon: <CodeSandboxOutlined /> },
                      { label: '按日期', value: 'trend', icon: <LineChartOutlined /> },
                    ]}
                  />
                </Space>
              }
              bordered={false}
              styles={{ body: { padding: 0 } }}
            >
              {view === 'model' ? (
                byModelData.length > 0 ? (
                  <Table
                    columns={columns}
                    dataSource={byModelData.map((item, index) => ({ ...item, key: index.toString() }))}
                    pagination={false}
                    scroll={{ x: 860 }}
                  />
                ) : (
                  <Empty description="该时间范围内暂无 Token 用量数据" style={{ padding: '40px 0' }} />
                )
              ) : (
                byDateData.length > 0 ? (
                  <div style={{ padding: 24 }}>
                    <div style={{ marginBottom: 20 }}>
                      {byDateData.map((item) => (
                        <div
                          key={item.usageDate ?? Math.random()}
                          style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 10 }}
                        >
                          <Text style={{ width: 96, flexShrink: 0 }} type="secondary">
                            {item.usageDate ? dayjs(item.usageDate).format('MM-DD') : '-'}
                          </Text>
                          <Progress
                            percent={maxDailyTokens ? Math.round(((item.totalTokens ?? 0) / maxDailyTokens) * 100) : 0}
                            strokeColor="var(--td-highlight)"
                            style={{ flex: 1, margin: 0 }}
                            format={() => `${(item.totalTokens ?? 0).toLocaleString()} tok`}
                          />
                          <Tag style={{ flexShrink: 0 }}>{item.callCount ?? 0} 次调用</Tag>
                        </div>
                      ))}
                    </div>
                    <Table
                      columns={trendColumns}
                      dataSource={byDateData.map((item, index) => ({ ...item, key: index.toString() }))}
                      pagination={false}
                      size="small"
                    />
                  </div>
                ) : (
                  <Empty description="该时间范围内暂无 Token 用量数据" style={{ padding: '40px 0' }} />
                )
              )}
            </Card>
          </>
        ) : (
          !loading && <Empty description="暂无数据" />
        )}
      </Spin>
    </div>
  );
};

export default TokenUsage;
