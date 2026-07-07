import React, { useEffect, useState } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ConfigProvider, App as AntApp } from 'antd';
import { getHiveMindTheme } from './theme';
import MainLayout from './layouts/MainLayout';
import TaskTemplateLibrary from './pages/Workspace/TaskTemplateLibrary';
import ScheduledTasks from './pages/TaskCenter/ScheduledTasks';
import CourtRules from './pages/Admin/CourtRules';
import SkillLibrary from './pages/Admin/SkillLibrary';
import ToolLibrary from './pages/Admin/ToolLibrary';
import TokenUsage from './pages/Services/TokenUsage';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { ProfilePage } from './pages/ProfilePage';
import { ChatPage } from './pages/Workspace/ChatPage.tsx';
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
    <ConfigProvider theme={getHiveMindTheme(isDark)}>
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
              
              {/* 需要登录的页面 - 全部受 ProtectedRoute 保护 */}
              <Route path="task-templates" element={<ProtectedRoute><TaskTemplateLibrary /></ProtectedRoute>} />
              <Route path="scheduled-tasks" element={<ProtectedRoute><ScheduledTasks /></ProtectedRoute>} />
              <Route path="workspace-settings" element={<ProtectedRoute><CourtRules /></ProtectedRoute>} />
              <Route path="skill-library" element={<ProtectedRoute><SkillLibrary /></ProtectedRoute>} />
              <Route path="tool-library" element={<ProtectedRoute><ToolLibrary /></ProtectedRoute>} />
              <Route path="token-usage" element={<ProtectedRoute><TokenUsage /></ProtectedRoute>} />
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
