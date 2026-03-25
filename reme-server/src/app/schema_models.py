from __future__ import annotations

from typing import Any, Dict, List, Optional

from pydantic import BaseModel, Field


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