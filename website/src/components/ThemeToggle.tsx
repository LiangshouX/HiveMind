import React from 'react';
import { Button, Tooltip } from 'antd';
import { BulbOutlined, MoonOutlined } from '@ant-design/icons';
import { useTheme } from '../providers/ThemeProvider';

/**
 * 主题切换按钮。
 *
 * <p>主题状态统一由 {@link ThemeProvider} 持有，本组件只负责展示与触发，
 * 避免出现"CSS 变量已切换但 antd 主题没跟上"这类半切换状态。</p>
 */
const ThemeToggle: React.FC = () => {
  const { isDarkMode, toggleTheme } = useTheme();

  return (
    <Tooltip title={`切换至${isDarkMode ? '明亮' : '暗黑'}主题 (Ctrl/Cmd+Shift+L)`}>
      <Button
        type="text"
        aria-label={isDarkMode ? '切换至明亮主题' : '切换至暗黑主题'}
        icon={isDarkMode ? <BulbOutlined /> : <MoonOutlined />}
        onClick={toggleTheme}
        style={{
          fontSize: '18px',
          color: 'var(--td-highlight)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          width: 40,
          height: 40,
        }}
      />
    </Tooltip>
  );
};

export default ThemeToggle;
