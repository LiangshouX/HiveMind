#!/bin/bash
# ReMe Production Startup Script (Linux)
# Usage: ./scripts/start_linux.sh [prod|dev] [prod|local]

set -euo pipefail

# ===== Configuration Parameters =====
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="${SCRIPT_DIR}/.."
ENV_FILE="${PROJECT_ROOT}/.env"
CONFIG_DIR="${PROJECT_ROOT}/config"

# Default parameters
ENV_PROFILE="${1:-prod}"
CONFIG_PROFILE="${2:-prod}"

echo "ReMe Production Startup Script (Linux)"
echo "=========================================="

# ===== Environment Check =====
if [[ ! -f "$ENV_FILE" ]]; then
    echo "Error: .env file not found at $ENV_FILE"
    echo "Please copy .env.example to .env and fill in your API keys"
    exit 1
fi

# ===== Load Environment Variables (using python-dotenv) =====
echo "Loading environment from $ENV_FILE..."
python3 -c "import os; from dotenv import dotenv_values; [os.environ.setdefault(k,v) for k,v in dotenv_values(r'$ENV_FILE').items() if v]"

# ===== Start Service =====
echo "Starting ReMe HTTP Service..."
echo "   Profile: $ENV_PROFILE"
echo "   Config:  ${CONFIG_PROFILE}.yaml"
echo "   Port:    ${HTTP_PORT:-8002}"
echo ""

cd "$PROJECT_ROOT"

# Start command (Linux paths use forward slashes, Python auto-compatible)
reme \
    backend=http \
    log.level=${LOG_LEVEL:-INFO} \
    vector_store.default.backend=local
