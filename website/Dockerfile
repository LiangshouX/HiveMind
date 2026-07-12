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

# Build argument for API root (can be overridden at build time)
ARG VITE_BACKEND_API_ROOT=/api/v1
ENV VITE_BACKEND_API_ROOT=${VITE_BACKEND_API_ROOT}

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
