"""
ReMe Server Router

提供与官方 ReMe API 完全兼容的端点，同时保留自定义端点供向后兼容。
"""
from __future__ import annotations

from typing import Any, Dict

from fastapi import APIRouter, HTTPException, Request

from schema_models import (
    AddMemoryRequest,
    DeleteMemoryRequest,
    ListMemoryRequest,
    RetrieveMemoryRequest,
    RetrievePersonalMemoryRequest,
    RetrievePersonalMemoryResponse,
    SummarizeMemoryRequest,
    SummaryPersonalMemoryRequest,
    SummaryPersonalMemoryResponse,
    UpdateMemoryRequest,
)
from service import ReMeService


router = APIRouter(tags=["reme"])


def _get_service(request: Request) -> ReMeService:
    """获取 ReMe 服务实例"""
    svc = getattr(request.app.state, "reme_service", None)
    if svc is None:
        raise HTTPException(status_code=503, detail="ReMe service is not initialized")
    return svc


def _raise_http(e: Exception) -> None:
    """将异常转换为 HTTP 异常"""
    status = 500
    if isinstance(e, RuntimeError):
        status = 503
    raise HTTPException(status_code=status, detail=str(e)) from e


# =============================================================================
# 官方 API 端点（Java 客户端使用）
# =============================================================================


@router.post("/summary_personal_memory")
async def summary_personal_memory(
    body: SummaryPersonalMemoryRequest,
    request: Request,
) -> SummaryPersonalMemoryResponse:
    """
    官方 API：添加记忆并生成摘要
    
    接收对话轨迹，处理并提取可记忆的信息。
    与 Java 客户端的 ReMeAddRequest 完全兼容。
    """
    svc = _get_service(request)
    try:
        # 将官方格式转换为内部格式
        all_messages = []
        for trajectory in body.trajectories:
            for msg in trajectory.messages:
                all_messages.append(
                    {
                        "role": msg.role,
                        "content": msg.content,
                        "time_created": msg.time_created,
                    }
                )

        # 调用内部服务
        result = await svc.call(
            "summarize_memory",
            messages=all_messages,
            user_name=body.workspace_id,  # 使用 workspace_id 作为 user_name
            task_name=None,
        )

        # 转换为官方响应格式
        return SummaryPersonalMemoryResponse(
            answer=result.get("summary") if isinstance(result, dict) else None,
            success=True,
            metadata={"workspace_id": body.workspace_id},
            memories=[result] if result and not isinstance(result, dict) else None,
        )
    except Exception as e:
        _raise_http(e)


@router.post("/retrieve_personal_memory")
async def retrieve_personal_memory(
    body: RetrievePersonalMemoryRequest,
    request: Request,
) -> RetrievePersonalMemoryResponse:
    """
    官方 API：检索记忆
    
    根据查询字符串从历史记忆中检索相关信息。
    与 Java 客户端的 ReMeSearchRequest 完全兼容。
    """
    svc = _get_service(request)
    try:
        kwargs: Dict[str, Any] = {
            "query": body.query,
            "user_name": body.workspace_id,  # 使用 workspace_id 作为 user_name
            "task_name": None,
        }
        if body.top_k is not None:
            kwargs["limit"] = body.top_k

        result = await svc.call("retrieve_memory", **kwargs)

        # 转换为官方响应格式
        if isinstance(result, dict):
            return RetrievePersonalMemoryResponse(
                answer=result.get("answer"),
                success=True,
                metadata=result.get("metadata"),
                memories=result.get("memories"),
            )
        else:
            return RetrievePersonalMemoryResponse(
                answer=str(result) if result else None,
                success=True,
                metadata={"workspace_id": body.workspace_id},
                memories=[str(result)] if result else None,
            )
    except Exception as e:
        _raise_http(e)


# =============================================================================
# 自定义 API 端点（保留向后兼容）
# =============================================================================


@router.get("/healthz")
async def healthz(request: Request) -> Dict[str, Any]:
    """健康检查"""
    svc = _get_service(request)
    hb = await svc.heartbeat()
    return {"ok": True, "reme": hb}


@router.get("/heartbeat")
async def heartbeat(request: Request) -> Dict[str, Any]:
    """心跳检测"""
    svc = _get_service(request)
    return await svc.heartbeat()


@router.post("/summarize_memory")
async def summarize_memory(
    body: SummarizeMemoryRequest,
    request: Request,
) -> Any:
    """自定义 API：摘要记忆"""
    svc = _get_service(request)
    try:
        return await svc.call(
            "summarize_memory",
            messages=body.messages,
            user_name=body.user_name,
            task_name=body.task_name,
        )
    except Exception as e:
        _raise_http(e)


@router.post("/retrieve_memory")
async def retrieve_memory(
    body: RetrieveMemoryRequest,
    request: Request,
) -> Any:
    """自定义 API：检索记忆"""
    svc = _get_service(request)
    kwargs: Dict[str, Any] = {
        "query": body.query,
        "user_name": body.user_name,
        "task_name": body.task_name,
    }
    if body.limit is not None:
        kwargs["limit"] = body.limit
    try:
        return await svc.call("retrieve_memory", **kwargs)
    except Exception as e:
        _raise_http(e)


@router.post("/add_memory")
async def add_memory(
    body: AddMemoryRequest,
    request: Request,
) -> Any:
    """自定义 API：添加记忆"""
    svc = _get_service(request)
    try:
        return await svc.call(
            "add_memory",
            memory_content=body.memory_content,
            user_name=body.user_name,
            task_name=body.task_name,
        )
    except Exception as e:
        _raise_http(e)


@router.get("/get_memory/{memory_id}")
async def get_memory(memory_id: str, request: Request) -> Any:
    """自定义 API：获取单条记忆"""
    svc = _get_service(request)
    try:
        return await svc.call("get_memory", memory_id=memory_id)
    except Exception as e:
        _raise_http(e)


@router.post("/update_memory")
async def update_memory(
    body: UpdateMemoryRequest,
    request: Request,
) -> Any:
    """自定义 API：更新记忆"""
    svc = _get_service(request)
    try:
        return await svc.call(
            "update_memory",
            memory_id=body.memory_id,
            memory_content=body.memory_content,
            user_name=body.user_name,
            task_name=body.task_name,
        )
    except Exception as e:
        _raise_http(e)


@router.post("/list_memory")
async def list_memory(
    body: ListMemoryRequest,
    request: Request,
) -> Any:
    """自定义 API：列出记忆"""
    svc = _get_service(request)
    kwargs: Dict[str, Any] = {
        "user_name": body.user_name,
        "task_name": body.task_name,
    }
    if body.limit is not None:
        kwargs["limit"] = body.limit
    if body.sort_key is not None:
        kwargs["sort_key"] = body.sort_key
    if body.reverse is not None:
        kwargs["reverse"] = body.reverse
    try:
        return await svc.call("list_memory", **kwargs)
    except Exception as e:
        _raise_http(e)


@router.post("/delete_memory")
async def delete_memory(
    body: DeleteMemoryRequest,
    request: Request,
) -> Any:
    """自定义 API：删除记忆"""
    svc = _get_service(request)
    try:
        return await svc.call("delete_memory", memory_id=body.memory_id)
    except Exception as e:
        _raise_http(e)


@router.post("/delete_all")
async def delete_all(request: Request) -> Any:
    """自定义 API：删除所有记忆"""
    svc = _get_service(request)
    try:
        return await svc.call("delete_all")
    except Exception as e:
        _raise_http(e)
