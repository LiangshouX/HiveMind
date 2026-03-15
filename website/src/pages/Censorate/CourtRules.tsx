import React, { useState } from 'react';
import { Typography, Tabs, Input, Button, message } from 'antd';
import { SaveOutlined } from '@ant-design/icons';

const { Title, Text } = Typography;

const initialFiles = {
  'SOUL.md': '# 三省六部 AI 助手灵魂设定\n\n你是一个基于三省六部制度的 AI 协作系统。',
  'AGENTS.md': '# 智能体定义\n\n- 中书省：负责规划和起草\n- 门下省：负责审核\n- 尚书省：负责执行和分发',
  'HEARTBEAT.md': '# 心跳机制\n\n每 5 分钟检查一次系统状态，并生成报告。',
};

const CourtRules: React.FC = () => {
  const [activeTab, setActiveTab] = useState('SOUL.md');
  const [files, setFiles] = useState(initialFiles);
  const [loading, setLoading] = useState(false);

  const handleSave = () => {
    setLoading(true);
    // Simulate API call
    setTimeout(() => {
      setLoading(false);
      message.success(`${activeTab} 已保存`);
    }, 500);
  };

  const handleChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setFiles({ ...files, [activeTab]: e.target.value });
  };

  return (
    <div style={{ padding: 24, background: '#fff', minHeight: '100%', borderRadius: 8 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <div>
          <Title level={4} style={{ margin: 0 }}>朝纲 (Court Rules)</Title>
          <Text type="secondary">编辑定义 AI 助手人设和行为的文件。</Text>
        </div>
        <Button type="primary" icon={<SaveOutlined />} onClick={handleSave} loading={loading}>
          保存当前文件
        </Button>
      </div>

      <Tabs 
        activeKey={activeTab} 
        onChange={setActiveTab}
        items={Object.keys(files).map(key => ({
          key,
          label: key,
          children: (
            <Input.TextArea
              value={files[key as keyof typeof files]}
              onChange={handleChange}
              autoSize={{ minRows: 20, maxRows: 30 }}
              style={{ fontFamily: 'monospace', fontSize: 14 }}
            />
          )
        }))}
      />
    </div>
  );
};

export default CourtRules;
