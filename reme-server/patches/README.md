# ReMe Patches for HiveMind

本目录包含 HiveMind 对 ReMe **已安装包**的补丁。补丁直接修改 pip 安装的 `reme` 包源码，不修改 `.lib-repo/` 目录。

## 补丁列表

### mcp_metadata_fix

**问题**：ReMe 的 `MCPService.add_job()` 在将 Job 注册为 MCP 工具时，只返回 `response.answer`（人类可读文本），丢弃了 `response.metadata` 中的结构化数据。

```python
# 原始代码 (reme/components/service/mcp_service.py)
async def execute_tool(**kwargs):
    response = await job(**kwargs)
    return response.answer  # ← metadata 被丢弃
```

**影响**：

| 工具 | answer 内容 | 丢失的 metadata |
|---|---|---|
| `list` | `"Listed 4 file(s) under user-001/"` | `items: string[]`, `count: int` |
| `search` | 人类可读搜索结果文本 | `results`, `link_expansion`, `counts` |
| `daily_list` | 人类可读 note 列表 | `notes`, `count`, `date` |
| `traverse` | 图遍历文本结果 | `edges`, `count` |
| `node_search` | 节点列表文本 | `hits`, `counts` |

**修复**：当 `response.metadata` 非空时，返回 JSON envelope `{"answer": "...", "metadata": {...}}`；metadata 为空时行为不变。

**上游修复状态**：待提交 PR 到 ReMe 项目

## 应用方法

```bash
# Windows
patches\apply_patches.bat

# Linux/macOS
./patches/apply_patches.sh

# 或直接运行 Python 脚本（推荐，可看到详细输出）
python patches/apply_patch.py
```

脚本会：
1. 自动定位 pip 安装的 `reme` 包路径
2. 检查补丁是否已应用（幂等）
3. 应用代码修改
4. 提示重启 ReMe 服务

## 恢复方法

```bash
# Windows
python patches\revert_patch.py

# Linux/macOS
python patches/revert_patch.py
```

或重新安装 ReMe 包：

```bash
pip install --force-reinstall reme-ai[core]
```

## 新开发者首次设置

```bash
cd reme-server
pip install -r requirements.txt    # 安装依赖（含 reme 包）
python patches/apply_patch.py      # 应用补丁
scripts\start_mcp.bat              # 启动服务
```

## 文件说明

| 文件 | 说明 |
|---|---|
| `apply_patch.py` | 补丁应用脚本（自动定位已安装的 reme 包） |
| `revert_patch.py` | 补丁回滚脚本 |
| `apply_patches.bat` | Windows 包装脚本 |
| `apply_patches.sh` | Linux/macOS 包装脚本 |
