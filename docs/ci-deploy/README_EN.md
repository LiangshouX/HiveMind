# HiveMind CI/CD & Deployment Guide

> Complete guide for building, testing, and deploying HiveMind using Docker and GitHub Actions.

[中文版本](./README.md)

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Prerequisites](#2-prerequisites)
3. [Quick Start (Docker Compose)](#3-quick-start-docker-compose)
4. [Configuration Reference](#4-configuration-reference)
5. [GitHub Actions CI/CD](#5-github-actions-cicd)
6. [Building Docker Images Manually](#6-building-docker-images-manually)
7. [Production Deployment](#7-production-deployment)
8. [Troubleshooting](#8-troubleshooting)

---

## 1. Architecture Overview

HiveMind consists of 5 services:

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Nginx     │────▶│  Java App   │────▶│   MySQL     │
│  (Frontend) │     │  (Spring    │     │   8.0       │
│   :80       │     │   Boot)     │     │   :3306     │
└─────────────┘     │   :8080     │     └─────────────┘
                    │             │
                    │             │────▶┌─────────────┐
                    │             │     │  MongoDB    │
                    │             │     │   7.0       │
                    │             │     │   :27017    │
                    │             │     └─────────────┘
                    │             │
                    │             │────▶┌─────────────┐
                    └─────────────┘     │  ReMe       │
                                        │  Server     │
                                        │  (Python)   │
                                        │   :8002     │
                                        └─────────────┘
```

| Service | Technology | Port | Docker Image |
|---------|-----------|------|--------------|
| **web** | Nginx + React | 80 | `hivemind-web` |
| **hivemind** | Java 17 + Spring Boot 4.0.3 | 8080 | `hivemind-app` |
| **mysql** | MySQL 8.0 | 3306 | `mysql:8.0` |
| **mongodb** | MongoDB 7.0 | 27017 | `mongo:7.0` |
| **reme-server** | Python 3.11 + FastAPI | 8002 | `hivemind-reme` |

---

## 2. Prerequisites

### For Docker Deployment (Recommended)

- **Docker** 20.10+ (with Docker Compose V2)
- **Git** for cloning the repository
- An **LLM API Key** (DashScope / DeepSeek / OpenAI compatible)

### For Local Development

- **JDK 17+** (Eclipse Temurin recommended)
- **Maven** 3.9+
- **Node.js** 20+ (with npm)
- **Python** 3.11+ (for ReMe server)
- **MySQL** 8.0+
- **MongoDB** 6.0+

---

## 3. Quick Start (Docker Compose)

### 3.1 Clone the Repository

```bash
git clone https://github.com/LiangshouX/HiveMind.git
cd HiveMind
```

### 3.2 Configure Environment Variables

```bash
cp .env.example .env
```

Edit `.env` and fill in your API key:

```bash
# Required: Your LLM API Key
AI_DASHSCOPE_API_KEY=sk-your-api-key-here

# Required for ReMe memory server
FLOW_LLM_API_KEY=sk-your-api-key-here
FLOW_LLM_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
```

### 3.3 Start All Services

```bash
# Build and start all services
docker compose up -d

# View logs
docker compose logs -f

# View logs for a specific service
docker compose logs -f hivemind
```

### 3.4 Access the Application

| Service | URL |
|---------|-----|
| **Frontend** | http://localhost |
| **Backend API** | http://localhost:8080 |
| **Swagger UI** | http://localhost:8080/swagger-ui.html |
| **ReMe Server** | http://localhost:8002 |

### 3.5 First-Time Setup

1. Open http://localhost in your browser
2. Register a new account
3. Configure your LLM model in the Admin panel (or use the default)
4. Start chatting with the AI agent!

### 3.6 Stop Services

```bash
# Stop all services
docker compose down

# Stop and remove volumes (WARNING: deletes all data)
docker compose down -v
```

---

## 4. Configuration Reference

### 4.1 Environment Variables

All configurable environment variables are defined in `.env.example`. Here's the complete reference:

#### Database

| Variable | Default | Description |
|----------|---------|-------------|
| `MYSQL_ROOT_PASSWORD` | `hivemind123` | MySQL root password |
| `SPRING_DATASOURCE_URL` | (auto-set in compose) | MySQL JDBC URL |
| `MONGODB_URI` | (auto-set in compose) | MongoDB connection URI |

#### Security

| Variable | Default | Description |
|----------|---------|-------------|
| `JWT_SECRET` | `HiveMindJwtSecretKey2026...` | JWT signing secret (change in production!) |

#### LLM Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `AI_DASHSCOPE_API_KEY` | (empty) | LLM API key (required) |
| `TDAGENT_MODEL_PROVIDER` | `dashscope` | Provider: `dashscope` / `deepseek` / `openai` |
| `TDAGENT_MODEL_NAME` | `qwen-max` | Model name for display |
| `TDAGENT_MODEL_ID` | `qwen3-max` | Model ID for API calls |
| `TDAGENT_MODEL_BASE_URL` | `https://dashscope.aliyuncs.com` | API base URL |

#### ReMe Memory Server

| Variable | Default | Description |
|----------|---------|-------------|
| `FLOW_LLM_API_KEY` | (required) | API key for ReMe's LLM operations |
| `FLOW_LLM_BASE_URL` | `https://dashscope.aliyuncs.com/compatible-mode/v1` | LLM API URL |
| `FLOW_EMBEDDING_API_KEY` | (same as LLM) | Embedding model API key |
| `FLOW_EMBEDDING_BASE_URL` | (same as LLM) | Embedding model URL |

### 4.2 Configuration Files

| File | Purpose |
|------|---------|
| `application.yaml` | Main Spring Boot config (port, profiles) |
| `application-backend.yaml` | MySQL, MyBatis-Plus, JWT, Swagger |
| `application-agentic.yaml` | Agent engine: model, sandbox, tool-guard, ReMe, compaction |
| `reme-server/.env` | ReMe server environment variables |
| `provider/builtin_provider.json` | LLM provider definitions |

### 4.3 Supported LLM Providers

| Provider | Provider ID | Example Models | Base URL |
|----------|------------|----------------|----------|
| Alibaba DashScope | `dashscope` | qwen-max, qwen-plus, qwen-turbo | `https://dashscope.aliyuncs.com` |
| DeepSeek | `deepseek` | deepseek-chat, deepseek-reasoner | `https://api.deepseek.com` |
| OpenAI Compatible | `openai` | gpt-4o, gpt-4o-mini | `https://api.openai.com` |

---

## 5. GitHub Actions CI/CD

### 5.1 CI Pipeline (`.github/workflows/ci.yml`)

Triggered on every push to `main`/`develop` and pull requests to `main`.

**Jobs:**

1. **Java Build & Test** - Builds Maven project, runs all tests with MySQL + MongoDB services
2. **Frontend Build & Lint** - Installs npm dependencies, runs ESLint, builds TypeScript + Vite
3. **Docker Build Verify** - Builds all 3 Docker images to verify Dockerfiles work

### 5.2 Release Pipeline (`.github/workflows/release.yml`)

Triggered when a version tag is pushed:

```bash
# Create and push a release tag
git tag v1.0.0
git push origin v1.0.0
```

This will:
1. Build all 3 Docker images
2. Push to GitHub Container Registry (GHCR) with version tag + `latest`

**Published Images:**
- `ghcr.io/liangshoux/hivemind-app:latest`
- `ghcr.io/liangshoux/hivemind-web:latest`
- `ghcr.io/liangshoux/hivemind-reme:latest`

### 5.3 Using Pre-built Images

After a release, users can run without building:

```bash
# Download the production compose file
curl -O https://raw.githubusercontent.com/LiangshouX/HiveMind/main/docker-compose.prod.yml
curl -O https://raw.githubusercontent.com/LiangshouX/HiveMind/main/.env.example

# Configure
cp .env.example .env
# Edit .env with your API keys

# Run with pre-built images
docker compose -f docker-compose.prod.yml up -d
```

---

## 6. Building Docker Images Manually

### 6.1 Java Backend

```bash
# From project root
docker build -t hivemind-app:latest .
```

The Dockerfile uses multi-stage build:
- **Stage 1**: Maven builds the fat JAR
- **Stage 2**: JRE Alpine runs the application

### 6.2 Frontend

```bash
# From website/ directory
docker build -t hivemind-web:latest ./website
```

The Dockerfile uses multi-stage build:
- **Stage 1**: Node.js builds the React app
- **Stage 2**: Nginx serves static files + reverse proxy

### 6.3 ReMe Server

```bash
# From reme-server/ directory
docker build -t hivemind-reme:latest -f docker/Dockerfile ./reme-server
```

---

## 7. Production Deployment

### 7.1 Security Checklist

- [ ] Change `JWT_SECRET` to a strong random string
- [ ] Change `MYSQL_ROOT_PASSWORD` to a strong password
- [ ] Use HTTPS (add SSL termination via Nginx or reverse proxy)
- [ ] Restrict database ports (don't expose 3306/27017 publicly)
- [ ] Use Docker secrets or a vault for API keys
- [ ] Set `TDAGENT_SANDBOX_ENABLED=false` unless Docker-in-Docker is configured

### 7.2 Resource Requirements

| Service | Minimum RAM | Recommended RAM | CPU |
|---------|------------|-----------------|-----|
| Java App | 512MB | 1-2GB | 2 cores |
| MySQL | 256MB | 512MB | 1 core |
| MongoDB | 256MB | 512MB | 1 core |
| ReMe Server | 256MB | 512MB | 1 core |
| Nginx | 64MB | 128MB | 1 core |
| **Total** | **~1.3GB** | **~2.7GB** | **6 cores** |

### 7.3 Volume Mounts for Data Persistence

The Docker Compose configuration uses named volumes:

| Volume | Purpose |
|--------|---------|
| `mysql-data` | MySQL database files |
| `mongo-data` | MongoDB database files |
| `reme-workspace` | ReMe memory data |

To use host directories instead (recommended for production):

```yaml
volumes:
  - /data/hivemind/mysql:/var/lib/mysql
  - /data/hivemind/mongodb:/data/db
  - /data/hivemind/reme:/workspace
```

### 7.4 Reverse Proxy (HTTPS)

For production, place HiveMind behind a reverse proxy with SSL:

```nginx
# /etc/nginx/sites-available/hivemind
server {
    listen 443 ssl;
    server_name hivemind.example.com;

    ssl_certificate /etc/letsencrypt/live/hivemind.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/hivemind.example.com/privkey.pem;

    location / {
        proxy_pass http://localhost:80;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # WebSocket / SSE support
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
        proxy_buffering off;
        proxy_read_timeout 300s;
    }
}
```

---

## 8. Troubleshooting

### 8.1 Common Issues

#### MySQL fails to start

```bash
# Check logs
docker compose logs mysql

# Common fix: remove old data volume
docker compose down -v
docker compose up -d
```

#### Java app fails to connect to MySQL

Ensure MySQL is healthy before the app starts:
```bash
docker compose ps  # Check mysql health status
docker compose logs hivemind  # Check app logs
```

#### ReMe server crashes on startup

```bash
# Check if API key is set correctly
docker compose logs reme-server

# Common issue: FLOW_LLM_API_KEY not set or invalid
```

#### Frontend shows "Network Error"

The Nginx container proxies to `hivemind:8080`. Ensure:
1. The `hivemind` service is running and healthy
2. The API calls go to `/api/` path

### 8.2 Useful Commands

```bash
# View all service status
docker compose ps

# Restart a specific service
docker compose restart hivemind

# Enter a container for debugging
docker compose exec hivemind sh
docker compose exec mysql mysql -u root -p

# View resource usage
docker stats

# Clean up everything
docker compose down -v --rmi all
```

### 8.3 Logs

```bash
# All services
docker compose logs -f

# Specific service
docker compose logs -f hivemind

# Last 100 lines
docker compose logs --tail=100 hivemind
```

---

## Appendix: File Structure

```
HiveMind/
├── .github/
│   └── workflows/
│       ├── ci.yml              # CI pipeline (test + build)
│       └── release.yml         # Release pipeline (Docker push)
├── .env.example                # Environment variables template
├── .dockerignore               # Docker build context exclusions
├── Dockerfile                  # Java backend multi-stage build
├── docker-compose.yml          # Full stack (build from source)
├── docker-compose.prod.yml     # Full stack (pre-built images)
├── docs/
│   └── ci-deploy/
│       ├── README.md           # Chinese version (中文版)
│       └── README_EN.md        # This file
├── hivemind-agent-engine/      # Agent engine module
├── hivemind-backend/           # Business backend module
├── hivemind-launcher/          # Spring Boot entry point
├── reme-server/
│   ├── docker/
│   │   ├── Dockerfile          # ReMe server build
│   │   └── docker-compose.yml  # ReMe standalone compose
│   └── .dockerignore           # ReMe build exclusions
├── website/
│   ├── Dockerfile              # Frontend multi-stage build
│   └── nginx.conf              # Nginx configuration
└── pom.xml                     # Maven parent POM
```
