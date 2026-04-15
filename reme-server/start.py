# ReMe Server 启动脚本

import uvicorn

if __name__ == "__main__":
    # 启动 ReMe Server
    # 默认监听 0.0.0.0:8085
    # 可以通过环境变量配置：
    #   - REME_API_KEY: API Key（可选）
    #   - REME_WORKING_DIR: 工作目录
    #   - REME_LLM_BACKEND: LLM 后端
    #   - REME_LLM_MODEL_NAME: LLM 模型
    #   - REME_EMBEDDING_BACKEND: Embedding 后端
    #   - REME_EMBEDDING_MODEL_NAME: Embedding 模型
    #   - REME_VECTOR_BACKEND: 向量后端 (chroma/local)
    #   - REME_CHROMA_HOST: Chroma 主机
    #   - REME_CHROMA_PORT: Chroma 端口
    #   - REME_CHROMA_COLLECTION: Chroma 集合
    
    uvicorn.run(
        "src.app.main:app",
        host="0.0.0.0",
        port=8085,
        reload=False,
        log_level="info",
    )
