from __future__ import annotations

from typing import Any, Dict

from fastapi import APIRouter, HTTPException, Request

from schema_models import (
    AddMemoryRequest,
    DeleteMemoryRequest,
    ListMemoryRequest,
    RetrieveMemoryRequest,
    SummarizeMemoryRequest,
    UpdateMemoryRequest,
)
from service import ReMeService


router = APIRouter(tags=["reme"])


def _get_service(request: Request) -> ReMeService:
    svc = getattr(request.app.state, "reme_service", None)
    if svc is None:
        raise HTTPException(status_code=503, detail="ReMe service is not initialized")
    return svc


def _raise_http(e: Exception) -> None:
    status = 500
    if isinstance(e, RuntimeError):
        status = 503
    raise HTTPException(status_code=status, detail=str(e)) from e


@router.get("/healthz")
async def healthz(request: Request) -> Dict[str, Any]:
    svc = _get_service(request)
    hb = await svc.heartbeat()
    return {"ok": True, "reme": hb}


@router.get("/heartbeat")
async def heartbeat(request: Request) -> Dict[str, Any]:
    svc = _get_service(request)
    return await svc.heartbeat()


@router.post("/summarize_memory")
async def summarize_memory(
    body: SummarizeMemoryRequest,
    request: Request,
) -> Any:
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
    svc = _get_service(request)
    try:
        return await svc.call("delete_memory", memory_id=body.memory_id)
    except Exception as e:
        _raise_http(e)


@router.post("/delete_all")
async def delete_all(request: Request) -> Any:
    svc = _get_service(request)
    try:
        return await svc.call("delete_all")
    except Exception as e:
        _raise_http(e)