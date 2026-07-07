# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is the frontend for **HiveMind**, an AI multi-agent collaboration platform. Built with React 19, TypeScript (strict mode), Ant Design v6, and Vite.

The frontend requires a Java/Spring backend running on port 8080 — the Vite dev server proxies `/api` requests there.

## Commands

```bash
npm run dev        # Start Vite dev server with HMR (requires backend on :8080)
npm run build      # Type-check (tsc -b) then produce production build
npm run lint       # Run ESLint
npm run preview    # Preview production build locally
```

No test framework is configured.

## Architecture

**Stack:** React 19 · React Router v7 · Ant Design v6 + @ant-design/x · Zustand · Vite 5 · TypeScript strict

### Layer structure (top-down)

1. **Providers** (`src/providers/`) — `AuthProvider` (JWT auth) and `ThemeProvider` (light/dark) wrap the entire app.
2. **Routing** (`src/App.tsx`) — All routes defined here. Public routes (`/login`, `/register`) sit outside the main layout. Protected routes use `ProtectedRoute` which redirects unauthenticated users.
3. **Layout** (`src/layouts/MainLayout.tsx`) — App shell with collapsible sidebar, header, and content outlet.
4. **Pages** (`src/pages/`) — Organized by domain: `Workspace/` (chat, templates), `TaskCenter/` (kanban, channels, reports), `Admin/` (skills, tools, MCP, agents), `Dàlǐsì/` (models, env vars, security, token usage).
5. **Services** (`src/services/`) — Two HTTP clients coexist:
   - `http.ts` — Modern fetch-based helpers (`getJson`, `postJson`, `putJson`, `postFormData`) with JWT injection. **Use this for new code.**
   - `api.ts` — Legacy Axios client for dashboard endpoints. Do not extend.
6. **State** — Two patterns coexist:
   - `src/store.ts` — Zustand store for legacy dashboard polling.
   - `src/hooks/useAgentConsole.ts` — The primary hook for chat. Manages sessions, SSE streaming, tool approvals, and message state with `requestAnimationFrame`-based batching.

### Chat streaming flow

`agentConsoleApi.streamChat()` opens an SSE connection. `useAgentConsole` processes event types: `MESSAGE`, `REASONING`, `TOOL_RESULT`, `RESULT`, `APPROVAL_REQUIRED`, `ERROR`, `DONE`. Messages are batched into renders via `requestAnimationFrame`. Tool approval requests surface through `ApprovalDrawerPanel`.

### Theming

HiveMind color scheme: crimson (primary) + gold (highlight). CSS custom properties in `src/styles/theme.css` (`--td-*` variables) drive light/dark mode. Ant Design tokens configured in `src/theme.ts`. Theme toggled via `data-theme` attribute on `<html>`.

### Authentication

JWT stored in localStorage via `authStorage.ts`. All API calls inject the token. 401 responses trigger automatic logout and redirect to `/login`.

### Backend API contract

All endpoints use `/api/v1` prefix. Key areas: auth (`/auth/*`), agent chat streaming (`/agent/console/*`), tool approvals, skills, profiles, token usage. The backend is a separate Java/Spring project in the parent `HiveMind` repository.

## Env Configuration

Copy `.env.example` to `.env`. Key variable: `VITE_BACKEND_API_ROOT` (defaults to `/api/v1`).
