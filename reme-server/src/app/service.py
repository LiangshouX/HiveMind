from __future__ import annotations

import asyncio
import os
import time
from dataclasses import dataclass
from typing import Any, Dict, Optional, Tuple

import httpx


@dataclass(frozen=True)
class ReMeServerConfig:
    working_dir: str
    llm_backend: str
    llm_model_name: str
    embedding_backend: str
    embedding_model_name: str
    embedding_dimensions: int
    preferred_vector_backend: str
    chroma_host: str
    chroma_port: int
    chroma_collection: str

    @staticmethod
    def from_env() -> "ReMeServerConfig":
        return ReMeServerConfig(
            working_dir=os.getenv("REME_WORKING_DIR", ".reme"),
            llm_backend=os.getenv("REME_LLM_BACKEND", "openai"),
            llm_model_name=os.getenv("REME_LLM_MODEL_NAME", "qwen3.5-plus"),
            embedding_backend=os.getenv("REME_EMBEDDING_BACKEND", "openai"),
            embedding_model_name=os.getenv(
                "REME_EMBEDDING_MODEL_NAME",
                "text-embedding-v4",
            ),
            embedding_dimensions=int(os.getenv("REME_EMBEDDING_DIMENSIONS", "1024")),
            preferred_vector_backend=os.getenv(
                "REME_VECTOR_BACKEND",
                "chroma",
            ).lower(),
            chroma_host=os.getenv("REME_CHROMA_HOST", "127.0.0.1"),
            chroma_port=int(os.getenv("REME_CHROMA_PORT", "8000")),
            chroma_collection=os.getenv("REME_CHROMA_COLLECTION", "reme"),
        )


class ReMeService:
    def __init__(self, config: ReMeServerConfig) -> None:
        self._config = config
        self._lock = asyncio.Lock()
        self._reme: Any | None = None
        self._started_at: float | None = None
        self._vector_backend_in_use: str | None = None
        self._last_error: str | None = None

    @property
    def config(self) -> ReMeServerConfig:
        return self._config

    def _build_reme(self, vector_backend: str) -> Any:
        from reme import ReMe

        default_vector_store_config: Dict[str, Any] = {"backend": vector_backend}
        if vector_backend == "chroma":
            default_vector_store_config.update(
                {
                    "host": self._config.chroma_host,
                    "port": self._config.chroma_port,
                    "collection_name": self._config.chroma_collection,
                },
            )

        return ReMe(
            working_dir=self._config.working_dir,
            default_llm_config={
                "backend": self._config.llm_backend,
                "model_name": self._config.llm_model_name,
            },
            default_embedding_model_config={
                "backend": self._config.embedding_backend,
                "model_name": self._config.embedding_model_name,
                "dimensions": self._config.embedding_dimensions,
            },
            default_vector_store_config=default_vector_store_config,
        )

    async def _probe_chroma(self) -> Tuple[bool, Optional[str]]:
        url_base = f"http://{self._config.chroma_host}:{self._config.chroma_port}"
        candidates = [
            f"{url_base}/api/v1/heartbeat",
            f"{url_base}/api/v2/heartbeat",
            f"{url_base}/heartbeat",
        ]
        timeout = httpx.Timeout(connect=1.0, read=1.0, write=1.0, pool=1.0)
        async with httpx.AsyncClient(timeout=timeout) as client:
            for url in candidates:
                try:
                    resp = await client.get(url)
                    if 200 <= resp.status_code < 300:
                        return True, url
                except Exception:
                    continue
        return False, None

    async def _start_locked(self) -> None:
        if self._reme is not None:
            return

        try:
            import reme as reme_module  # noqa: F401
        except Exception as e:
            self._last_error = f"reme import failed: {e}"
            self._reme = None
            self._vector_backend_in_use = None
            self._started_at = None
            return

        preferred = self._config.preferred_vector_backend
        backends = [preferred]
        if preferred != "local":
            backends.append("local")

        for backend in backends:
            try:
                if backend == "chroma":
                    ok, _ = await self._probe_chroma()
                    if not ok:
                        raise RuntimeError("chroma heartbeat probe failed")
                reme = self._build_reme(backend)
                await reme.start()
                self._reme = reme
                self._vector_backend_in_use = backend
                self._started_at = time.time()
                self._last_error = None
                return
            except Exception as e:
                self._last_error = f"start failed (backend={backend}): {e}"
                self._reme = None
                self._vector_backend_in_use = None
                self._started_at = None

    async def start(self) -> None:
        async with self._lock:
            await self._start_locked()

    async def _close_locked(self) -> None:
        if self._reme is None:
            return
        reme = self._reme
        self._reme = None
        self._vector_backend_in_use = None
        self._started_at = None
        try:
            await reme.close()
        except Exception as e:
            self._last_error = f"close failed: {e}"

    async def close(self) -> None:
        async with self._lock:
            await self._close_locked()

    def _should_degrade(self, exc: Exception) -> bool:
        if self._vector_backend_in_use != "chroma":
            return False
        msg = str(exc).lower()
        keywords = [
            "connection",
            "connect",
            "timeout",
            "refused",
            "unavailable",
            "chroma",
        ]
        return any(k in msg for k in keywords)

    async def call(self, method_name: str, **kwargs: Any) -> Any:
        async with self._lock:
            if self._reme is None:
                await self._start_locked()
            if self._reme is None:
                raise RuntimeError(self._last_error or "ReMe is not available")

            method = getattr(self._reme, method_name)
            try:
                return await method(**kwargs)
            except Exception as e:
                if self._should_degrade(e):
                    try:
                        await self._close_locked()
                        self._config = ReMeServerConfig(
                            working_dir=self._config.working_dir,
                            llm_backend=self._config.llm_backend,
                            llm_model_name=self._config.llm_model_name,
                            embedding_backend=self._config.embedding_backend,
                            embedding_model_name=self._config.embedding_model_name,
                            embedding_dimensions=self._config.embedding_dimensions,
                            preferred_vector_backend="local",
                            chroma_host=self._config.chroma_host,
                            chroma_port=self._config.chroma_port,
                            chroma_collection=self._config.chroma_collection,
                        )
                        await self._start_locked()
                        if self._reme is not None:
                            method2 = getattr(self._reme, method_name)
                            return await method2(**kwargs)
                    except Exception as e2:
                        self._last_error = f"degrade retry failed: {e2}"
                self._last_error = f"call failed ({method_name}): {e}"
                raise

    async def heartbeat(self) -> Dict[str, Any]:
        chroma_ok = None
        chroma_url = None
        try:
            chroma_ok, chroma_url = await self._probe_chroma()
        except Exception:
            chroma_ok, chroma_url = False, None

        return {
            "ready": self._reme is not None,
            "started_at": self._started_at,
            "vector_backend_in_use": self._vector_backend_in_use,
            "preferred_vector_backend": self._config.preferred_vector_backend,
            "chroma": {
                "host": self._config.chroma_host,
                "port": self._config.chroma_port,
                "collection": self._config.chroma_collection,
                "reachable": chroma_ok,
                "heartbeat_url": chroma_url,
            },
            "working_dir": self._config.working_dir,
            "llm": {
                "backend": self._config.llm_backend,
                "model_name": self._config.llm_model_name,
            },
            "embedding": {
                "backend": self._config.embedding_backend,
                "model_name": self._config.embedding_model_name,
                "dimensions": self._config.embedding_dimensions,
            },
            "last_error": self._last_error,
        }