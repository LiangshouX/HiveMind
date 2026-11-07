# ============================================================
# HiveMind Frontend - Multi-stage Dockerfile
# Stage 1: Node.js build  |  Stage 2: Nginx serve
# ============================================================

# ---- Stage 1: Build ----
FROM node:20-alpine AS builder

WORKDIR /app

# Copy package files first for dependency caching
COPY package.json package-lock.json* ./

# Install dependencies
RUN npm ci --silent 2>/dev/null || npm install --silent

# Copy source code
COPY . .

# Build arguments
# VITE_BACKEND_API_ROOT: 后端 API 前缀（默认 /api/v1，由 Nginx 反代）
# VITE_API_URL: 跨域部署时的后端绝对地址（同源部署留空）
# VITE_AGENT_API_BASE: Agent 流式接口前缀（默认 /tdagent）
ARG VITE_BACKEND_API_ROOT=/api/v1
ARG VITE_API_URL=""
ARG VITE_AGENT_API_BASE=/tdagent
ENV VITE_BACKEND_API_ROOT=${VITE_BACKEND_API_ROOT}
ENV VITE_API_URL=${VITE_API_URL}
ENV VITE_AGENT_API_BASE=${VITE_AGENT_API_BASE}

# Build the application
RUN npm run build

# ---- Stage 2: Serve with Nginx ----
FROM nginx:alpine

LABEL maintainer="LiangshouX"
LABEL description="HiveMind Frontend - React + Nginx"

# Remove default nginx config
RUN rm /etc/nginx/conf.d/default.conf

# Copy custom nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy built assets from builder
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD wget -qO- http://localhost:80/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
