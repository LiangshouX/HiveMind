#!/usr/bin/env bash
set -euo pipefail

HOST="${HOST:-0.0.0.0}"
PORT="${PORT:-8090}"
ENV_NAME="${ENV_NAME:-reme-serve}"
PY_VER="${PY_VER:-3.12}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}/.."

use_conda=0
if command -v conda >/dev/null 2>&1; then
  use_conda=1
fi

if [ "$use_conda" -eq 1 ]; then
  . "$(conda info --base)/etc/profile.d/conda.sh"
  if ! conda env list | awk '{print $1}' | grep -qx "$ENV_NAME"; then
    conda create -y -n "$ENV_NAME" "python=${PY_VER}"
  fi
  conda activate "$ENV_NAME"
else
  if [ ! -d ".venv" ]; then
    python3 -m venv .venv
  fi
  . .venv/bin/activate
fi

python -m pip install -U pip setuptools wheel
pip install -e .

export REME_WORKING_DIR="${REME_WORKING_DIR:-.reme}"
export REME_LLM_BACKEND="${REME_LLM_BACKEND:-openai}"
export REME_LLM_MODEL_NAME="${REME_LLM_MODEL_NAME:-qwen3.5-plus}"
export REME_EMBEDDING_BACKEND="${REME_EMBEDDING_BACKEND:-openai}"
export REME_EMBEDDING_MODEL_NAME="${REME_EMBEDDING_MODEL_NAME:-text-embedding-v4}"
export REME_EMBEDDING_DIMENSIONS="${REME_EMBEDDING_DIMENSIONS:-1024}"
export REME_VECTOR_BACKEND="${REME_VECTOR_BACKEND:-chroma}"
export REME_CHROMA_HOST="${REME_CHROMA_HOST:-127.0.0.1}"
export REME_CHROMA_PORT="${REME_CHROMA_PORT:-8000}"
export REME_CHROMA_COLLECTION="${REME_CHROMA_COLLECTION:-reme}"
export REME_AUTO_START="${REME_AUTO_START:-1}"

export REME_SERVER_HOST="${REME_SERVER_HOST:-$HOST}"
export REME_SERVER_PORT="${REME_SERVER_PORT:-$PORT}"

if [ -z "${OPENAI_API_KEY:-}" ]; then
  echo "警告：未检测到 OPENAI_API_KEY，确保你已在环境中设置或改用其他模型提供商" >&2
fi

reme-server --host "$HOST" --port "$PORT"