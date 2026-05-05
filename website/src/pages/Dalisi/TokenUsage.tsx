import React, { useState, useEffect } from 'react';
import { Typography, Row, Col, Card, Table, Statistic, DatePicker, Spin, Empty } from 'antd';
import { FireOutlined, DollarOutlined, CodeSandboxOutlined, MessageOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import { api, type TokenUsageByModel, type TokenUsageSummary } from '../../api';

const { Title, Text } = Typography;
const { RangePicker } = DatePicker;

const TokenUsage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [summary, setSummary] = useState<TokenUsageSummary | null>(null);
  const [byModelData, setByModelData] = useState<TokenUsageByModel[]>([]);
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs]>([
    dayjs().subtract(7, 'day'),
    dayjs()
  ]);

  // 加载数据
  const loadData = async (start: string, end: string) => {
    setLoading(true);
    try {
      const [summaryRes, modelRes] = await Promise.all([
        api.tokenUsageSummary(start, end),
        api.tokenUsageByModel(start, end)
      ]);
      setSummary(summaryRes as any);
      setByModelData(modelRes as any);
    } catch (error) {
      console.error('加载 Token Usage 数据失败:', error);
    } finally {
      setLoading(false);
    }
  };

  // 监听日期变化
  useEffect(() => {
    if (dateRange && dateRange[0] && dateRange[1]) {
      loadData(
        dateRange[0].format('YYYY-MM-DD'),
        dateRange[1].format('YYYY-MM-DD')
      );
    }
  }, [dateRange]);

  // 表格列定义
  const columns: ColumnsType<TokenUsageByModel> = [
    { 
      title: '模型名称', 
      dataIndex: 'modelName', 
      key: 'modelName',
      render: text => <Text strong>{text}</Text> 
    },
    { 
      title: '供应商', 
      dataIndex: 'modelProvider', 
      key: 'modelProvider',
      render: text => <Text type="secondary">{text}</Text> 
    },
    { 
      title: '输入 Tokens', 
      dataIndex: 'inputTokens', 
      key: 'inputTokens', 
      render: val => val?.toLocaleString() || '0' 
    },
    { 
      title: '输出 Tokens', 
      dataIndex: 'outputTokens', 
      key: 'outputTokens', 
      render: val => val?.toLocaleString() || '0' 
    },
    { 
      title: '总计 Tokens', 
      dataIndex: 'totalTokens', 
      key: 'totalTokens', 
      render: val => <Text type="warning">{val?.toLocaleString() || '0'}</Text> 
    },
    { 
      title: '调用次数', 
      dataIndex: 'callCount', 
      key: 'callCount',
      render: val => val?.toLocaleString() || '0' 
    },
    { 
      title: '估算费用 (¥)', 
      dataIndex: 'estimatedCost', 
      key: 'estimatedCost', 
      render: val => <Text type="success">¥{(val || 0).toFixed(2)}</Text> 
    },
  ];

  return (
    <div style={{ padding: 24, background: '#fff', minHeight: '100%', borderRadius: 8 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <div>
          <Title level={4} style={{ margin: 0 }}>司农寺 (Token Usage)</Title>
          <Text type="secondary">查看一段时间内的 LLM Token 消耗，按日期和模型统计。</Text>
        </div>
        <RangePicker 
          value={dateRange}
          onChange={(dates) => {
            if (dates && dates[0] && dates[1]) {
              setDateRange([dates[0], dates[1]]);
            }
          }}
        />
      </div>

      <Spin spinning={loading}>
        {summary ? (
          <>
            <Row gutter={16} style={{ marginBottom: 24 }}>
              <Col span={6}>
                <Card bordered={false} style={{ background: '#f6ffed' }}>
                  <Statistic
                    title="总估算费用"
                    value={summary.estimatedCost || 0}
                    precision={2}
                    prefix={<DollarOutlined />}
                    suffix="¥"
                  />
                </Card>
              </Col>
              <Col span={6}>
                <Card bordered={false} style={{ background: '#fffbe6' }}>
                  <Statistic
                    title="总 Token 消耗"
                    value={summary.totalTokens || 0}
                    prefix={<FireOutlined />}
                    suffix="Tokens"
                  />
                </Card>
              </Col>
              <Col span={6}>
                <Card bordered={false} style={{ background: '#e6f7ff' }}>
                  <Statistic
                    title="调用次数"
                    value={summary.totalCalls || 0}
                    prefix={<MessageOutlined />}
                    suffix="次"
                  />
                </Card>
              </Col>
              <Col span={6}>
                <Card bordered={false} style={{ background: '#f9f0ff' }}>
                  <Statistic
                    title="活跃模型数"
                    value={byModelData.length}
                    prefix={<CodeSandboxOutlined />}
                    suffix="个"
                  />
                </Card>
              </Col>
            </Row>

            <Card title="按模型统计消耗" bordered={false} styles={{ body: { padding: 0 } }}>
              {byModelData.length > 0 ? (
                <Table 
                  columns={columns} 
                  dataSource={byModelData.map((item, index) => ({ ...item, key: index.toString() }))} 
                  pagination={false} 
                />
              ) : (
                <Empty description="暂无数据" style={{ padding: '40px 0' }} />
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
