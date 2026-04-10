from __future__ import annotations

import argparse
import os
from contextlib import asynccontextmanager

import uvicorn
from fastapi import FastAPI

from router import router as reme_router
from service import ReMeServerConfig, ReMeService


@asynccontextmanager
async def lifespan(app: FastAPI):
    config = ReMeServerConfig.from_env()
    service = ReMeService(config=config)
    app.state.reme_service = service
    auto_start = os.getenv("REME_AUTO_START", "1").lower() not in {
        "0",
        "false",
        "no",
        "off",
    }
    if auto_start:
        await service.start()
    try:
        yield
    finally:
        await service.close()


def create_app() -> FastAPI:
    app = FastAPI(lifespan=lifespan, title="ReMe Server", version="0.1.0")
    app.include_router(reme_router)
    return app


app = create_app()


def main() -> None:
    parser = argparse.ArgumentParser(description="ReMe standalone server")
    parser.add_argument("--host", default=os.getenv("REME_SERVER_HOST", "127.0.0.1"))
    parser.add_argument("--port", default=int(os.getenv("REME_SERVER_PORT", "8085")), type=int)
    parser.add_argument("--reload", action="store_true")
    parser.add_argument("--workers", default=int(os.getenv("REME_SERVER_WORKERS", "1")), type=int)
    parser.add_argument("--log-level", default=os.getenv("REME_SERVER_LOG_LEVEL", "info"))
    args = parser.parse_args()

    uvicorn.run(
        app,
        host=args.host,
        port=args.port,
        reload=False,
        workers=args.workers,
        log_level=args.log_level,
    )


if __name__ == "__main__":
    main()