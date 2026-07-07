# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

HiveMind is a cloud-based AI assistant platform with a multi-agent collaboration architecture. Different AI agents assume distinct roles — message triage (AGENT_TRIAGE), task planning (AGENT_PLANNER), review (AGENT_REVIEWER), and execution dispatch (AGENT_EXECUTOR) — to collaboratively handle complex tasks through an institutionalized workflow.

## Environment Requirements

| Component | Minimum | Recommended |
|-----------|---------|-------------|
| JDK | 17 | 17+ |
| Python | 3.10 | 3.10-3.13 |
| Node.js | 18 | 20+ |
| MySQL | 8.0 | 8.0.36+ |
| MongoDB | 6.0 | 7.0+ |
| Docker | 20.10 | 24.0+ |

## Build & Run Commands

```bash
# Build entire project
mvn clean install

# Run the application (port 8080)
mvn spring-boot:run -pl hivemind-launcher

# Run tests
mvn test

# Run integration tests (requires Docker for Testcontainers)
mvn verify

# Run a single test class
mvn test -pl hivemind-backend -Dtest=SomeClassName

# Run a single test method
mvn test -pl hivemind-backend -Dtest=SomeClassName#methodName
```

## Module Structure

Three Maven modules with dependency flow: `launcher` → `backend` → `agent-engine`

| Module | Role |
|--------|------|
| `hivemind-launcher` | Spring Boot entry point (`HiveMindApplication.java`), aggregates all modules |
| `hivemind-backend` | Business services: user auth, task management, channels, models, MCP config |
| `hivemind-agent-engine` | AI Agent runtime: chat, tools, memory, skills, sandbox, streaming |
| `website` | Frontend console: React 19 + TypeScript + Ant Design v6 + Vite |

## Architecture

DDD-influenced layered architecture. Dependency direction:

```
adapter → application → domain ← infrastructure
                      ↑
                   common
```

**Strict rules**: Domain layer must NOT depend on infrastructure layer. Application layer must NOT directly depend on infrastructure layer. No cross-layer calls (e.g., adapter directly calling infrastructure).

### Agent Engine Package Layout
- `adapter/controller/` — REST controllers
- `application/` — service interfaces (`I*Service`) and implementations (`*ServiceImpl`), DTOs (`*Request`, `*Response`)
- `domain/` — business models (`*Document`, `*Model`), domain services, enums, constants
- `infrastructure/` — data access (MongoDB repositories, MyBatis-Plus mappers/POs), external integrations
- `common/` — shared exceptions, utilities, config

### Backend Module Package Layout
- `adapter/controller/` — REST controllers
- `service/` — service interfaces (`I*Service`) and implementations in `impl/` subdirectory
- `infrastructure/` — data access (MyBatis-Plus mappers/POs), external integrations
- `common/` — shared exceptions, utilities, config

## Key Technology Stack

- **Java 17**, **Spring Boot 4.0.3**
- **AI Agent SDK**: AgentScope-Java 1.0.12 (agentscope-core, agentscope-runtime)
- **Databases**: MySQL 8.0 (via MyBatis-Plus 3.5.15) + MongoDB (Spring Data)
- **Security**: Spring Security + JWT (java-jwt 4.4.0)
- **Caching**: Caffeine 3.1.8
- **Object Storage**: Aliyun OSS SDK 3.17.4
- **API Docs**: SpringDoc OpenAPI (Swagger UI) at `/swagger-ui.html`

## Agent Engine Configuration

All agent-related config lives in `hivemind-agent-engine/src/main/resources/application-agentic.yaml` under the `tdagent` prefix. Key subsystems:

- **Model**: LLM provider selection (`provider-id`, `model-name`), provider definitions in `provider/builtin_provider.json`
- **Tool Guard**: Three-layer safety (deny → guard/approve → allow), config under `tdagent.tool-guard`
- **ReMe**: Long-term memory via separate Python service (`reme-server/`), config under `tdagent.reme`
- **Compaction**: Auto-summarizes conversation history when it exceeds thresholds
- **Skills**: Custom skill definitions, builtin in `classpath:skills/`, configurable via `tdagent.skill`
- **Sandbox**: Browser/filesystem sandbox for agent tool execution

## Agent Roles

The system uses four agent roles for task orchestration:

| Role Code | Responsibility |
|-----------|---------------|
| `AGENT_TRIAGE` | Message triage — routes user messages to appropriate handlers |
| `AGENT_PLANNER` | Task planning — breaks down complex tasks into subtasks |
| `AGENT_REVIEWER` | Review — validates and approves planned tasks |
| `AGENT_EXECUTOR` | Execution — dispatches and executes approved tasks |

## Dual Database Usage

- **MySQL**: Business data (users, tasks, channels, models, scheduled jobs) — mappers in `infrastructure/datasource/mapper/` (backend) and `infrastructure/mysql/mapper/` (agent-engine)
- **MongoDB**: Agent-specific data (conversation memory, session state, skills, tool approvals, task templates) — repositories in `infrastructure/mongo/repository/`

## Configuration Files

| File | Purpose |
|------|---------|
| `hivemind-launcher/src/main/resources/application.yaml` | Main config, port 8080, includes `backend` and `agentic` profiles |
| `hivemind-backend/src/main/resources/application-backend.yaml` | MySQL datasource, MyBatis-Plus, JWT, Swagger |
| `hivemind-agent-engine/src/main/resources/application-agentic.yaml` | Agent engine: model, sandbox, tool-guard, ReMe, compaction, streaming, skills |
| `hivemind-agent-engine/src/main/resources/provider/builtin_provider.json` | LLM provider definitions (DashScope/Qwen, DeepSeek) |

## Testing

- JUnit 5 via Spring Boot Starter Test
- Testcontainers for MySQL integration tests
- Spring Security Test for auth-related tests
- Backend tests: `hivemind-backend/src/test/java/com/liangshou/`
- Agent engine tests: `hivemind-agent-engine/src/test/java/com/liangshou/agentic/`

## External Services

- **ReMe Server** (`reme-server/`): Python FastAPI service for long-term memory retrieval (AgentScope-ReMe), runs on port 8002/8085
- **LLM Providers**: DashScope (Alibaba Cloud, Qwen models), DeepSeek (OpenAI-compatible API)
- **MCP**: Model Context Protocol for extensible tool capabilities
- **AgentScope Studio**: Observability UI at `localhost:5174`

## API & Response Conventions

- All REST endpoints return `Result<T>` (code, message, data wrapper) — never return raw entities.
- Agent engine controllers: `/api/v1/tdagent/` (chat, skills, profiles)
- Backend controllers: `/api/` prefix (e.g., `/api/agent-tasks`, `/api/sys-models`, `/api/scheduled-jobs`), except auth at `/api/v1/auth`
- Dependency injection: prefer constructor injection via Lombok `@RequiredArgsConstructor` over `@Autowired` field injection.
- No linting tools (checkstyle, spotbugs, pmd) are configured — there is no `mvn lint` equivalent.

## Frontend Development

The `website/` module is a React 19 + TypeScript frontend with Ant Design v6.

### Commands
```bash
cd website
npm install        # Install dependencies
npm run dev        # Start Vite dev server with HMR (requires backend on :8080)
npm run build      # Type-check (tsc -b) then produce production build
npm run lint       # Run ESLint
npm run preview    # Preview production build locally
```

### Key Architecture
- **Stack**: React 19, React Router v7, Ant Design v6, Zustand, Vite 5, TypeScript strict
- **HTTP Clients**: Use `http.ts` (modern fetch-based) for new code; `api.ts` (legacy Axios) is read-only
- **State Management**: Zustand store in `src/store.ts` for legacy dashboard; `useAgentConsole.ts` hook for chat
- **Chat Streaming**: SSE-based via `agentConsoleApi.streamChat()`, processes events: MESSAGE, REASONING, TOOL_RESULT, RESULT, APPROVAL_REQUIRED, ERROR, DONE
- **Theming**: HiveMind color scheme (crimson primary + gold highlight), CSS custom properties in `src/styles/theme.css`
- **Auth**: JWT stored in localStorage, 401 responses trigger auto-logout and redirect to `/login`

### Environment Configuration
Copy `.env.example` to `.env`. Key variable: `VITE_BACKEND_API_ROOT` (defaults to `/api/v1`).

<!-- SPECKIT START -->
## Active Feature: Model Configuration Management

**Plan**: `specs/001-model-config-management/plan.md`
**Spec**: `specs/001-model-config-management/spec.md`
**Research**: `specs/001-model-config-management/research.md`
**Data Model**: `specs/001-model-config-management/data-model.md`
**Contracts**: `specs/001-model-config-management/contracts/`
**Quickstart**: `specs/001-model-config-management/quickstart.md`
<!-- SPECKIT END -->
