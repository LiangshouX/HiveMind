import React, { useState } from 'react';
import { Typography, List, Switch, Button, Input, Card, Row, Col, message, Space } from 'antd';
import { SaveOutlined, ReloadOutlined, FileMarkdownOutlined, EyeOutlined, EditOutlined, CloudUploadOutlined, CloudDownloadOutlined } from '@ant-design/icons';

const { Title, Text } = Typography;
const { TextArea } = Input;

const initialFiles = {
  'SOUL.md': { content: '# 三省六部 AI 助手灵魂设定\n\n你是一个基于三省六部制度的 AI 协作系统。', size: '1.6 KB', time: '6d ago', active: true },
  'AGENTS.md': { content: '# 智能体定义\n\n- 中书省：负责规划和起草\n- 门下省：负责审核\n- 尚书省：负责执行和分发', size: '5.6 KB', time: '6d ago', active: true },
  'PROFILE.md': { content: '# Profile\n\nAssistant Profile Configuration...', size: '357 B', time: '2d ago', active: true },
  'HEARTBEAT.md': { content: '# 心跳机制\n\n每 5 分钟检查一次系统状态，并生成报告。', size: '253 B', time: '6d ago', active: false },
  'MEMORY.md': { content: '# Memory\n\nLong-term memory storage...', size: '34 B', time: '2d ago', active: false },
};

const CourtRules: React.FC = () => {
  const [activeFile, setActiveFile] = useState('SOUL.md');
  const [files, setFiles] = useState(initialFiles);
  const [preview, setPreview] = useState(false);
  const [loading, setLoading] = useState(false);

  const currentFile = files[activeFile as keyof typeof files];

  const handleSave = () => {
    setLoading(true);
    setTimeout(() => {
      setLoading(false);
      message.success(`${activeFile} 已保存`);
    }, 500);
  };

  const handleContentChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setFiles({
      ...files,
      [activeFile]: { ...currentFile, content: e.target.value }
    });
  };

  const toggleFileActive = (key: string, checked: boolean) => {
    setFiles({
      ...files,
      [key]: { ...files[key as keyof typeof files], active: checked }
    });
    message.success(`${key} ${checked ? '已启用' : '已禁用'}`);
  };

  return (
    <div style={{ height: 'calc(100vh - 120px)', display: 'flex', flexDirection: 'column' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0 }}>工作区</Title>
        <Space>
          <Button icon={<CloudUploadOutlined />}>上传</Button>
          <Button icon={<CloudDownloadOutlined />}>下载</Button>
        </Space>
      </div>

      <Row gutter={16} style={{ flex: 1, overflow: 'hidden' }}>
        {/* Left Sidebar: File List */}
        <Col span={6} style={{ height: '100%', overflowY: 'auto' }}>
          <Card title="核心文件" extra={<Button type="text" icon={<ReloadOutlined />}>刷新</Button>} bodyStyle={{ padding: 0 }}>
            <List
              itemLayout="horizontal"
              dataSource={Object.keys(files)}
              renderItem={(key) => {
                const file = files[key as keyof typeof files];
                return (
                  <List.Item 
                    style={{ 
                      padding: '12px 16px', 
                      cursor: 'pointer',
                      background: activeFile === key ? '#e6f7ff' : 'transparent',
                      borderLeft: activeFile === key ? '3px solid #1890ff' : '3px solid transparent'
                    }}
                    onClick={() => setActiveFile(key)}
                    actions={[
                      <Switch 
                        size="small" 
                        checked={file.active} 
                        onChange={(checked) => toggleFileActive(key, checked)} 
                        onClick={(_, e) => e.stopPropagation()}
                      />
                    ]}
                  >
                    <List.Item.Meta
                      avatar={<FileMarkdownOutlined style={{ fontSize: 20, color: file.active ? '#52c41a' : '#d9d9d9' }} />}
                      title={<span style={{ fontWeight: activeFile === key ? 'bold' : 'normal' }}>{key}</span>}
                      description={<span style={{ fontSize: 12 }}>{file.size} · {file.time}</span>}
                    />
                  </List.Item>
                );
              }}
            />
          </Card>
        </Col>

        {/* Right Content: Editor */}
        <Col span={18} style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
          <Card 
            title={activeFile} 
            extra={
              <Space>
                <Button icon={<ReloadOutlined />}>重置</Button>
                <Button type="primary" icon={<SaveOutlined />} loading={loading} onClick={handleSave}>保存</Button>
              </Space>
            }
            bodyStyle={{ flex: 1, display: 'flex', flexDirection: 'column', height: 'calc(100% - 57px)', padding: 0 }}
            style={{ height: '100%', display: 'flex', flexDirection: 'column' }}
          >
            <div style={{ padding: '8px 16px', borderBottom: '1px solid #f0f0f0', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <Text strong>内容</Text>
              <Space>
                <Text type="secondary" style={{ fontSize: 12 }}>预览</Text>
                <Switch 
                  checkedChildren={<EyeOutlined />} 
                  unCheckedChildren={<EditOutlined />} 
                  checked={preview} 
                  onChange={setPreview} 
                />
              </Space>
            </div>
            
            <div style={{ flex: 1, overflow: 'auto', padding: 16 }}>
              {preview ? (
                <div style={{ whiteSpace: 'pre-wrap', fontFamily: 'monospace', lineHeight: 1.6 }}>
                  {/* Simple markdown simulation */}
                  {currentFile.content.split('\n').map((line, i) => {
                    if (line.startsWith('# ')) return <h1 key={i}>{line.substring(2)}</h1>;
                    if (line.startsWith('## ')) return <h2 key={i}>{line.substring(3)}</h2>;
                    if (line.startsWith('- ')) return <li key={i} style={{ marginLeft: 20 }}>{line.substring(2)}</li>;
                    return <p key={i} style={{ margin: '4px 0' }}>{line}</p>;
                  })}
                </div>
              ) : (
                <TextArea
                  value={currentFile.content}
                  onChange={handleContentChange}
                  style={{ height: '100%', resize: 'none', fontFamily: 'monospace', border: 'none', boxShadow: 'none' }}
                  placeholder="在此输入 Markdown 内容..."
                />
              )}
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default CourtRules;
