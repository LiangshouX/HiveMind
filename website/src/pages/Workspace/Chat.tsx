import React, { useState, useEffect, useRef } from 'react';
import { Layout, Input, Button, Avatar, Typography, Space, Tooltip, Badge } from 'antd';
import { 
  PlusOutlined, 
  SendOutlined, 
  UserOutlined, 
  MoreOutlined,
  ThunderboltOutlined,
  AudioOutlined,
  PaperClipOutlined,
  HistoryOutlined
} from '@ant-design/icons';
import dayjs from 'dayjs';

const { Sider, Content } = Layout;
const { Text, Title } = Typography;

// Mock Data
const MOCK_CHATS = [
  { id: '1', title: '查看报告：AI 早报', lastMsg: '今日科技新闻汇总...', time: '10:00', active: true },
  { id: '2', title: '请求：拉取早报', lastMsg: '正在获取...', time: '昨日', active: false },
  { id: '3', title: '调用MCP工具', lastMsg: '好的，正在尝试...', time: '昨日', active: false },
];

const MOCK_MESSAGES = [
  { id: '1', role: 'user', content: '请帮我查看今日的AI早报。', time: '2026-03-12 10:00:00' },
  { id: '2', role: 'ai', department: '规划组', content: '规划Agent已接收任务。已分析需求，现调度【数据组】拉取数据，【文档组】排版...', time: '2026-03-12 10:00:05' },
  { id: '3', role: 'ai', department: '执行组', content: '臣执行Agent奉命执行：\n\n1. 数据组已获取 InfoQ 最新动态。\n2. 文档组已完成格式化整理。\n\n呈上今日早报：\n- GPT-5 传闻将于下月发布\n- 芯片产能突破...', time: '2026-03-12 10:01:10' },
];

/**
 * @deprecated 此组件已弃用，请使用 ConversationWorkspace 组件替代
 */
const Chat: React.FC = () => {
  const [inputValue, setInputValue] = useState('');
  const [messages, setMessages] = useState(MOCK_MESSAGES);
  const [chats, setChats] = useState(MOCK_CHATS);
  const [activeChatId, setActiveChatId] = useState('1');
  const [isTyping, setIsTyping] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const panelHeight = 'calc(100vh)';

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, isTyping]);

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
    setIsTyping(true);
    
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
        department: '助理',
        content: `收到。关于“${inputValue}”，臣以为...`,
        time: dayjs().format('YYYY-MM-DD HH:mm:ss'),
      };
      setMessages(prev => [...prev, aiMsg]);
      setIsTyping(false);
      
      setChats(prevChats => 
        prevChats.map(chat => 
          chat.id === activeChatId 
            ? { ...chat, lastMsg: aiMsg.content, time: dayjs().format('HH:mm') } 
            : chat
        )
      );
    }, 1500);
  };

  return (
    <Layout style={{ height: panelHeight, width: '100%', background: 'transparent', display: 'flex', flexDirection: 'row', gap: '24px', overflow: 'hidden', minHeight: 0 }}>
      {/* Session List */}
      <Sider 
        width={300} 
        style={{ 
          background: 'var(--td-bg-elevated)', 
          borderRadius: '12px',
          border: '1px solid var(--td-border-light)',
          boxShadow: 'var(--td-shadow-base)',
          overflow: 'hidden',
          display: 'flex',
          flexDirection: 'column',
          height: '100%',
          minHeight: 0
        }}
      >
        <div style={{ padding: '20px', borderBottom: '1px solid var(--td-border-light)' }}>
          <Button 
            type="primary" 
            block 
            icon={<PlusOutlined />} 
            onClick={() => {}} 
            style={{ 
              height: '44px', 
              borderRadius: '8px', 
              background: 'var(--td-primary)',
              border: 'none',
              boxShadow: 'var(--td-shadow-primary)',
              fontSize: '16px',
              fontWeight: 'bold',
              letterSpacing: '2px',
              color: 'var(--td-text-inverse)'
            }}
          >
            新建对话
          </Button>
        </div>
        <div style={{ flex: 1, overflowY: 'auto', padding: '12px', minHeight: 0 }}>
          <Text type="secondary" style={{ fontSize: '12px', padding: '0 8px 8px', display: 'block', color: 'var(--td-text-tertiary)' }}>近期对话</Text>
          {chats.map(chat => (
            <div
              key={chat.id}
              onClick={() => setActiveChatId(chat.id)}
              style={{
                padding: '16px',
                borderRadius: '8px',
                cursor: 'pointer',
                marginBottom: '8px',
                background: chat.active ? 'var(--td-item-selected-bg)' : 'transparent',
                border: `1px solid ${chat.active ? 'var(--td-border-color)' : 'transparent'}`,
                transition: 'all 0.3s ease',
                position: 'relative',
                overflow: 'hidden'
              }}
              className="chat-session-item"
            >
              {chat.active && (
                <div style={{ position: 'absolute', left: 0, top: 0, bottom: 0, width: '3px', background: 'var(--td-highlight)' }} />
              )}
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
                <Text strong style={{ color: chat.active ? 'var(--td-highlight)' : 'var(--td-text-base)', fontSize: '14px' }} ellipsis>
                  {chat.title}
                </Text>
                <Text type="secondary" style={{ fontSize: '12px', color: 'var(--td-text-tertiary)' }}>{chat.time}</Text>
              </div>
              <Text type="secondary" style={{ fontSize: '13px', color: 'var(--td-text-secondary)' }} ellipsis>
                {chat.lastMsg}
              </Text>
            </div>
          ))}
        </div>
      </Sider>
      
      {/* Chat Area */}
      <Content style={{ 
        display: 'flex', 
        flexDirection: 'column', 
        background: 'var(--td-chat-panel-bg)', 
        borderRadius: '12px',
        border: '1px solid var(--td-border-light)',
        backdropFilter: 'blur(10px)',
        boxShadow: 'var(--td-shadow-base)',
        height: '100%',
        overflow: 'hidden',
        minHeight: 0
      }}>
        {/* Chat Header */}
        <div style={{ 
          padding: '20px 24px', 
          borderBottom: '1px solid var(--td-border-light)', 
          display: 'flex', 
          justifyContent: 'space-between', 
          alignItems: 'center',
          background: 'var(--td-bg-elevated)',
          borderTopLeftRadius: '12px',
          borderTopRightRadius: '12px',
        }}>
          <div>
            <Title level={4} style={{ margin: 0, color: 'var(--td-text-base)' }} className="chat-heading">
              {chats.find(c => c.active)?.title || '未命名对话'}
            </Title>
            <Space style={{ marginTop: '4px' }}>
              <Badge status="processing" color="var(--td-highlight)" />
              <Text type="secondary" style={{ fontSize: '12px', color: 'var(--td-text-secondary)' }}>AI就绪中</Text>
            </Space>
          </div>
          <Space>
             <Tooltip title="查看任务流转">
               <Button type="text" icon={<HistoryOutlined style={{ color: 'var(--td-highlight)' }} />} />
             </Tooltip>
             <Button type="text" icon={<MoreOutlined style={{ color: 'var(--td-text-tertiary)' }} />} />
          </Space>
        </div>
        
        {/* Messages */}
        <div style={{ flex: 1, overflowY: 'auto', padding: '32px 24px', minHeight: 0 }}>
          <div style={{ textAlign: 'center', marginBottom: '32px' }}>
            <Text type="secondary" style={{ fontSize: '12px', background: 'var(--td-bg-base)', padding: '4px 12px', borderRadius: '12px', color: 'var(--td-text-secondary)' }}>
              2026年6月9日
            </Text>
          </div>
          
          {messages.map((msg) => (
            <div 
              key={msg.id} 
              style={{ 
                marginBottom: '32px', 
                display: 'flex', 
                flexDirection: 'column', 
                alignItems: msg.role === 'user' ? 'flex-end' : 'flex-start' 
              }}
            >
              <div style={{ 
                display: 'flex', 
                flexDirection: msg.role === 'user' ? 'row-reverse' : 'row', 
                gap: '16px', 
                maxWidth: '85%' 
              }}>
                {/* Avatar */}
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '4px' }}>
                  <Avatar 
                    size={48}
                    icon={msg.role === 'user' ? <UserOutlined /> : <span style={{ fontFamily: 'STZhongsong', fontWeight: 'bold' }}>{msg.department?.[0] || 'AI'}</span>} 
                    style={{ 
                      backgroundColor: msg.role === 'user' ? 'var(--td-primary)' : 'var(--td-bg-container)',
                      border: `2px solid ${msg.role === 'user' ? 'var(--td-primary-hover)' : 'var(--td-highlight)'}`,
                      boxShadow: 'var(--td-shadow-base)',
                      color: msg.role === 'user' ? 'var(--td-text-inverse)' : 'var(--td-text-base)'
                    }} 
                  />
                  {msg.role === 'ai' && (
                    <Text style={{ fontSize: '10px', color: 'var(--td-highlight)', fontFamily: 'STZhongsong' }}>
                      {msg.department}
                    </Text>
                  )}
                </div>

                {/* Bubble */}
                <div style={{
                  position: 'relative',
                  background: msg.role === 'user' ? 'var(--td-msg-user-bg)' : 'var(--td-msg-ai-bg)',
                  border: msg.role === 'user' ? '1px solid var(--td-primary-hover)' : `1px solid var(--td-msg-ai-border)`,
                  padding: '16px 20px',
                  borderRadius: '12px',
                  borderTopRightRadius: msg.role === 'user' ? '2px' : '12px',
                  borderTopLeftRadius: msg.role === 'ai' ? '2px' : '12px',
                  boxShadow: 'var(--td-shadow-base)',
                  color: msg.role === 'user' ? 'var(--td-msg-user-text)' : 'var(--td-text-base)',
                  fontSize: '15px',
                  lineHeight: '1.6',
                  whiteSpace: 'pre-wrap',
                }}>
                  {/* Subtle texture for task request (user message) */}
                  {msg.role === 'user' && (
                    <div style={{
                      position: 'absolute',
                      top: 0, left: 0, right: 0, bottom: 0,
                      backgroundImage: 'radial-gradient(rgba(255,255,255,0.05) 1px, transparent 1px)',
                      backgroundSize: '10px 10px',
                      pointerEvents: 'none',
                      borderRadius: 'inherit'
                    }} />
                  )}
                  <span style={{ position: 'relative', zIndex: 1 }}>{msg.content}</span>
                </div>
              </div>
            </div>
          ))}
          
          {isTyping && (
            <div style={{ display: 'flex', gap: '16px', maxWidth: '85%' }}>
              <Avatar 
                size={48}
                style={{ 
                  backgroundColor: 'var(--td-bg-container)',
                  border: '2px solid var(--td-highlight)',
                  color: 'var(--td-text-base)',
                  fontFamily: 'STZhongsong',
                  fontWeight: 'bold'
                }} 
              >臣</Avatar>
              <div style={{
                background: 'var(--td-msg-ai-bg)',
                border: '1px solid var(--td-msg-ai-border)',
                padding: '16px 20px',
                borderRadius: '12px',
                borderTopLeftRadius: '2px',
                color: 'var(--td-highlight)',
                boxShadow: 'var(--td-shadow-base)'
              }}>
                <span className="typing-indicator">AI正在思考...</span>
              </div>
            </div>
          )}
          <div ref={messagesEndRef} />
        </div>

        {/* Input Area */}
        <div style={{ 
          padding: '24px', 
          background: 'var(--td-bg-elevated)',
          borderTop: '1px solid var(--td-border-light)',
          borderBottomLeftRadius: '12px',
          borderBottomRightRadius: '12px',
        }}>
           <div style={{ 
             background: 'var(--td-input-bg)',
             border: '1px solid var(--td-border-color)', 
             borderRadius: '12px', 
             padding: '12px 16px',
             boxShadow: 'inset 0 2px 8px rgba(0,0,0,0.02), 0 2px 8px var(--td-pattern-color)',
             transition: 'border-color 0.3s',
             position: 'relative'
           }}>
             <Input.TextArea 
               value={inputValue}
               onChange={e => setInputValue(e.target.value)}
               placeholder="输入任务..." 
               autoSize={{ minRows: 2, maxRows: 8 }}
               bordered={false}
               onPressEnter={(e) => {
                 if (!e.shiftKey) {
                   e.preventDefault();
                   handleSend();
                 }
               }}
               style={{ 
                 resize: 'none', 
                 color: 'var(--td-text-base)',
                 fontSize: '15px',
                 padding: '0 0 16px 0',
                 boxShadow: 'none',
                 background: 'transparent'
               }}
             />
             <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
               <Space>
                 <Tooltip title="附件">
                   <Button type="text" icon={<PaperClipOutlined />} style={{ color: 'var(--td-text-tertiary)' }} />
                 </Tooltip>
                 <Tooltip title="语音">
                   <Button type="text" icon={<AudioOutlined />} style={{ color: 'var(--td-text-tertiary)' }} />
                 </Tooltip>
                 <Tooltip title="快捷指令">
                   <Button type="text" icon={<ThunderboltOutlined />} style={{ color: 'var(--td-highlight)' }} />
                 </Tooltip>
               </Space>
               <Button 
                 type="primary" 
                 icon={<SendOutlined />} 
                 onClick={handleSend} 
                 disabled={!inputValue.trim()} 
                 style={{ 
                   background: inputValue.trim() ? 'var(--td-primary)' : 'var(--td-bg-base)',
                   borderColor: inputValue.trim() ? 'var(--td-primary)' : 'var(--td-border-light)',
                   color: inputValue.trim() ? 'var(--td-text-inverse)' : 'var(--td-text-tertiary)',
                   borderRadius: '8px',
                   boxShadow: inputValue.trim() ? 'var(--td-shadow-primary)' : 'none'
                 }}
               >
                 发送
               </Button>
             </div>
           </div>
           <Text type="secondary" style={{ fontSize: '12px', display: 'block', textAlign: 'center', marginTop: '16px', color: 'var(--td-text-tertiary)' }}>
             AI 多Agent协作架构
           </Text>
        </div>
      </Content>
      <style>{`
        .chat-session-item:hover {
          background: var(--td-item-hover-bg) !important;
        }
        .typing-indicator::after {
          content: '...';
          animation: typing 1.5s infinite;
        }
        @keyframes typing {
          0% { content: '.'; }
          33% { content: '..'; }
          66% { content: '...'; }
        }
        .ant-input-textarea-clear-icon {
          color: var(--td-highlight) !important;
        }
      `}</style>
    </Layout>
  );
};

export default Chat;
