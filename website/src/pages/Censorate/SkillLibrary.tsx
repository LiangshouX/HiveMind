import React, { useState } from 'react';
import { Typography, Card, Button, Row, Col, Tag, Switch, Drawer, Form, Input, Space, message, Badge } from 'antd';
import { PlusOutlined, RobotOutlined } from '@ant-design/icons';

const { Title, Text, Paragraph } = Typography;
const { TextArea } = Input;

interface Skill {
  id: string;
  name: string;
  description: string;
  source: 'builtin' | 'customized';
  path: string;
  content: string;
  enabled: boolean;
}

const initialSkills: Skill[] = [
  { id: '1', name: 'AI 早报', description: '从指定新闻网站为用户搜索最新资讯', source: 'customized', path: 'C:\\Users\\...\\customized_skills\\AI早报', content: '# AI早报\n...', enabled: true },
  { id: '2', name: 'browser_visible', description: '当用户希望打开真实可见的浏览器窗口时使用', source: 'builtin', path: 'D:\\ProgramFiles\\...\\site-packages\\...', content: '# browser_visible\n...', enabled: true },
  { id: '3', name: 'cron', description: '通过 copaw 命令管理定时任务', source: 'customized', path: 'C:\\Users\\...\\customized_skills\\cron', content: '# cron\n...', enabled: true },
  { id: '4', name: 'dingtalk_channel', description: '使用可视化界面自动完成钉钉频道接入', source: 'builtin', path: 'D:\\ProgramFiles\\...\\site-packages\\...', content: '# dingtalk_channel\n...', enabled: true },
];

const SkillLibrary: React.FC = () => {
  const [skills, setSkills] = useState<Skill[]>(initialSkills);
  const [drawerVisible, setDrawerVisible] = useState(false);
  const [currentSkill, setCurrentSkill] = useState<Skill | null>(null);
  const [form] = Form.useForm();
  const [preview, setPreview] = useState(true);

  const handleEdit = (skill: Skill) => {
    setCurrentSkill(skill);
    form.setFieldsValue(skill);
    setDrawerVisible(true);
  };

  const handleSave = () => {
    form.validateFields().then(values => {
      if (currentSkill) {
        setSkills(skills.map(s => s.id === currentSkill.id ? { ...s, ...values } : s));
        message.success('技能已更新');
        setDrawerVisible(false);
      }
    });
  };

  const toggleEnable = (id: string, checked: boolean) => {
    setSkills(skills.map(s => s.id === id ? { ...s, enabled: checked } : s));
    message.success(`技能已${checked ? '启用' : '禁用'}`);
  };

  return (
    <div style={{ height: 'calc(100vh - 120px)', display: 'flex', flexDirection: 'column' }}>
      <div style={{ marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0 }}>技能</Title>
        <Text type="secondary">管理智能体技能和能力。</Text>
      </div>

      <div style={{ flex: 1, overflowY: 'auto' }}>
        <Row gutter={[16, 16]}>
          {skills.map(skill => (
            <Col xs={24} sm={12} md={8} lg={6} key={skill.id}>
              <Card 
                hoverable
                style={{ height: '100%', display: 'flex', flexDirection: 'column' }}
                bodyStyle={{ flex: 1, display: 'flex', flexDirection: 'column' }}
                onClick={() => handleEdit(skill)}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 12 }}>
                  <Space>
                    <RobotOutlined style={{ fontSize: 24, color: '#1890ff' }} />
                    <Text strong style={{ fontSize: 16 }}>{skill.name}</Text>
                  </Space>
                  <Space onClick={e => e.stopPropagation()}>
                    <Badge status={skill.enabled ? 'success' : 'default'} text={skill.enabled ? '已启用' : '已禁用'} />
                  </Space>
                </div>
                
                <div style={{ flex: 1, marginBottom: 16 }}>
                  <Paragraph type="secondary" ellipsis={{ rows: 3 }}>
                    {skill.description}
                  </Paragraph>
                </div>

                <div style={{ marginTop: 'auto' }}>
                  <div style={{ marginBottom: 8 }}>
                    <Text type="secondary" style={{ fontSize: 12 }}>来源</Text>
                    <div style={{ marginTop: 4 }}>
                      <Tag>{skill.source}</Tag>
                    </div>
                  </div>
                  <div>
                    <Text type="secondary" style={{ fontSize: 12 }}>路径</Text>
                    <div style={{ marginTop: 4 }}>
                      <Text ellipsis style={{ fontSize: 12, color: '#999', width: '100%' }}>{skill.path}</Text>
                    </div>
                  </div>
                  <div style={{ marginTop: 12, textAlign: 'right' }} onClick={e => e.stopPropagation()}>
                    <Text type="secondary" style={{ fontSize: 12, marginRight: 8 }}>禁用</Text>
                    <Switch size="small" checked={skill.enabled} onChange={checked => toggleEnable(skill.id, checked)} />
                  </div>
                </div>
              </Card>
            </Col>
          ))}
          <Col xs={24} sm={12} md={8} lg={6}>
            <Button type="dashed" style={{ width: '100%', height: '100%', minHeight: 280 }} icon={<PlusOutlined />}>
              添加新技能
            </Button>
          </Col>
        </Row>
      </div>

      <Drawer
        title="查看技能"
        placement="right"
        width={600}
        onClose={() => setDrawerVisible(false)}
        open={drawerVisible}
        extra={
          <Space>
            <Button onClick={() => setDrawerVisible(false)}>取消</Button>
            <Button type="primary" onClick={handleSave}>保存</Button>
          </Space>
        }
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="Name" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
            <Text strong>Content</Text>
            <Space>
              <Text type="secondary">预览</Text>
              <Switch checked={preview} onChange={setPreview} />
            </Space>
          </div>
          
          <Form.Item name="content" style={{ marginBottom: 24 }}>
             {preview ? (
               <div style={{ padding: 12, background: '#f5f5f5', borderRadius: 4, minHeight: 200, whiteSpace: 'pre-wrap', fontFamily: 'monospace' }}>
                 {form.getFieldValue('content')}
               </div>
             ) : (
               <TextArea autoSize={{ minRows: 10, maxRows: 20 }} style={{ fontFamily: 'monospace' }} />
             )}
          </Form.Item>

          <Form.Item name="source" label="Source">
            <Tag>{currentSkill?.source}</Tag>
          </Form.Item>

          <Form.Item name="path" label="Path">
            <Input disabled />
          </Form.Item>
        </Form>
      </Drawer>
    </div>
  );
};

export default SkillLibrary;