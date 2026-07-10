import { useCallback, useEffect, useRef, useState } from "react";

/**
 * 响应式折叠 Hook
 * 当浏览器窗口宽度缩小（如取消最大化、按F12、打开侧边插件）时自动折叠面板。
 * 支持手动展开/折叠，以及基于阈值的自动折叠。
 *
 * @param options.autoCollapseWidth  低于此宽度时自动折叠（默认 1024px）
 * @param options.expandedWidth     展开时的面板宽度（默认 280px）
 * @param options.collapsedWidth    折叠时的面板宽度（默认 0px）
 */
export function useResponsiveCollapse(options?: {
    autoCollapseWidth?: number;
    expandedWidth?: number;
    collapsedWidth?: number;
}) {
    const autoCollapseWidth = options?.autoCollapseWidth ?? 1024;
    const expandedWidth = options?.expandedWidth ?? 280;
    const collapsedWidth = options?.collapsedWidth ?? 0;

    const [collapsed, setCollapsed] = useState(false);
    const [hasAutoCollapsed, setHasAutoCollapsed] = useState(false);
    const prevWidthRef = useRef<number>(window.innerWidth);

    const toggle = useCallback(() => {
        setCollapsed((prev) => {
            const next = !prev;
            if (next) setHasAutoCollapsed(false);
            return next;
        });
    }, []);

    const expand = useCallback(() => {
        setCollapsed(false);
        setHasAutoCollapsed(false);
    }, []);

    const collapse = useCallback(() => {
        setCollapsed(true);
    }, []);

    useEffect(() => {
        const handleResize = () => {
            const currentWidth = window.innerWidth;
            const prevWidth = prevWidthRef.current;
            prevWidthRef.current = currentWidth;

            if (currentWidth < prevWidth && currentWidth < autoCollapseWidth && !collapsed) {
                setCollapsed(true);
                setHasAutoCollapsed(true);
            }
            if (currentWidth > prevWidth && hasAutoCollapsed && currentWidth >= autoCollapseWidth) {
                setCollapsed(false);
                setHasAutoCollapsed(false);
            }
        };

        window.addEventListener("resize", handleResize);
        return () => window.removeEventListener("resize", handleResize);
    }, [collapsed, hasAutoCollapsed, autoCollapseWidth]);

    useEffect(() => {
        if (window.innerWidth < autoCollapseWidth) {
            setCollapsed(true);
            setHasAutoCollapsed(true);
        }
    }, [autoCollapseWidth]);

    const currentWidth = collapsed ? collapsedWidth : expandedWidth;

    return {
        collapsed,
        currentWidth,
        toggle,
        expand,
        collapse,
        hasAutoCollapsed,
    };
}
