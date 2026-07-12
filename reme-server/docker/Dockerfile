# ReMe MCP Server Docker Image
# 基于 Python 3.11-slim，支持 MCP SSE 传输模式

FROM python:3.11-slim

# 设置工作目录
WORKDIR /app

# 安装系统依赖
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 复制依赖文件
COPY requirements.txt .

# 安装 Python 依赖
RUN pip install --no-cache-dir -r requirements.txt

# 复制应用代码
COPY . .

# 创建数据目录
RUN mkdir -p /workspace/session /workspace/daily /workspace/digest /workspace/metadata

# 暴露端口
EXPOSE 8002

# 环境变量默认值
ENV HTTP_PORT=8002
ENV LOG_LEVEL=INFO
ENV REME_BACKEND=mcp
ENV MCP_TRANSPORT=sse

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD curl -f http://localhost:8002/ || exit 1

# 启动命令
CMD ["reme", "start", "service.backend=mcp", "mcp.transport=${MCP_TRANSPORT}", "log.level=${LOG_LEVEL}"]
