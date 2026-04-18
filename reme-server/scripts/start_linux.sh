#!/bin/bash
# ReMe Production Startup Script (Linux)
# Usage: ./scripts/start_linux.sh [--env prod|dev] [--config prod|local]

set -euo pipefail

# ===== Configuration Parameters =====
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
ENV_FILE="${PROJECT_ROOT}/.env"
CONFIG_DIR="${PROJECT_ROOT}/config"

# Default parameters
ENV_PROFILE="${1:-prod}"        # prod | dev
CONFIG_PROFILE="${2:-prod}"     # prod | local

# ===== Environment Check =====
check_env() {
    if [[ ! -f "$ENV_FILE" ]]; then
        echo "❌ Error: .env file not found at $ENV_FILE"
        echo "💡 Please copy .env.example to .env and fill in your API keys"
        exit 1
    fi

    if [[ ! -f "${CONFIG_DIR}/${CONFIG_PROFILE}.yaml" ]]; then
        echo "❌ Error: config file not found: ${CONFIG_DIR}/${CONFIG_PROFILE}.yaml"
        exit 1
    fi

    # Check Python environment
    if ! command -v python3 &>/dev/null; then
        echo "❌ Error: python3 not found in PATH"
        exit 1
    fi
}

# ===== Load Environment Variables =====
load_env() {
    echo "Loading environment from $ENV_FILE..."
    # Use python-dotenv to load (safer than set -a, supports variable references)
    python3 -c "
import os, sys
from dotenv import dotenv_values
env_vars = dotenv_values('$ENV_FILE')
for k, v in env_vars.items():
    if v is not None:
        os.environ.setdefault(k, v)
"
}

# ===== Create Data Directories =====
setup_dirs() {
    echo "Setting up data directories..."
    mkdir -p "${PROJECT_ROOT}/data"/{vectors,memories,logs}

    # Set log directory permissions (avoid uvicorn write failures)
    chmod 755 "${PROJECT_ROOT}/data/logs" 2>/dev/null || true
}

# ===== Start Service =====
start_service() {
    echo "Starting ReMe HTTP Service..."
    echo "   Profile: $ENV_PROFILE"
    echo "   Config:  ${CONFIG_PROFILE}.yaml"
    echo "   Port:    ${HTTP_PORT:-8002}"
    echo ""

    cd "$PROJECT_ROOT"

    # Start command: load YAML using config.file, environment variables auto-injected
  reme \
        backend=http \
        config.file="${CONFIG_DIR}/${CONFIG_PROFILE}.yaml" \
        log.level="${LOG_LEVEL:-INFO}" \
        2>&1 | tee -a "${PROJECT_ROOT}/data/logs/reme-$(date +%Y%m%d).log"
}

# ===== Main Process =====
main() {
    echo "ReMe Production Startup Script"
    echo "=================================="

    check_env
    load_env
    setup_dirs
    start_service
}

# Capture exit signals for graceful shutdown
trap 'echo "⚠️  Received shutdown signal, exiting..."; exit 0' SIGINT SIGTERM

main "$@"