import type { ThemeConfig } from 'antd';

// Create a function that returns theme based on current mode
export const getTangTheme = (isDark: boolean = true): ThemeConfig => ({
  token: {
    colorPrimary: isDark ? '#b32934' : '#a3222e',
    colorInfo: isDark ? '#c7a434' : '#b8912d',
    colorBgBase: isDark ? '#16171a' : '#f0f2f5',
    colorTextBase: isDark ? '#ececec' : '#1f2024',
    colorBgContainer: isDark ? '#1e1f24' : '#ffffff',
    colorBgElevated: isDark ? '#282a30' : '#ffffff',
    colorBorder: isDark ? 'rgba(199, 164, 52, 0.2)' : 'rgba(184, 145, 45, 0.3)',
    colorBorderSecondary: isDark ? 'rgba(199, 164, 52, 0.08)' : 'rgba(184, 145, 45, 0.15)',
    borderRadius: 8,
    fontFamily: '"Noto Sans SC", "PingFang SC", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif',
    fontFamilyCode: '"Fira Code", "JetBrains Mono", source-code-pro, Menlo, Monaco, Consolas, "Courier New", monospace',
  },
  components: {
    Layout: {
      siderBg: isDark ? '#16171a' : '#ffffff',
      headerBg: isDark ? 'rgba(22, 23, 26, 0.8)' : 'rgba(240, 242, 245, 0.85)',
      bodyBg: isDark ? '#16171a' : '#f0f2f5',
    },
    Menu: {
      itemBg: 'transparent',
      itemColor: isDark ? '#a8aab0' : '#5c5f66',
      itemHoverColor: isDark ? '#c7a434' : '#b8912d',
      itemSelectedColor: isDark ? '#c7a434' : '#b8912d',
      itemSelectedBg: isDark ? 'rgba(179, 41, 52, 0.15)' : 'rgba(163, 34, 46, 0.1)',
      itemHoverBg: isDark ? 'rgba(199, 164, 52, 0.08)' : 'rgba(184, 145, 45, 0.08)',
      itemBorderRadius: 6,
    },
    Button: {
      colorPrimary: isDark ? '#b32934' : '#a3222e',
      colorPrimaryHover: isDark ? '#d1323f' : '#851a24',
      colorPrimaryActive: isDark ? '#8f212a' : '#66141c',
      primaryShadow: isDark ? '0 4px 12px rgba(179, 41, 52, 0.3)' : '0 4px 12px rgba(163, 34, 46, 0.2)',
      defaultBg: isDark ? '#1e1f24' : '#ffffff',
      defaultColor: isDark ? '#c7a434' : '#b8912d',
      defaultBorderColor: isDark ? 'rgba(199, 164, 52, 0.2)' : 'rgba(184, 145, 45, 0.3)',
    },
    Card: {
      colorBgContainer: isDark ? '#1e1f24' : '#ffffff',
      colorBorderSecondary: isDark ? 'rgba(199, 164, 52, 0.15)' : 'rgba(184, 145, 45, 0.15)',
      headerBg: isDark ? '#282a30' : '#fafafa',
    },
    Input: {
      colorBgContainer: isDark ? '#16171a' : '#ffffff',
      colorBorder: isDark ? 'rgba(199, 164, 52, 0.2)' : 'rgba(184, 145, 45, 0.3)',
      hoverBorderColor: isDark ? '#c7a434' : '#b8912d',
      activeBorderColor: isDark ? '#c7a434' : '#b8912d',
      activeShadow: isDark ? '0 0 0 2px rgba(199, 164, 52, 0.1)' : '0 0 0 2px rgba(184, 145, 45, 0.1)',
    },
    Typography: {
      colorTextHeading: isDark ? '#c7a434' : '#b8912d',
    }
  },
});

// For backward compatibility during transition
export const tangTheme = getTangTheme(true);
