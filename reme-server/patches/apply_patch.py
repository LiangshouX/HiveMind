"""
Apply mcp_metadata_fix patch to the installed ReMe package.

Fixes: MCPService.add_job() discards response.metadata, returning only response.answer.
When metadata is non-empty, the patched version returns a JSON envelope:
  {"answer": "...", "metadata": {...}}

Idempotent: safe to run multiple times.
"""

import importlib.util
import sys
from pathlib import Path


def find_mcp_service_path() -> Path:
    """Locate the installed reme package's mcp_service.py."""
    # Try direct import without triggering full reme import (which needs agentscope)
    spec = importlib.util.find_spec("reme")
    if spec is None or spec.origin is None:
        print("ERROR: 'reme' package not found. Is it installed in this Python environment?")
        sys.exit(1)

    reme_root = Path(spec.origin).parent
    target = reme_root / "components" / "service" / "mcp_service.py"
    if not target.exists():
        print(f"ERROR: mcp_service.py not found at {target}")
        sys.exit(1)
    return target


def apply_patch(target: Path) -> bool:
    """Apply the metadata fix patch. Returns True if changes were made."""
    content = target.read_text(encoding="utf-8")

    old_block = (
        "        async def execute_tool(**kwargs):\n"
        "            response = await job(**kwargs)\n"
        "            return response.answer"
    )

    if old_block not in content:
        if "response.metadata" in content:
            print("Patch already applied — skipping.")
            return False
        print("ERROR: Target code block not found. ReMe version may have changed.")
        print(f"File: {target}")
        sys.exit(1)

    new_block = (
        "        async def execute_tool(**kwargs):\n"
        "            response = await job(**kwargs)\n"
        "            # HiveMind fix: return structured data when metadata is present,\n"
        "            # so MCP consumers can access file lists, search results, etc.\n"
        "            if response.metadata:\n"
        "                import json as _json\n"
        "                return _json.dumps(\n"
        '                    {"answer": response.answer, "metadata": response.metadata},\n'
        "                    ensure_ascii=False,\n"
        "                    default=str,\n"
        "                )\n"
        "            return response.answer"
    )

    content = content.replace(old_block, new_block, 1)
    target.write_text(content, encoding="utf-8")
    return True


def main():
    print("ReMe MCP Metadata Fix — HiveMind Integration Patch")
    print("=" * 55)
    print()

    target = find_mcp_service_path()
    print(f"Target: {target}")
    print()

    if apply_patch(target):
        print("SUCCESS: Patch applied.")
    print()
    print("Restart ReMe MCP server for changes to take effect.")


if __name__ == "__main__":
    main()
