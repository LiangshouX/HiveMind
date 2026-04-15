"""
ReMe Server - 主应用入口

支持以下认证方式：
1. API Key 认证（通过 Authorization: Bearer <api_key> 头）
2. 无认证模式（用于本地开发）

环境变量：
- REME_API_KEY: API 密钥（可选，不设置则禁用认证）
- REME_WORKING_DIR: ReMe 工作目录
- REME_LLM_BACKEND: LLM 后端类型
- REME_LLM_MODEL_NAME: LLM 模型名称
- REME_EMBEDDING_BACKEND: Embedding 后端类型
- REME_EMBEDDING_MODEL_NAME: Embedding 模型名称
- REME_EMBEDDING_DIMENSIONS: Embedding 维度
- REME_VECTOR_BACKEND: 向量后端类型 (chroma/local)
- REME_CHROMA_HOST: Chroma 主机
- REME_CHROMA_PORT: Chroma 端口
- REME_CHROMA_COLLECTION: Chroma 集合名称
"""
from __future__ import annotations

import os
from contextlib import asynccontextmanager
from typing import AsyncGenerator

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from .router import router
from .service import ReMeServerConfig, ReMeService


# =============================================================================
# 认证配置
# =============================================================================


API_KEY = os.getenv("REME_API_KEY")


async def verify_auth(request: Request, call_next):
    """
    认证中间件
    
    如果设置了 REME_API_KEY，则验证 Authorization 头
    如果未设置 REME_API_KEY，则跳过认证（允许所有请求）
    """
    # 如果未配置 API Key，跳过认证
    if not API_KEY:
        return await call_next(request)

    # 跳过健康检查和心跳检测
    if request.url.path in ["/healthz", "/heartbeat"]:
        return await call_next(request)

    # 验证 Authorization 头
    auth_header = request.headers.get("Authorization")
    if not auth_header:
        return JSONResponse(
            status_code=401,
            content={"detail": "Missing Authorization header"},
        )

    # 支持两种格式：
    # 1. Authorization: Bearer <api_key>
    # 2. Authorization: <api_key>
    auth_value = auth_header
    if auth_header.startswith("Bearer "):
        auth_value = auth_header[7:]

    if auth_value != API_KEY:
        return JSONResponse(
            status_code=403,
            content={"detail": "Invalid API key"},
        )

    return await call_next(request)


# =============================================================================
# 应用生命周期
# =============================================================================


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncGenerator[None, None]:
    """
    应用生命周期管理
    
    启动时初始化 ReMe 服务
    关闭时清理 ReMe 服务
    """
    # 启动
    config = ReMeServerConfig.from_env()
    service = ReMeService(config)
    await service.start()
    app.state.reme_service = service

    print(f"ReMe Server started successfully")
    print(f"  - Working Directory: {config.working_dir}")
    print(f"  - Vector Backend: {config.preferred_vector_backend}")
    print(f"  - LLM Backend: {config.llm_backend} / {config.llm_model_name}")
    print(f"  - Embedding Backend: {config.embedding_backend} / {config.embedding_model_name}")
    print(f"  - Chroma: {config.chroma_host}:{config.chroma_port} / {config.chroma_collection}")
    print(f"  - API Key Auth: {'Enabled' if API_KEY else 'Disabled'}")

    yield

    # 关闭
    if hasattr(app.state, "reme_service"):
        await app.state.reme_service.close()
        print("ReMe Server shutdown complete")


# =============================================================================
# FastAPI 应用
# =============================================================================


app = FastAPI(
    title="ReMe Server",
    description="ReMe (Retrieval-enhanced Memory) Server - 长期记忆服务",
    version="1.0.0",
    lifespan=lifespan,
)

# CORS 中间件
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 生产环境应限制具体域名
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 认证中间件
app.middleware("http")(verify_auth)

# 注册路由
app.include_router(router)


# =============================================================================
# 根路径
# =============================================================================


@app.get("/")
async def root() -> dict:
    """根路径 - 返回服务器信息"""
    return {
        "name": "ReMe Server",
        "version": "1.0.0",
        "status": "running",
        "endpoints": {
            "official": [
                "POST /summary_personal_memory",
                "POST /retrieve_personal_memory",
            ],
            "custom": [
                "POST /add_memory",
                "POST /summarize_memory",
                "POST /retrieve_memory",
                "GET /get_memory/{memory_id}",
                "POST /update_memory",
                "POST /list_memory",
                "POST /delete_memory",
                "POST /delete_all",
            ],
            "health": [
                "GET /healthz",
                "GET /heartbeat",
            ],
        },
    }
