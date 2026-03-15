import React, { useState } from 'react';
import { Layout, List, Input, Button, Avatar, Typography, Space } from 'antd';
import { 
  PlusOutlined, 
  SendOutlined, 
  UserOutlined, 
  RobotOutlined, 
  MoreOutlined 
} from '@ant-design/icons';
import dayjs from 'dayjs';

const { Sider, Content } = Layout;
const { Text, Title } = Typography;

// Mock Data
const MOCK_CHATS = [
  { id: '1', title: 'AI 早报', lastMsg: '今日科技新闻汇总...', time: '10:00', active: true },
  { id: '2', title: '拉取早报', lastMsg: '正在获取...', time: 'Yesterday', active: false },
  { id: '3', title: '使用MCP工具接一下', lastMsg: '好的，正在尝试...', time: 'Yesterday', active: false },
  { id: '4', title: 'hi', lastMsg: 'Hello!', time: 'Yesterday', active: false },
  { id: '5', title: '@QH', lastMsg: '有什么可以帮您？', time: 'Yesterday', active: false },
];

const MOCK_MESSAGES = [
  { id: '1', role: 'user', content: 'hi', time: '2026-03-12 10:00:00' },
  { id: '2', role: 'ai', content: 'Hello! How can I help you today?', time: '2026-03-12 10:00:05' },
  { id: '3', role: 'user', content: 'AI 早报', time: '2026-03-12 10:01:00' },
  { id: '4', role: 'ai', content: '很抱歉，今日无法直接访问原始指定的两个新闻源 (InfoQ 和 ACM TechNews)，它们都返回了错误或访问被阻止。不过，我通过...', time: '2026-03-12 10:01:10' },
];

const Chat: React.FC = () => {
  const [inputValue, setInputValue] = useState('');
  const [messages, setMessages] = useState(MOCK_MESSAGES);
  const [chats, setChats] = useState(MOCK_CHATS);
  const [activeChatId, setActiveChatId] = useState('1');

  const handleSend = () => {
    if (!inputValue.trim()) return;
    const newMsg = {
      id: Date.now().toString(),
      role: 'user',
      content: inputValue,
      time: dayjs().format('YYYY-MM-DD HH:mm:ss'),
    };
    setMessages([...messages, newMsg]);
    setInputValue('');
    
    // Update chat list last message
    setChats(prevChats => 
      prevChats.map(chat => 
        chat.id === activeChatId 
          ? { ...chat, lastMsg: inputValue, time: dayjs().format('HH:mm') } 
          : chat
      )
    );
    
    // Mock AI Reply
    setTimeout(() => {
      const aiMsg = {
        id: (Date.now() + 1).toString(),
        role: 'ai',
        content: `I received: ${inputValue}`,
        time: dayjs().format('YYYY-MM-DD HH:mm:ss'),
      };
      setMessages(prev => [...prev, aiMsg]);
      
      // Update chat list last message again with AI reply
      setChats(prevChats => 
        prevChats.map(chat => 
          chat.id === activeChatId 
            ? { ...chat, lastMsg: aiMsg.content, time: dayjs().format('HH:mm') } 
            : chat
        )
      );
    }, 1000);
  };

  const handleChatSelect = (chatId: string) => {
    setActiveChatId(chatId);
    setChats(prevChats => 
      prevChats.map(chat => ({
        ...chat,
        active: chat.id === chatId
      }))
    );
    // In a real app, we would fetch messages for the selected chat here
    // For now, we'll just clear the input
    setInputValue('');
  };

  const handleNewChat = () => {
    const newChatId = Date.now().toString();
    const newChat = {
      id: newChatId,
      title: 'New Conversation',
      lastMsg: '...',
      time: dayjs().format('HH:mm'),
      active: true
    };
    
    setChats(prevChats => [
      newChat,
      ...prevChats.map(chat => ({ ...chat, active: false }))
    ]);
    setActiveChatId(newChatId);
    setMessages([]); // Clear messages for new chat
  };

  return (
    <Layout style={{ height: 'calc(100vh - 100px)', background: '#fff' }}>
      <Sider width={280} theme="light" style={{ borderRight: '1px solid #f0f0f0' }}>
        <div style={{ padding: '16px' }}>
          <div style={{ marginBottom: 16, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <Text strong>Work with CoPaw</Text>
            <Button type="text" icon={<MoreOutlined />} />
          </div>
          <Button type="primary" block icon={<PlusOutlined />} onClick={handleNewChat} style={{ marginBottom: 16, borderRadius: 20 }}>
            New Chat
          </Button>
          <List
            itemLayout="horizontal"
            dataSource={chats}
            renderItem={(item) => (
              <List.Item 
                onClick={() => handleChatSelect(item.id)}
                style={{ 
                  padding: '12px', 
                  cursor: 'pointer',
                  background: item.active ? '#e6f7ff' : 'transparent',
                  borderRadius: '8px',
                  marginBottom: '4px'
                }}
              >
                <List.Item.Meta
                  title={<Text strong={item.active}>{item.title}</Text>}
                  description={<Text type="secondary" ellipsis style={{ fontSize: 12 }}>{item.lastMsg}</Text>}
                />
                <div style={{ fontSize: 10, color: '#999' }}>{item.time}</div>
              </List.Item>
            )}
          />
        </div>
      </Sider>
      
      <Content style={{ display: 'flex', flexDirection: 'column' }}>
        <div style={{ padding: '16px', borderBottom: '1px solid #f0f0f0', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Title level={4} style={{ margin: 0 }}>{chats.find(c => c.active)?.title || 'Chat'}</Title>
          <Space>
             <Button>更新日志</Button>
             <Button>文档</Button>
          </Space>
        </div>
        
        <div style={{ flex: 1, overflowY: 'auto', padding: '24px' }}>
          {messages.map((msg) => (
            <div key={msg.id} style={{ marginBottom: 24, display: 'flex', flexDirection: 'column', alignItems: msg.role === 'user' ? 'flex-end' : 'flex-start' }}>
              <div style={{ display: 'flex', flexDirection: msg.role === 'user' ? 'row-reverse' : 'row', gap: 12, maxWidth: '80%' }}>
                <Avatar icon={msg.role === 'user' ? <UserOutlined /> : <RobotOutlined />} style={{ backgroundColor: msg.role === 'user' ? '#87d068' : '#1677ff' }} />
                <div style={{ 
                  background: msg.role === 'user' ? '#95de64' : '#f0f0f0', 
                  padding: '12px 16px', 
                  borderRadius: '12px',
                  borderTopRightRadius: msg.role === 'user' ? 2 : 12,
                  borderTopLeftRadius: msg.role === 'ai' ? 2 : 12,
                }}>
                  <Text>{msg.content}</Text>
                </div>
              </div>
              <Text type="secondary" style={{ fontSize: 10, marginTop: 4, marginInline: 44 }}>{msg.time}</Text>
            </div>
          ))}
        </div>

        <div style={{ padding: '24px', borderTop: '1px solid #f0f0f0' }}>
           <div style={{ 
             border: '1px solid #d9d9d9', 
             borderRadius: '12px', 
             padding: '12px',
             boxShadow: '0 2px 8px rgba(0,0,0,0.05)'
           }}>
             <Input.TextArea 
               value={inputValue}
               onChange={e => setInputValue(e.target.value)}
               placeholder="输入消息..." 
               autoSize={{ minRows: 1, maxRows: 6 }}
               bordered={false}
               onPressEnter={(e) => {
                 if (!e.shiftKey) {
                   e.preventDefault();
                   handleSend();
                 }
               }}
               style={{ resize: 'none', marginBottom: 8 }}
             />
             <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
               <Button type="primary" icon={<SendOutlined />} onClick={handleSend} disabled={!inputValue.trim()} />
             </div>
           </div>
           <Text type="secondary" style={{ fontSize: 12, display: 'block', textAlign: 'center', marginTop: 12 }}>
             懂你所需，伴你左右
           </Text>
        </div>
      </Content>
    </Layout>
  );
};

export default Chat;
