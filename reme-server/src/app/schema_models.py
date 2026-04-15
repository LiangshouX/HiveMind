"""
ReMe Server Schema Models

包含官方 API 和自定义 API 的数据模型定义。
"""
from __future__ import annotations

from typing import Any, Dict, List, Optional

from pydantic import BaseModel, Field


# =============================================================================
# 官方 API 数据模型（与 Java 客户端兼容）
# =============================================================================


class ReMeMessage(BaseModel):
    """官方 ReMe 消息格式"""

    role: str  # "user" 或 "assistant"
    content: str
    time_created: Optional[str] = None


class ReMeTrajectory(BaseModel):
    """官方 ReMe 对话轨迹格式"""

    messages: List[ReMeMessage]


class SummaryPersonalMemoryRequest(BaseModel):
    """官方 ReMe 添加记忆请求格式"""

    workspace_id: str
    trajectories: List[ReMeTrajectory] = Field(default_factory=list)


class RetrievePersonalMemoryRequest(BaseModel):
    """官方 ReMe 检索记忆请求格式"""

    workspace_id: str
    query: str
    top_k: Optional[int] = 5


class SummaryPersonalMemoryResponse(BaseModel):
    """官方 ReMe 添加记忆响应格式"""

    answer: Optional[str] = None
    success: bool = True
    metadata: Optional[Dict[str, Any]] = None
    memories: Optional[List[str]] = None


class RetrievePersonalMemoryResponse(BaseModel):
    """官方 ReMe 检索记忆响应格式"""

    answer: Optional[str] = None
    success: bool = True
    metadata: Optional[Dict[str, Any]] = None
    memories: Optional[List[str]] = None


# =============================================================================
# 自定义 API 数据模型（保留向后兼容）
# =============================================================================


class Message(BaseModel):
    role: str
    content: str
    time_created: Optional[str] = None


class SummarizeMemoryRequest(BaseModel):
    messages: List[Dict[str, Any]] = Field(default_factory=list)
    user_name: Optional[str] = None
    task_name: Optional[str] = None


class RetrieveMemoryRequest(BaseModel):
    query: str
    user_name: Optional[str] = None
    task_name: Optional[str] = None
    limit: Optional[int] = None


class AddMemoryRequest(BaseModel):
    memory_content: str
    user_name: Optional[str] = None
    task_name: Optional[str] = None


class UpdateMemoryRequest(BaseModel):
    memory_id: str
    memory_content: str
    user_name: Optional[str] = None
    task_name: Optional[str] = None


class ListMemoryRequest(BaseModel):
    user_name: Optional[str] = None
    task_name: Optional[str] = None
    limit: Optional[int] = None
    sort_key: Optional[str] = None
    reverse: Optional[bool] = None


class DeleteMemoryRequest(BaseModel):
    memory_id: str
