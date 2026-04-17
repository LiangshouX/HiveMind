import React, { useEffect, useState } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ConfigProvider, App as AntApp } from 'antd';
import { getTangTheme } from './theme';
import MainLayout from './layouts/MainLayout';
import EdictLibrary from './pages/MorningCourt/EdictLibrary';
import Channels from './pages/ImperialStudy/Channels';
import EdictBoard from './pages/ImperialStudy/EdictBoard';
import Memorials from './pages/ImperialStudy/Memorials';
import ScheduledTasks from './pages/ImperialStudy/ScheduledTasks';
import CourtRules from './pages/Censorate/CourtRules';
import SkillLibrary from './pages/Censorate/SkillLibrary';
import ToolLibrary from './pages/Censorate/ToolLibrary';
import MCP from './pages/Censorate/MCP';
import OfficialManagement from './pages/Censorate/OfficialManagement';
import Models from './pages/Dalisi/Models';
import EnvVars from './pages/Dalisi/EnvVars';
import Security from './pages/Dalisi/Security';
import TokenUsage from './pages/Dalisi/TokenUsage';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { ProfilePage } from './pages/ProfilePage';
import { ChatPage } from './pages/MorningCourt/ChatPage.tsx';
import { ProtectedRoute } from './components/common/ProtectedRoute';
import { AuthProvider } from './providers/AuthProvider';
import { ThemeProvider } from './providers/ThemeProvider';

const App: React.FC = () => {
  const [isDark, setIsDark] = useState(true);

  useEffect(() => {
    // 初始化检查主题 - 这是必要的初始化逻辑，不是级联更新
    const currentTheme = document.documentElement.getAttribute('data-theme') ||
                         localStorage.getItem('theme') ||
                         (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setIsDark(currentTheme === 'dark');

    // 监听 data-theme 属性变化
    const observer = new MutationObserver((mutations) => {
      mutations.forEach((mutation) => {
        if (mutation.attributeName === 'data-theme') {
          const newTheme = document.documentElement.getAttribute('data-theme');
          setIsDark(newTheme === 'dark');
        }
      });
    });

    observer.observe(document.documentElement, { attributes: true });

    return () => observer.disconnect();
  }, []);

  return (
    <ConfigProvider theme={getTangTheme(isDark)}>
      <AntApp>
        <BrowserRouter>
          <Routes>
            {/* 登录页 - 不嵌套MainLayout */}
            <Route
              path="/login"
              element={<LoginPage />}
            />
            <Route
              path="/register"
              element={<RegisterPage />}
            />
            
            {/* 需要MainLayout的路由 */}
            <Route path="/" element={<MainLayout />}>
              <Route index element={<Navigate to="/chat" replace />} />
              
              {/* 个人资料页 - 需要登录 */}
              <Route
                path="profile"
                element={
                  <ProtectedRoute>
                    <ProfilePage />
                  </ProtectedRoute>
                }
              />
              
              {/* 聊天页 - 支持可选的 sessionId 参数 */}
              <Route
                path="chat/:sessionId?"
                element={
                  <ProtectedRoute>
                    <ChatPage />
                  </ProtectedRoute>
                }
              />
              
              <Route path="edict-library" element={<EdictLibrary />} />
              <Route path="channels" element={<Channels />} />
              <Route path="edict-board" element={<EdictBoard />} />
              <Route path="memorials" element={<Memorials />} />
              <Route path="scheduled-tasks" element={<ScheduledTasks />} />
              <Route path="court-rules" element={<CourtRules />} />
              <Route path="skill-library" element={<SkillLibrary />} />
              <Route path="tool-library" element={<ToolLibrary />} />
              <Route path="mcp" element={<MCP />} />
              <Route path="official-management" element={<OfficialManagement />} />
              <Route path="models" element={<Models />} />
              <Route path="env-vars" element={<EnvVars />} />
              <Route path="security" element={<Security />} />
              <Route path="token-usage" element={<TokenUsage />} />
            </Route>
          </Routes>
        </BrowserRouter>
      </AntApp>
    </ConfigProvider>
  );
};

// 包装App组件，提供Auth和Theme上下文
const WrappedApp: React.FC = () => (
  <ThemeProvider>
    <AuthProvider>
      <App />
    </AuthProvider>
  </ThemeProvider>
);

export default WrappedApp;
