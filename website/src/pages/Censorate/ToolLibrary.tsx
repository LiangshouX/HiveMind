import React, { useState } from 'react';
import { Typography, Card, Row, Col, Switch, Space, message, Badge, Button } from 'antd';

const { Title, Text, Paragraph } = Typography;

interface Tool {
  id: string;
  name: string;
  description: string;
  enabled: boolean;
}

const initialTools: Tool[] = [
  { id: '1', name: 'execute_shell_command', description: 'Execute shell commands', enabled: true },
  { id: '2', name: 'read_file', description: 'Read file contents', enabled: true },
  { id: '3', name: 'write_file', description: 'Write content to file', enabled: true },
  { id: '4', name: 'edit_file', description: 'Edit file using find-and-replace', enabled: true },
  { id: '5', name: 'browser_use', description: 'Browser automation and web interaction', enabled: true },
  { id: '6', name: 'desktop_screenshot', description: 'Capture desktop screenshots', enabled: true },
  { id: '7', name: 'send_file_to_user', description: 'Send files to user', enabled: true },
  { id: '8', name: 'get_current_time', description: 'Get current date and time', enabled: true },
  { id: '9', name: 'get_token_usage', description: 'Get llm token usage', enabled: true },
];

const ToolLibrary: React.FC = () => {
  const [tools, setTools] = useState<Tool[]>(initialTools);

  const toggleEnable = (id: string, checked: boolean) => {
    setTools(tools.map(t => t.id === id ? { ...t, enabled: checked } : t));
    message.success(`工具已${checked ? '启用' : '禁用'}`);
  };

  return (
    <div style={{ height: 'calc(100vh - 120px)', display: 'flex', flexDirection: 'column' }}>
      <div style={{ marginBottom: 16 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
           <div>
            <Title level={4} style={{ margin: 0 }}>内置工具</Title>
            <Text type="secondary">管理内置工具及其启用状态。禁用的工具将不会提供给智能体使用。</Text>
           </div>
           <Space>
             <Button type="text" style={{ color: '#1890ff' }} onClick={() => { setTools(tools.map(t => ({...t, enabled: true}))); message.success('全部启用'); }}>全部启用</Button>
             <Button type="text" style={{ color: '#ff4d4f' }} onClick={() => { setTools(tools.map(t => ({...t, enabled: false}))); message.success('全部禁用'); }}>全部禁用</Button>
           </Space>
        </div>
      </div>

      <div style={{ flex: 1, overflowY: 'auto' }}>
        <Row gutter={[16, 16]}>
          {tools.map(tool => (
            <Col xs={24} sm={12} md={8} lg={6} key={tool.id}>
              <Card 
                hoverable
                style={{ height: '100%', display: 'flex', flexDirection: 'column' }}
                bodyStyle={{ flex: 1, display: 'flex', flexDirection: 'column' }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 12 }}>
                  <Text strong style={{ fontSize: 16 }}>{tool.name}</Text>
                  <Space>
                     <Badge status="success" text="已启用" style={{ display: tool.enabled ? 'block' : 'none' }} />
                  </Space>
                </div>
                
                <div style={{ flex: 1, marginBottom: 16 }}>
                  <Paragraph type="secondary" ellipsis={{ rows: 2 }}>
                    {tool.description}
                  </Paragraph>
                </div>

                <div style={{ marginTop: 'auto', textAlign: 'right' }}>
                  <Switch checked={tool.enabled} onChange={checked => toggleEnable(tool.id, checked)} />
                </div>
              </Card>
            </Col>
          ))}
        </Row>
      </div>
    </div>
  );
};

// Add Button import that was missing
// import { Button } from 'antd';

export default ToolLibrary;