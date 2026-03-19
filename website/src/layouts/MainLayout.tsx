import React, { useState } from 'react';
import {
  CommentOutlined,
  ReadOutlined,
  AppstoreOutlined,
  SettingOutlined,
  SafetyCertificateOutlined,
  ToolOutlined,
  UserOutlined,
  RobotOutlined,
  DatabaseOutlined,
  FieldTimeOutlined,
  ProfileOutlined,
  MessageOutlined,
  BookOutlined,
  ApiOutlined,
  MenuUnfoldOutlined,
  MenuFoldOutlined
} from '@ant-design/icons';
import type { MenuProps } from 'antd';
import { Layout, Menu, theme, Button, Typography, Space } from 'antd';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import ThemeToggle from '../components/ThemeToggle';

const { Header, Content, Footer, Sider } = Layout;
const { Title, Text } = Typography;

type MenuItem = Required<MenuProps>['items'][number];

function getItem(
  label: React.ReactNode,
  key: React.Key,
  icon?: React.ReactNode,
  children?: MenuItem[],
): MenuItem {
  return {
    key,
    icon,
    children,
    label,
  } as MenuItem;
}

const items: MenuItem[] = [
  getItem('早朝', 'morning-court', <CommentOutlined />, [
    getItem('上朝', '/chat', <MessageOutlined />),
    getItem('旨意库', '/edict-library', <BookOutlined />),
  ]),
  getItem('御书房', 'imperial-study', <ReadOutlined />, [
    getItem('频道', '/channels', <AppstoreOutlined />),
    getItem('旨意看板', '/edict-board', <ProfileOutlined />),
    getItem('奏折', '/memorials', <ReadOutlined />),
    getItem('定时任务', '/scheduled-tasks', <FieldTimeOutlined />),
  ]),
  getItem('御史台', 'censorate', <UserOutlined />, [
    getItem('朝纲', '/court-rules', <ReadOutlined />),
    getItem('技能库', '/skill-library', <RobotOutlined />),
    getItem('工具库', '/tool-library', <ToolOutlined />),
    getItem('MCP', '/mcp', <ApiOutlined />),
    getItem('官员管理', '/official-management', <UserOutlined />),
  ]),
  getItem('大理寺', 'dalisi', <SafetyCertificateOutlined />, [
    getItem('模型', '/models', <RobotOutlined />),
    getItem('环境变量', '/env-vars', <SettingOutlined />),
    getItem('御林军', '/security', <SafetyCertificateOutlined />),
    getItem('大司农', '/token-usage', <DatabaseOutlined />),
  ]),
];

const MainLayout: React.FC = () => {
  const [collapsed, setCollapsed] = useState(false);
  const {
    token: { borderRadiusLG },
  } = theme.useToken();
  const navigate = useNavigate();
  const location = useLocation();

  const onClick: MenuProps['onClick'] = (e) => {
    navigate(e.key);
  };

  const selectedKeys = [location.pathname];
  const defaultOpenKeys = items
    .filter(item => item && 'children' in item && item.children?.some((child: any) => child.key === location.pathname))
    .map(item => item?.key as string);

  return (
    <Layout style={{ height: '100vh' }} className="layout-pattern">
      <Sider 
        trigger={null}
        collapsible 
        collapsed={collapsed} 
        width={240}
        style={{ 
          overflow: 'auto', 
          height: '100vh', 
          position: 'fixed', 
          left: 0, 
          top: 0, 
          bottom: 0,
          borderRight: '1px solid var(--td-border-light)',
          boxShadow: 'var(--td-shadow-elevated)',
          zIndex: 10,
          background: 'var(--td-sider-bg)'
        }}
      >
        <div style={{ 
          height: 80, 
          margin: '16px', 
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          borderBottom: '1px solid var(--td-border-light)',
          paddingBottom: '16px'
        }}>
          <Title level={collapsed ? 3 : 2} className="imperial-heading" style={{ margin: 0, color: 'var(--td-primary)', textShadow: '0 2px 4px rgba(0,0,0,0.2)' }}>
            {collapsed ? '唐' : '大唐'}
          </Title>
          {!collapsed && (
            <Text style={{ color: 'var(--td-highlight)', fontSize: '12px', letterSpacing: '4px', marginTop: '4px' }}>
              TANG DYNASTY
            </Text>
          )}
        </div>
        <Menu 
          mode="inline" 
          defaultSelectedKeys={selectedKeys} 
          defaultOpenKeys={defaultOpenKeys}
          items={items} 
          onClick={onClick}
          selectedKeys={selectedKeys}
          style={{ 
            borderRight: 0,
            padding: '0 8px 48px 8px',
            background: 'transparent'
          }}
        />
      </Sider>
      <Layout style={{ marginLeft: collapsed ? 80 : 240, transition: 'all 0.3s cubic-bezier(0.645, 0.045, 0.355, 1)', background: 'transparent' }}>
        <Header style={{ 
          padding: 0, 
          background: 'var(--td-header-bg)', 
          backdropFilter: 'blur(8px)',
          borderBottom: '1px solid var(--td-border-light)',
          display: 'flex',
          alignItems: 'center',
          position: 'sticky',
          top: 0,
          zIndex: 9
        }}>
          <Button
            type="text"
            icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            onClick={() => setCollapsed(!collapsed)}
            style={{
              fontSize: '16px',
              width: 64,
              height: 64,
              color: 'var(--td-highlight)'
            }}
          />
          <div style={{ flex: 1 }} />
          <Space style={{ paddingRight: 24 }} size="large">
            <ThemeToggle />
            <Space>
              <Text style={{ color: 'var(--td-text-secondary)' }}>当前朝代：</Text>
              <Text strong style={{ color: 'var(--td-highlight)' }}>贞观</Text>
            </Space>
          </Space>
        </Header>
        <Content style={{ margin: '24px 24px 0', overflow: 'initial' }}>
          <div
            style={{
              minHeight: 'calc(100vh - 150px)',
              background: 'var(--td-bg-container)',
              borderRadius: borderRadiusLG,
              border: '1px solid var(--td-border-light)',
              boxShadow: 'var(--td-shadow-base)',
              overflow: 'hidden',
              position: 'relative'
            }}
          >
            <Outlet />
          </div>
        </Content>
        <Footer style={{ textAlign: 'center', background: 'transparent', borderTop: 'none' }}>
          <Text style={{ color: 'var(--td-text-tertiary)' }}>Tang Dynasty AI Assistant ©{new Date().getFullYear()} · 三省六部制</Text>
        </Footer>
      </Layout>
    </Layout>
  );
};

export default MainLayout;
