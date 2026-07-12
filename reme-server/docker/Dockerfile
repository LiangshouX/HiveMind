# ============================================================
# ReMe MCP Server Docker Image
# Python 3.11-slim, supports MCP SSE transport
# ============================================================

FROM python:3.11-slim

LABEL maintainer="LiangshouX"
LABEL description="HiveMind ReMe - Long-term Memory Server"

WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Copy dependency files first for layer caching
COPY requirements.txt .

# Install Python dependencies
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY scripts/ scripts/
COPY README.md .

# Create data directories
RUN mkdir -p /workspace/session /workspace/daily /workspace/digest /workspace/metadata /workspace/resource /workspace/mem_session /workspace/user-001

# Expose port
EXPOSE 8002

# Environment variable defaults
ENV HTTP_PORT=8002
ENV LOG_LEVEL=INFO
ENV REME_BACKEND=mcp
ENV MCP_TRANSPORT=sse

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=40s \
    CMD curl -sf http://localhost:8002/ || exit 1

# Use shell form so environment variables get expanded
CMD reme start service.backend=mcp mcp.transport=${MCP_TRANSPORT} log.level=${LOG_LEVEL}
