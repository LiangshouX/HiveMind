#!/bin/bash
# ReMe MCP Server Startup Script (Linux)
# Usage: ./scripts/start_mcp.sh [transport] [log_level]
# Example: ./scripts/start_mcp.sh sse INFO

set -euo pipefail

# ===== Configuration Parameters =====
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="${SCRIPT_DIR}/.."
ENV_FILE="${PROJECT_ROOT}/.env"

# Default parameters
MCP_TRANSPORT="${1:-sse}"
LOG_LEVEL="${2:-INFO}"

echo "ReMe MCP Server Startup Script (Linux)"
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
echo "Starting ReMe MCP Server..."
echo "   Transport: $MCP_TRANSPORT"
echo "   Log Level: $LOG_LEVEL"
echo ""

cd "$PROJECT_ROOT"

# Start command with MCP backend
reme \
    start \
    service.backend=mcp \
    mcp.transport=$MCP_TRANSPORT \
    log.level=$LOG_LEVEL
