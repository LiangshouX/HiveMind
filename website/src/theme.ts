import { theme, type ThemeConfig } from 'antd';

/**
 * HiveMind 统一主题。
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li><b>单一来源</b>：全站只有这一份主题配置，页面级不再嵌套自己的 ConfigProvider，
 *       避免出现"三套调色板"导致的明暗主题互相突兀。</li>
 *   <li><b>基于算法派生</b>：使用 antd 的 {@code darkAlgorithm} / {@code defaultAlgorithm}
 *       作为基座，让 antd 自动派生的 token（colorFillAlter、colorTextDescription、
 *       colorBgLayout、colorSplit 等）跟随明暗模式，而不是沿用亮色算法的半透明黑，
 *       那正是暗色主题下"深灰字压深色底"看不清的根因。</li>
 *   <li><b>对比度可控</b>：所有文本层级都显式给出明暗两套取值，
 *       正文 ≥ 4.5:1、大标题 ≥ 3:1，保证两套主题都可读。</li>
 * </ul>
 */
export const getHiveMindTheme = (isDark: boolean = true): ThemeConfig => {
  const algorithm = isDark ? theme.darkAlgorithm : theme.defaultAlgorithm;

  const token = isDark
    ? {
        // ---- 主色：绯红 ----
        colorPrimary: '#b32934',
        colorPrimaryBg: 'rgba(179, 41, 52, 0.18)',
        colorPrimaryBgHover: 'rgba(179, 41, 52, 0.26)',
        colorPrimaryBorder: 'rgba(179, 41, 52, 0.45)',
        colorPrimaryBorderHover: '#d1323f',
        colorPrimaryText: '#e06872',
        colorPrimaryTextHover: '#ec7f88',
        colorPrimaryTextActive: '#c9545e',

        // ---- 功能色：琉璃黄 ----
        colorInfo: '#c7a434',
        colorInfoBg: 'rgba(199, 164, 52, 0.14)',
        colorInfoBorder: 'rgba(199, 164, 52, 0.4)',
        colorLink: '#d9b64a',
        colorLinkHover: '#e8cd74',
        colorLinkActive: '#b8912d',

        // ---- 语义色 ----
        colorSuccess: '#49aa19',
        colorSuccessBg: 'rgba(73, 170, 25, 0.15)',
        colorSuccessBorder: 'rgba(73, 170, 25, 0.4)',
        colorWarning: '#d4a017',
        colorWarningBg: 'rgba(212, 160, 23, 0.15)',
        colorWarningBorder: 'rgba(212, 160, 23, 0.4)',
        colorError: '#e05252',
        colorErrorBg: 'rgba(224, 82, 82, 0.15)',
        colorErrorBorder: 'rgba(224, 82, 82, 0.4)',

        // ---- 背景层级 ----
        colorBgBase: '#16171a',
        colorBgLayout: '#16171a',
        colorBgContainer: '#1e1f24',
        colorBgElevated: '#282a30',
        colorBgSpotlight: '#33353c',
        colorBgMask: 'rgba(0, 0, 0, 0.65)',
        colorBgTextHover: 'rgba(255, 255, 255, 0.08)',
        colorBgTextActive: 'rgba(255, 255, 255, 0.14)',

        // ---- 文本层级 ----
        colorTextBase: '#ececec',
        colorText: '#ececec',
        colorTextSecondary: '#b6b8be',
        colorTextTertiary: '#93959c',
        colorTextQuaternary: '#7a7c83',
        colorTextDescription: '#b6b8be',
        colorTextHeading: '#c7a434',
        colorTextLabel: '#b6b8be',
        colorTextPlaceholder: '#707279',
        colorTextDisabled: '#55575d',
        colorTextLightSolid: '#16171a',
        colorIcon: '#b6b8be',
        colorIconHover: '#ececec',

        // ---- 填充层级（暗色下必须用白色半透明，用黑色会糊成一片）----
        colorFill: 'rgba(255, 255, 255, 0.1)',
        colorFillSecondary: 'rgba(255, 255, 255, 0.08)',
        colorFillTertiary: 'rgba(255, 255, 255, 0.06)',
        colorFillQuaternary: 'rgba(255, 255, 255, 0.04)',
        colorFillAlter: 'rgba(255, 255, 255, 0.06)',
        colorFillContent: 'rgba(255, 255, 255, 0.12)',

        // ---- 边框 ----
        colorBorder: 'rgba(199, 164, 52, 0.22)',
        colorBorderSecondary: 'rgba(199, 164, 52, 0.12)',
        colorSplit: 'rgba(255, 255, 255, 0.08)',

        boxShadow: '0 4px 12px rgba(0, 0, 0, 0.4)',
        boxShadowSecondary: '0 8px 24px rgba(0, 0, 0, 0.45)',
        boxShadowTertiary: '0 2px 8px rgba(0, 0, 0, 0.35)',
      }
    : {
        // ---- 主色：绯红 ----
        colorPrimary: '#a3222e',
        colorPrimaryBg: 'rgba(163, 34, 46, 0.1)',
        colorPrimaryBgHover: 'rgba(163, 34, 46, 0.16)',
        colorPrimaryBorder: 'rgba(163, 34, 46, 0.4)',
        colorPrimaryBorderHover: '#851a24',
        colorPrimaryText: '#a3222e',
        colorPrimaryTextHover: '#851a24',
        colorPrimaryTextActive: '#66141c',

        // ---- 功能色：琉璃黄 ----
        colorInfo: '#b8912d',
        colorInfoBg: 'rgba(184, 145, 45, 0.12)',
        colorInfoBorder: 'rgba(184, 145, 45, 0.4)',
        colorLink: '#8a6d18',
        colorLinkHover: '#6f5711',
        colorLinkActive: '#5b470d',

        // ---- 语义色 ----
        colorSuccess: '#389e0d',
        colorSuccessBg: 'rgba(56, 158, 13, 0.1)',
        colorSuccessBorder: 'rgba(56, 158, 13, 0.35)',
        colorWarning: '#b87a00',
        colorWarningBg: 'rgba(184, 122, 0, 0.1)',
        colorWarningBorder: 'rgba(184, 122, 0, 0.35)',
        colorError: '#cf1322',
        colorErrorBg: 'rgba(207, 19, 34, 0.08)',
        colorErrorBorder: 'rgba(207, 19, 34, 0.3)',

        // ---- 背景层级 ----
        colorBgBase: '#f0f2f5',
        colorBgLayout: '#f0f2f5',
        colorBgContainer: '#ffffff',
        colorBgElevated: '#ffffff',
        colorBgSpotlight: '#33353c',
        colorBgMask: 'rgba(0, 0, 0, 0.45)',
        colorBgTextHover: 'rgba(31, 32, 36, 0.04)',
        colorBgTextActive: 'rgba(31, 32, 36, 0.08)',

        // ---- 文本层级 ----
        colorTextBase: '#1f2024',
        colorText: '#1f2024',
        colorTextSecondary: '#52555c',
        colorTextTertiary: '#6e7178',
        colorTextQuaternary: '#9a9da3',
        colorTextDescription: '#52555c',
        // 亮色下用更深的琉璃黄，避免金黄压白底看不清
        colorTextHeading: '#8a6d18',
        colorTextLabel: '#52555c',
        colorTextPlaceholder: '#9a9da3',
        colorTextDisabled: '#b4b7bc',
        colorTextLightSolid: '#ffffff',
        colorIcon: '#52555c',
        colorIconHover: '#1f2024',

        // ---- 填充层级（亮色下用黑色半透明）----
        colorFill: 'rgba(31, 32, 36, 0.06)',
        colorFillSecondary: 'rgba(31, 32, 36, 0.04)',
        colorFillTertiary: 'rgba(31, 32, 36, 0.03)',
        colorFillQuaternary: 'rgba(31, 32, 36, 0.02)',
        colorFillAlter: 'rgba(31, 32, 36, 0.04)',
        colorFillContent: 'rgba(31, 32, 36, 0.08)',

        // ---- 边框 ----
        colorBorder: 'rgba(184, 145, 45, 0.32)',
        colorBorderSecondary: 'rgba(184, 145, 45, 0.16)',
        colorSplit: 'rgba(31, 32, 36, 0.08)',

        boxShadow: '0 4px 12px rgba(0, 0, 0, 0.06)',
        boxShadowSecondary: '0 8px 24px rgba(0, 0, 0, 0.1)',
        boxShadowTertiary: '0 2px 8px rgba(0, 0, 0, 0.05)',
      };

  return {
    algorithm,
    token: {
      ...token,
      borderRadius: 8,
      borderRadiusLG: 12,
      borderRadiusSM: 6,
      borderRadiusXS: 4,
      fontFamily:
        '"Noto Sans SC", "PingFang SC", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif',
      fontFamilyCode:
        '"Fira Code", "JetBrains Mono", source-code-pro, Menlo, Monaco, Consolas, "Courier New", monospace',
      wireframe: false,
    },
    components: {
      Layout: {
        siderBg: isDark ? '#16171a' : '#ffffff',
        headerBg: isDark ? 'rgba(22, 23, 26, 0.85)' : 'rgba(240, 242, 245, 0.9)',
        bodyBg: isDark ? '#16171a' : '#f0f2f5',
        footerBg: isDark ? '#16171a' : '#f0f2f5',
        triggerBg: isDark ? '#282a30' : '#ffffff',
      },
      Menu: {
        itemBg: 'transparent',
        itemColor: isDark ? '#b6b8be' : '#52555c',
        itemHoverColor: isDark ? '#e0bb3f' : '#8a6d18',
        itemSelectedColor: isDark ? '#e0bb3f' : '#a3222e',
        itemSelectedBg: isDark ? 'rgba(179, 41, 52, 0.18)' : 'rgba(163, 34, 46, 0.1)',
        itemHoverBg: isDark ? 'rgba(199, 164, 52, 0.1)' : 'rgba(184, 145, 45, 0.1)',
        itemBorderRadius: 6,
        subMenuItemBg: 'transparent',
        groupTitleColor: isDark ? '#93959c' : '#6e7178',
        iconSize: 14,
        popupBg: isDark ? '#282a30' : '#ffffff',
      },
      Button: {
        colorPrimary: isDark ? '#b32934' : '#a3222e',
        colorPrimaryHover: isDark ? '#d1323f' : '#851a24',
        colorPrimaryActive: isDark ? '#8f212a' : '#66141c',
        primaryShadow: isDark ? '0 4px 12px rgba(179, 41, 52, 0.3)' : '0 4px 12px rgba(163, 34, 46, 0.2)',
        defaultBg: isDark ? '#282a30' : '#ffffff',
        defaultColor: isDark ? '#d9b64a' : '#8a6d18',
        defaultBorderColor: isDark ? 'rgba(199, 164, 52, 0.28)' : 'rgba(184, 145, 45, 0.4)',
        defaultHoverColor: isDark ? '#e8cd74' : '#6f5711',
        defaultHoverBg: isDark ? '#33353c' : '#faf7ef',
        defaultHoverBorderColor: isDark ? '#c7a434' : '#b8912d',
        defaultActiveColor: isDark ? '#c7a434' : '#5b470d',
        defaultActiveBorderColor: isDark ? '#b8912d' : '#8a6d18',
        defaultGhostColor: isDark ? '#b6b8be' : '#52555c',
      },
      Card: {
        colorBgContainer: isDark ? '#1e1f24' : '#ffffff',
        colorBorderSecondary: isDark ? 'rgba(199, 164, 52, 0.16)' : 'rgba(184, 145, 45, 0.18)',
        headerBg: isDark ? '#282a30' : '#fafafa',
        headerFontSize: 16,
      },
      Input: {
        colorBgContainer: isDark ? '#1e1f24' : '#ffffff',
        colorBorder: isDark ? 'rgba(199, 164, 52, 0.22)' : 'rgba(184, 145, 45, 0.38)',
        hoverBorderColor: isDark ? '#c7a434' : '#b8912d',
        activeBorderColor: isDark ? '#c7a434' : '#a3222e',
        activeShadow: isDark ? '0 0 0 2px rgba(199, 164, 52, 0.12)' : '0 0 0 2px rgba(163, 34, 46, 0.1)',
        colorText: isDark ? '#ececec' : '#1f2024',
        colorTextPlaceholder: isDark ? '#707279' : '#9a9da3',
      },
      InputNumber: {
        colorBgContainer: isDark ? '#1e1f24' : '#ffffff',
        activeBorderColor: isDark ? '#c7a434' : '#a3222e',
        hoverBorderColor: isDark ? '#c7a434' : '#b8912d',
      },
      Typography: {
        colorTextHeading: isDark ? '#c7a434' : '#8a6d18',
        colorTextSecondary: isDark ? '#b6b8be' : '#52555c',
        colorTextTertiary: isDark ? '#93959c' : '#6e7178',
      },
      Tag: {
        defaultBg: isDark ? 'rgba(255, 255, 255, 0.08)' : 'rgba(31, 32, 36, 0.05)',
        defaultColor: isDark ? '#c6c8ce' : '#4a4d54',
        colorTextDescription: isDark ? '#b6b8be' : '#52555c',
      },
      Table: {
        headerBg: isDark ? '#282a30' : '#fafafa',
        headerColor: isDark ? '#ececec' : '#1f2024',
        headerSplitColor: isDark ? 'rgba(255, 255, 255, 0.08)' : 'rgba(31, 32, 36, 0.08)',
        borderColor: isDark ? 'rgba(255, 255, 255, 0.08)' : 'rgba(31, 32, 36, 0.1)',
        rowHoverBg: isDark ? 'rgba(255, 255, 255, 0.04)' : 'rgba(31, 32, 36, 0.03)',
        colorBgContainer: isDark ? '#1e1f24' : '#ffffff',
        colorText: isDark ? '#ececec' : '#1f2024',
      },
      Modal: {
        contentBg: isDark ? '#282a30' : '#ffffff',
        headerBg: isDark ? '#282a30' : '#ffffff',
        titleColor: isDark ? '#ececec' : '#1f2024',
      },
      Drawer: {
        colorBgElevated: isDark ? '#1e1f24' : '#ffffff',
      },
      Dropdown: {
        colorBgElevated: isDark ? '#282a30' : '#ffffff',
      },
      Empty: {
        colorTextDescription: isDark ? '#a8aab0' : '#5c5f66',
        colorTextDisabled: isDark ? '#55575d' : '#b4b7bc',
      },
      Alert: {
        colorTextDescription: isDark ? '#b6b8be' : '#52555c',
        colorBgContainer: isDark ? 'rgba(255, 255, 255, 0.04)' : '#ffffff',
      },
      Collapse: {
        headerBg: 'transparent',
        contentBg: isDark ? '#1e1f24' : '#ffffff',
      },
      List: {
        colorBgContainer: 'transparent',
      },
      Tooltip: {
        colorBgSpotlight: '#33353c',
        colorTextLightSolid: '#f2f2f2',
      },
      Popover: {
        colorBgElevated: isDark ? '#282a30' : '#ffffff',
      },
      Descriptions: {
        labelBg: isDark ? '#282a30' : '#fafafa',
      },
      Segmented: {
        trackBg: isDark ? '#282a30' : '#f2f3f5',
        itemSelectedBg: isDark ? '#3a3d45' : '#ffffff',
        itemSelectedColor: isDark ? '#ececec' : '#1f2024',
        itemColor: isDark ? '#b6b8be' : '#52555c',
      },
      Progress: {
        defaultColor: isDark ? '#c7a434' : '#a3222e',
        remainingColor: isDark ? 'rgba(255, 255, 255, 0.12)' : 'rgba(31, 32, 36, 0.1)',
      },
      Badge: {
        colorText: isDark ? '#1e1f24' : '#ffffff',
      },
      Checkbox: {
        colorBgContainer: isDark ? '#1e1f24' : '#ffffff',
      },
      Switch: {
        colorBgContainer: isDark ? '#33353c' : '#ffffff',
      },
      Select: {
        colorBgContainer: isDark ? '#1e1f24' : '#ffffff',
        optionSelectedBg: isDark ? 'rgba(255, 255, 255, 0.08)' : 'rgba(31, 32, 36, 0.05)',
      },
      Form: {
        labelColor: isDark ? '#b6b8be' : '#52555c',
        labelRequiredMarkColor: isDark ? '#e06872' : '#a3222e',
        itemMarginBottom: 20,
      },
    },
  };
};

// For backward compatibility during transition
export const hivemindTheme = getHiveMindTheme(true);
