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
  ApiOutlined
} from '@ant-design/icons';
import type { MenuProps } from 'antd';
import { Layout, Menu, theme } from 'antd';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';

const { Content, Footer, Sider } = Layout;

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
    token: { colorBgContainer, borderRadiusLG },
  } = theme.useToken();
  const navigate = useNavigate();
  const location = useLocation();

  const onClick: MenuProps['onClick'] = (e) => {
    navigate(e.key);
  };

  // Determine selected keys based on current path
  const selectedKeys = [location.pathname];
  // Determine open keys based on current path
  const defaultOpenKeys = items
    .filter(item => item && 'children' in item && item.children?.some((child: any) => child.key === location.pathname))
    .map(item => item?.key as string);

  return (
    <Layout style={{ height: '100vh' }}>
      <Sider 
        collapsible 
        collapsed={collapsed} 
        onCollapse={(value) => setCollapsed(value)}
        style={{ overflow: 'auto', height: '100vh', position: 'fixed', left: 0, top: 0, bottom: 0 }}
      >
        <div style={{ height: 32, margin: 16, background: 'rgba(255, 255, 255, 0.2)', textAlign: 'center', color: 'white', lineHeight: '32px', fontWeight: 'bold', whiteSpace: 'nowrap', overflow: 'hidden' }}>
          {collapsed ? 'TD' : 'Tang Dynasty'}
        </div>
        <Menu 
          theme="dark" 
          defaultSelectedKeys={selectedKeys} 
          defaultOpenKeys={defaultOpenKeys}
          mode="inline" 
          items={items} 
          onClick={onClick}
          selectedKeys={selectedKeys}
          style={{ paddingBottom: 48 }}
        />
      </Sider>
      <Layout style={{ marginLeft: collapsed ? 80 : 200, transition: 'all 0.2s' }}>
        {/* <Header style={{ padding: 0, background: colorBgContainer }} /> */}
        <Content style={{ margin: '16px 16px 0', overflow: 'initial' }}>
          {/* Breadcrumb could go here */}
          <div
            style={{
              padding: 24,
              minHeight: 'calc(100vh - 110px)',
              background: colorBgContainer,
              borderRadius: borderRadiusLG,
            }}
          >
            <Outlet />
          </div>
        </Content>
        <Footer style={{ textAlign: 'center' }}>
          Tang Dynasty AI Assistant ©{new Date().getFullYear()} Created by Agents
        </Footer>
      </Layout>
    </Layout>
  );
};

export default MainLayout;
