#!/bin/bash
# Apply ReMe patches for HiveMind integration
# Usage: ./patches/apply_patches.sh
# Requires: Python environment with reme installed

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "Applying ReMe patches for HiveMind..."
echo ""

python3 "${SCRIPT_DIR}/apply_patch.py"
