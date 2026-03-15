import React from 'react';
import { Typography, Row, Col, Card, Table, Statistic, DatePicker } from 'antd';
import { FireOutlined, DollarOutlined, CodeSandboxOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';

const { Title, Text } = Typography;
const { RangePicker } = DatePicker;

interface TokenData {
  key: string;
  model: string;
  inputTokens: number;
  outputTokens: number;
  totalTokens: number;
  cost: number;
}

const mockData: TokenData[] = [
  { key: '1', model: 'gpt-4o', inputTokens: 125000, outputTokens: 45000, totalTokens: 170000, cost: 2.15 },
  { key: '2', model: 'claude-3-5-sonnet', inputTokens: 85000, outputTokens: 32000, totalTokens: 117000, cost: 1.48 },
  { key: '3', model: 'deepseek-coder', inputTokens: 250000, outputTokens: 110000, totalTokens: 360000, cost: 0.95 },
  { key: '4', model: 'qwen-max', inputTokens: 520000, outputTokens: 180000, totalTokens: 700000, cost: 0.50 },
];

const columns: ColumnsType<TokenData> = [
  { title: '模型名称', dataIndex: 'model', key: 'model', render: text => <Text strong>{text}</Text> },
  { title: '输入 Tokens', dataIndex: 'inputTokens', key: 'inputTokens', render: val => val.toLocaleString() },
  { title: '输出 Tokens', dataIndex: 'outputTokens', key: 'outputTokens', render: val => val.toLocaleString() },
  { title: '总计 Tokens', dataIndex: 'totalTokens', key: 'totalTokens', render: val => <Text type="warning">{val.toLocaleString()}</Text> },
  { title: '估算费用 ($)', dataIndex: 'cost', key: 'cost', render: val => <Text type="success">${val.toFixed(2)}</Text> },
];

const TokenUsage: React.FC = () => {
  const totalCost = mockData.reduce((acc, curr) => acc + curr.cost, 0);
  const totalTokens = mockData.reduce((acc, curr) => acc + curr.totalTokens, 0);

  return (
    <div style={{ padding: 24, background: '#fff', minHeight: '100%', borderRadius: 8 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <div>
          <Title level={4} style={{ margin: 0 }}>大司农 (Token Usage)</Title>
          <Text type="secondary">查看一段时间内的 LLM Token 消耗，按日期和模型统计。</Text>
        </div>
        <RangePicker defaultValue={[dayjs().subtract(7, 'day'), dayjs()]} />
      </div>

      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={8}>
          <Card bordered={false} style={{ background: '#f6ffed' }}>
            <Statistic
              title="总估算费用"
              value={totalCost}
              precision={2}
              prefix={<DollarOutlined />}
              suffix="USD"
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card bordered={false} style={{ background: '#fffbe6' }}>
            <Statistic
              title="总 Token 消耗"
              value={totalTokens}
              prefix={<FireOutlined />}
              suffix="Tokens"
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card bordered={false} style={{ background: '#e6f7ff' }}>
            <Statistic
              title="活跃模型数"
              value={mockData.length}
              prefix={<CodeSandboxOutlined />}
            />
          </Card>
        </Col>
      </Row>

      <Card title="按模型统计消耗" bordered={false} styles={{ body: { padding: 0 } }}>
        <Table columns={columns} dataSource={mockData} pagination={false} />
      </Card>
    </div>
  );
};

export default TokenUsage;
