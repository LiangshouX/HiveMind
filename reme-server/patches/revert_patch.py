"""
Revert mcp_metadata_fix patch from the installed ReMe package.

Idempotent: safe to run multiple times.
"""

import importlib.util
import sys
from pathlib import Path


def find_mcp_service_path() -> Path:
    """Locate the installed reme package's mcp_service.py."""
    spec = importlib.util.find_spec("reme")
    if spec is None or spec.origin is None:
        print("ERROR: 'reme' package not found.")
        sys.exit(1)
    reme_root = Path(spec.origin).parent
    target = reme_root / "components" / "service" / "mcp_service.py"
    if not target.exists():
        print(f"ERROR: mcp_service.py not found at {target}")
        sys.exit(1)
    return target


def revert_patch(target: Path) -> bool:
    """Revert the metadata fix. Returns True if changes were made."""
    content = target.read_text(encoding="utf-8")

    patched_block = (
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

    if patched_block not in content:
        print("Patch not applied — nothing to revert.")
        return False

    original_block = (
        "        async def execute_tool(**kwargs):\n"
        "            response = await job(**kwargs)\n"
        "            return response.answer"
    )

    content = content.replace(patched_block, original_block, 1)
    target.write_text(content, encoding="utf-8")
    return True


def main():
    print("ReMe MCP Metadata Fix — Revert Patch")
    print("=" * 40)
    print()

    target = find_mcp_service_path()
    print(f"Target: {target}")
    print()

    if revert_patch(target):
        print("SUCCESS: Patch reverted.")
    print()
    print("Restart ReMe MCP server for changes to take effect.")


if __name__ == "__main__":
    main()
