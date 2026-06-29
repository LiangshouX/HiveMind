import React, { useState } from 'react';
import { Card, Badge, Button, Row, Col, Typography, Tag, Space, Modal, Form, Input, Switch, message } from 'antd';

const { Title, Text, Paragraph } = Typography;

interface Channel {
  id: string;
  name: string;
  type: 'console' | 'dingtalk' | 'feishu' | 'imessage' | 'discord' | 'telegram' | 'qq' | 'matrix' | 'mattermost' | 'mqtt' | 'twilio';
  enabled: boolean;
  isInternal: boolean;
  desc?: string;
}

const CHANNELS: Channel[] = [
  { id: '1', name: 'Console', type: 'console', enabled: true, isInternal: true, desc: '机器人前缀: 未设置' },
  { id: '2', name: 'DingTalk', type: 'dingtalk', enabled: true, isInternal: true, desc: '机器人前缀: 未设置' },
  { id: '3', name: 'Feishu', type: 'feishu', enabled: false, isInternal: true, desc: '机器人前缀: 未设置' },
  { id: '4', name: 'iMessage', type: 'imessage', enabled: false, isInternal: true, desc: '机器人前缀: 未设置' },
  { id: '5', name: 'Discord', type: 'discord', enabled: true, isInternal: true, desc: '机器人前缀: 未设置' },
  { id: '6', name: 'Telegram', type: 'telegram', enabled: true, isInternal: true, desc: '机器人前缀: 未设置' },
  { id: '7', name: 'QQ', type: 'qq', enabled: false, isInternal: true, desc: '机器人前缀: @QH' },
  { id: '8', name: 'Matrix', type: 'matrix', enabled: false, isInternal: true, desc: '机器人前缀: 未设置' },
  { id: '9', name: 'Mattermost', type: 'mattermost', enabled: false, isInternal: true, desc: '机器人前缀: 未设置' },
  { id: '10', name: 'MQTT', type: 'mqtt', enabled: false, isInternal: true, desc: '机器人前缀: 未设置' },
  { id: '11', name: 'Twilio', type: 'twilio', enabled: false, isInternal: true, desc: '电话号码: 未设置' },
];

const ChannelCard: React.FC<{ channel: Channel, onClick: (channel: Channel) => void }> = ({ channel, onClick }) => {
  return (
    <Card 
      hoverable
      onClick={() => onClick(channel)}
      style={{ 
        height: '100%', 
        borderColor: channel.enabled ? '#1677ff' : undefined,
        backgroundColor: channel.enabled ? '#f0f5ff' : undefined 
      }}
      bodyStyle={{ padding: '20px' }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 16 }}>
        <Space>
          <Title level={5} style={{ margin: 0 }}>{channel.name}</Title>
          {channel.isInternal && <Tag color="purple" style={{ marginRight: 0 }}>内置</Tag>}
        </Space>
        <Badge status={channel.enabled ? "success" : "default"} text={channel.enabled ? "已启用" : "已禁用"} />
      </div>
      
      <Paragraph type="secondary" style={{ marginBottom: 24, fontSize: 13 }}>
        {channel.desc || '点击卡片进行编辑'}
      </Paragraph>
      
      <Text type="secondary" style={{ fontSize: 12 }}>点击卡片进行编辑</Text>
    </Card>
  );
};

const Channels: React.FC = () => {
  const [channels, setChannels] = useState<Channel[]>(CHANNELS);
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [editingChannel, setEditingChannel] = useState<Channel | null>(null);
  const [form] = Form.useForm();
  const [filter, setFilter] = useState<'all' | 'internal' | 'custom'>('all');

  const handleCardClick = (channel: Channel) => {
    setEditingChannel(channel);
    form.setFieldsValue({
      ...channel,
    });
    setIsModalVisible(true);
  };

  const handleModalOk = () => {
    form.validateFields().then(values => {
      setChannels(prev => prev.map(c => 
        c.id === editingChannel?.id ? { ...c, ...values, desc: `机器人前缀: ${values.prefix || '未设置'}` } : c
      ));
      setIsModalVisible(false);
      message.success(`${editingChannel?.name} 配置已更新`);
    });
  };

  const filteredChannels = channels.filter(c => {
    if (filter === 'internal') return c.isInternal;
    if (filter === 'custom') return !c.isInternal;
    return true;
  });

  return (
    <div style={{ padding: 24 }}>
      <div style={{ marginBottom: 24, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <Title level={2} style={{ margin: 0 }}>渠道管理</Title>
          <Text type="secondary">管理和配置消息频道</Text>
        </div>
        <Space>
           <Button type={filter === 'all' ? 'primary' : 'default'} onClick={() => setFilter('all')}>全部</Button>
           <Button type={filter === 'internal' ? 'primary' : 'text'} onClick={() => setFilter('internal')}>内置</Button>
           <Button type={filter === 'custom' ? 'primary' : 'text'} onClick={() => setFilter('custom')}>自定义</Button>
        </Space>
      </div>
      
      <Row gutter={[16, 16]}>
        {filteredChannels.map(channel => (
          <Col xs={24} sm={12} md={8} lg={6} xl={6} key={channel.id}>
            <ChannelCard channel={channel} onClick={handleCardClick} />
          </Col>
        ))}
      </Row>

      <Modal
        title={`配置 ${editingChannel?.name} 频道`}
        open={isModalVisible}
        onOk={handleModalOk}
        onCancel={() => setIsModalVisible(false)}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="enabled" label="启用频道" valuePropName="checked">
            <Switch />
          </Form.Item>
          <Form.Item name="prefix" label="机器人前缀" tooltip="用于触发机器人的消息前缀">
            <Input placeholder="例如: @bot" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default Channels;
