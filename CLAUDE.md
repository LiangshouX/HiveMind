# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

TangDynasty is a cloud-based AI assistant platform with a multi-agent collaboration architecture. Different AI agents assume distinct roles — message triage (AGENT_TRIAGE), task planning (AGENT_PLANNER), review (AGENT_REVIEWER), and execution dispatch (AGENT_EXECUTOR) — to collaboratively handle complex tasks through an institutionalized workflow.

## Build & Run Commands

```bash
# Build entire project
mvn clean install

# Run the application (port 8080)
mvn spring-boot:run -pl tang-dynasty-launcher

# Run tests
mvn test

# Run integration tests (requires Docker for Testcontainers)
mvn verify

# Run a single test class
mvn test -pl tang-dynasty-backend -Dtest=SomeClassName

# Run a single test method
mvn test -pl tang-dynasty-backend -Dtest=SomeClassName#methodName
```

## Module Structure

Three Maven modules with dependency flow: `launcher` → `backend` → `agent-engine`

| Module | Role |
|--------|------|
| `tang-dynasty-launcher` | Spring Boot entry point (`TangApplication.java`), aggregates all modules |
| `tang-dynasty-backend` | Business services: user auth, task management, channels, models, MCP config |
| `tang-dynasty-agent-engine` | AI Agent runtime: chat, tools, memory, skills, sandbox, streaming |

## Architecture

DDD-influenced layered architecture. Dependency direction:

```
adapter → application → domain ← infrastructure
                      ↑
                   common
```

**Strict rules**: Domain layer must NOT depend on infrastructure layer. Application layer must NOT directly depend on infrastructure layer. No cross-layer calls (e.g., adapter directly calling infrastructure).

Each module follows the same package layout:
- `adapter/controller/` — REST controllers
- `application/` — service interfaces (`I*Service`) and implementations (`*ServiceImpl`), DTOs (`*Request`, `*Response`)
- `domain/` — business models (`*Document`, `*Model`), domain services, enums, constants
- `infrastructure/` — data access (MongoDB repositories, MyBatis-Plus mappers/POs), external integrations
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

All agent-related config lives in `tang-dynasty-agent-engine/src/main/resources/application-agentic.yaml` under the `tdagent` prefix. Key subsystems:

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
| `tang-dynasty-launcher/src/main/resources/application.yaml` | Main config, port 8080, includes `backend` and `agentic` profiles |
| `tang-dynasty-backend/src/main/resources/application-backend.yaml` | MySQL datasource, MyBatis-Plus, JWT, Swagger |
| `tang-dynasty-agent-engine/src/main/resources/application-agentic.yaml` | Agent engine: model, sandbox, tool-guard, ReMe, compaction, streaming, skills |
| `tang-dynasty-agent-engine/src/main/resources/provider/builtin_provider.json` | LLM provider definitions (DashScope/Qwen, DeepSeek) |

## Testing

- JUnit 5 via Spring Boot Starter Test
- Testcontainers for MySQL integration tests
- Spring Security Test for auth-related tests
- Backend tests: `tang-dynasty-backend/src/test/java/com/liangshou/`
- Agent engine tests: `tang-dynasty-agent-engine/src/test/java/com/liangshou/agentic/`

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

<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read the current plan
<!-- SPECKIT END -->
