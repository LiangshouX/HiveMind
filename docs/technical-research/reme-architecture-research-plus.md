# ReMe 深度技术调研报告 Plus

> **文档版本**: 2.0 (Plus) \
> **创建日期**: 2026-07-14 \
> **基于版本**: reme-ai 0.4.0.9, ReMe 论文 (arXiv:2512.10696v2) \
> **状态**: 深度调研完成

---

## 目录

1. [论文理论基础](#1-论文理论基础)
2. [ReMe 演进历史](#2-reme-演进历史)
3. [核心设计哲学](#3-核心设计哲学)
4. [完整分层架构](#4-完整分层架构)
5. [核心子系统深度解析](#5-核心子系统深度解析)
6. [记忆流水线详解](#6-记忆流水线详解)
7. [高级检索系统](#7-高级检索系统)
8. [服务暴露与集成](#8-服务暴露与集成)
9. [部署架构与运维](#9-部署架构与运维)
10. [与 HiveMind 集成分析](#10-与-hivemind-集成分析)
11. [附录：完整组件清单](#11-附录完整组件清单)

---

## 1. 论文理论基础

### 1.1 论文概述

ReMe 论文 *"ReMe: Dynamic Procedural Memory Framework for Experience-Driven Evolution"*（arXiv:
2512.10696v2）由上海交通大学与阿里巴巴集团联合发表。论文提出了一个面向 LLM Agent 的**程序性记忆（Procedural Memory）**
框架，核心解决的问题是：**如何让 Agent 从交互经验中学习 "how-to" 知识，并随时间自我进化**。

### 1.2 论文核心论点

传统 LLM Agent 的记忆管理采用**被动积累（Passive Accumulation）**范式——将交互轨迹或经验摘要直接追加存储，存在三个根本性局限：

| 局限         | 具体表现                  |
|------------|-----------------------|
| **粒度过粗**   | 轨迹级存储引入无关信息，难以抓住核心逻辑  |
| **适应性不足**  | 检索到的经验无法动态适配当前任务的具体约束 |
| **缺乏自我优化** | 过时、错误的记忆持续累积，导致记忆质量退化 |

### 1.3 ReMe 的三层机制

论文提出**经验驱动进化（Experience-Driven Evolution）**框架，包含三个协调运作的机制：

#### 1.3.1 多面向蒸馏（Multi-Faceted Distillation）

```
原始交互轨迹
    │
    ├─→ 摘要分析（Summarization）：提取关键事实和结论
    ├─→ 模式识别（Pattern Recognition）：识别成功模式
    ├─→ 失败触发分析（Failure Trigger Analysis）：分析失败原因
    └─→ 比较洞察（Comparative Insights）：对比成功与失败
            │
            ▼
    结构化 Memory Unit = {name, category, confidence, usage_context}
```

**关键设计**：不是简单地把对话存下来，而是通过 LLM 从多个角度分析交互轨迹，提取出**细粒度、可复用的程序性知识单元**
。论文指出，关键点级（keypoint-level）蒸馏显著优于轨迹级（trajectory-level）蒸馏。

#### 1.3.2 任务导向利用（Task-Grounded Utilization）

```
当前任务 T
    │
    ├─→ 语义检索：embedding 相似度召回候选经验
    ├─→ 重排序（Reranking）：根据 T 的具体约束评估每条经验的适用性
    └─→ 自适应改写（Adaptive Rewriting）：将通用经验改写为 T 专用指导
            │
            ▼
    任务专用 Prompt = 原始指令 + 改写后的经验 + 约束说明
```

**关键设计**：检索不是终点。检索到的经验需要经过重排序和改写，才能真正适配当前任务。论文中称之为"
将历史洞察与当前挑战动态连接"。

#### 1.3.3 渐进式优化（Progressive Refinement）

```
执行结果
    │
    ├─→ 成功 → 强化相关记忆的置信度，追加成功来源
    ├─→ 失败 → 触发自我反思（Self-Reflection），探索替代方案
    └─→ 效用追踪 → 定期评估每条记忆的复用效果
            │
            ▼
    记忆库更新：强化/修正/删除/新增
```

**关键设计**：记忆不是一次写入就永恒不变。系统会根据后续使用效果自动强化有效记忆、修正错误记忆、删除过时记忆。

### 1.4 论文实验结果

论文在 BFCL-V3 等基准上进行了广泛实验，关键发现：

| 发现              | 意义                                         |
|-----------------|--------------------------------------------|
| ReMe 达到 SOTA 性能 | 在工具调用基准上超越所有基线                             |
| **记忆缩放效应**      | Qwen3-8B + ReMe 记忆 > Qwen3-14B 无记忆（pass@4） |
| 细粒度蒸馏优于粗粒度      | Keypoint-level 显著优于 trajectory-level       |
| 成功分析优于纯失败分析     | 成功模式识别比单纯失败反思更有效                           |
| 自适应改写提升即刻效果     | 动态改写经验到具体场景效果更好                            |
| 渐进式优化保持长期活力     | 持续优化机制使记忆库保持高质量                            |

**核心结论**：配备自进化记忆的小模型可以超越无记忆的大模型，表明 ReMe 式记忆是**计算高效（computation-efficient）**的终身学习路径。

### 1.5 从论文到实现

论文提出的三层机制在 ReMe V4 代码库中的映射关系：

| 论文机制        | ReMe 实现                                                        |
|-------------|----------------------------------------------------------------|
| 多面向蒸馏       | `auto_memory` + `auto_dream` 的 Extract 阶段                      |
| 任务导向利用      | `search` Job 的 RRF 混合检索 + 链接展开                                 |
| 渐进式优化       | `auto_dream` 的 Integrate 阶段（CREATE/CORROBORATE/REFINE/CORRECT） |
| Memory Unit | `digest/` 目录下的 Markdown 文件（personal/procedure/wiki）            |
| 效用追踪        | `file_catalog` 的 checkpoint 机制 + `dream_finish_step`           |

---

## 2. ReMe 演进历史

### 2.1 V3 → V4 的重大变更

| 维度    | V3              | V4                                                 |
|-------|-----------------|----------------------------------------------------|
| 存储后端  | Chroma (sqlite) | 文件系统 + numpy .npz                                  |
| 向量检索  | Chroma 内置       | `LocalEmbeddingStore` + numpy 暴力计算 / FAISS         |
| 关键词检索 | Chroma 内置       | 自实现 BM25（rank_bm25 库）                              |
| 图谱    | 无               | `LocalFileGraph`（wikilink 解析）                      |
| 服务暴露  | 有限              | HTTP + MCP (SSE/StdIO/StreamableHTTP)              |
| 记忆模型  | 扁平              | 四层分层（session/resource → daily → digest → metadata） |
| 自进化   | 有限              | 完整的 auto_memory → auto_dream → proactive 流水线       |

### 2.2 为什么移除 Chroma

ReMe V4 主动移除 Chroma 的原因：

1. **Core Dump 问题**：Chroma 的 sqlite 后端在特定条件下触发段错误
2. **低版本兼容性**：Chroma 的 Python 绑定与某些环境不兼容
3. **设计哲学冲突**：Chroma 是黑盒数据库，与 "Memory as File" 理念矛盾
4. **依赖复杂度**：Chroma 引入大量传递依赖，增加部署和维护负担

---

## 3. 核心设计哲学

### 3.1 Memory as File, File as Memory

这是 ReMe 最根本的设计理念，包含两层含义：

**Memory as File**（记忆即文件）：

- 长期记忆不是藏在黑盒数据库里，而是落在 workspace 目录中的 Markdown 文件
- 用户可以直接打开 workspace，像读笔记一样读 daily、digest
- 用户和 Agent 都能用文件操作修正、补充、移动或删除记忆
- workspace 是普通目录，可被备份、同步、版本管理

**File as Memory**（文件即记忆）：

- 每个文件不只是普通文本，也是一个可索引、可链接、可演化的记忆节点
- ReMe 从文件中解析 frontmatter（YAML 元数据）、正文 chunk、wikilink 边
- 这些解析结果被组织成检索索引（BM25 + 可选向量）和文件图谱（wikilink graph）

### 3.2 六大设计目标

| 目标      | 含义                    | 实现方式                                        |
|---------|-----------------------|---------------------------------------------|
| **可读**  | 用户可以直接读 daily/digest  | Markdown 格式，目录结构清晰                          |
| **可编辑** | 人和 Agent 都能改          | 标准文件操作（read/write/edit/delete/move）         |
| **可追溯** | 结论可以回溯到原始证据           | `derived_from:: [[...]]` wikilink           |
| **可迁移** | workspace 可备份/同步/版本管理 | 普通目录 + 普通文件                                 |
| **可索引** | 文件可被高效检索              | frontmatter + chunk + BM25 + wikilink graph |
| **可协作** | 人和 Agent 操作同一套文件      | 统一的文件接口                                     |

### 3.3 设计原则

**原则一：渐进式沉淀而非一次性整理**

```
对话 → session (原始) → daily (浅加工) → digest (深加工)
```

每一层保留不同抽象级别的信息，不会丢失原始证据。

**原则二：增强而非替代**

BM25 是基础（开箱即用），向量检索是增强（可选启用），wikilink 图谱是关系层（自动构建）。三者互补而非替代。

**原则三：人机协作而非人机对立**

文件是人和 Agent 的共同接口。人负责判断和修正，Agent 负责整理、链接和检索。

---

## 4. 完整分层架构

### 4.1 系统总览

```
┌─────────────────────────────────────────────────────────────────┐
│                        CLI (reme.py)                            │
│  parse_args → start / call_server                               │
├─────────────────────────────────────────────────────────────────┤
│                     Service Layer                                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐      │
│  │  HTTP Service │  │  MCP Service │  │  Client (HTTP/MCP)│      │
│  │  (FastAPI)    │  │  (FastMCP)   │  │  (用于 CLI 调用)   │      │
│  └──────────────┘  └──────────────┘  └──────────────────┘      │
├─────────────────────────────────────────────────────────────────┤
│                   Application Layer                              │
│  配置驱动的对象装配、依赖拓扑、生命周期管理                          │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  ApplicationContext → Components → Jobs → Steps           │   │
│  └──────────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────────┤
│                      Job Layer                                   │
│  ┌──────────┐ ┌──────────┐ ┌────────────┐ ┌──────────┐        │
│  │ BaseJob  │ │ StreamJob│ │BackgroundJob│ │ CronJob  │        │
│  │ (请求型)  │ │ (流式)   │ │ (后台循环)   │ │ (定时)   │        │
│  └──────────┘ └──────────┘ └────────────┘ └──────────┘        │
├─────────────────────────────────────────────────────────────────┤
│                      Step Layer                                  │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐          │
│  │ file_io/ │ │ index/   │ │ evolve/  │ │ common/  │          │
│  │ 读写编辑  │ │ 检索索引  │ │ 自进化    │ │ 通用工具  │          │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘          │
├─────────────────────────────────────────────────────────────────┤
│                    Component Layer                                │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐  │
│  │ file_store │ │keyword_index│ │ file_graph │ │embedding_  │  │
│  │ (协调层)    │ │ (BM25)     │ │ (wikilink) │ │store (向量) │  │
│  └────────────┘ └────────────┘ └────────────┘ └────────────┘  │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐  │
│  │as_llm      │ │as_embedding│ │agent_wrapper│ │file_catalog│  │
│  │ (LLM封装)  │ │(Embed封装) │ │ (Agent封装) │ │ (变更追踪)  │  │
│  └────────────┘ └────────────┘ └────────────┘ └────────────┘  │
├─────────────────────────────────────────────────────────────────┤
│                    Workspace (文件系统)                           │
│  session/ + resource/ → daily/ → digest/ + metadata/            │
└─────────────────────────────────────────────────────────────────┘
```

### 4.2 记忆四层模型

```
<workspace_dir>/
├── metadata/                    # 系统索引层
│   ├── file_store/              # FileChunk JSONL + FAISS 索引
│   ├── file_graph/              # FileNode + FileLink JSONL
│   ├── keyword_index/           # BM25 倒排索引
│   ├── file_catalog/            # 文件变更 checkpoint
│   └── embedding_store/         # embedding .npz 缓存
│
├── session/                     # 原始输入层
│   ├── dialog/<session_id>.jsonl      # auto_memory 保存的对话
│   ├── agentscope/<session_id>.jsonl  # AgentScope session
│   └── claude_code/<session_id>.jsonl # Claude Code session
│
├── resource/                    # 原始输入层
│   └── YYYY-MM-DD/
│       └── <resource>.<ext>     # 外部资源文件
│
├── daily/                       # 浅加工层
│   ├── YYYY-MM-DD.md            # 当天索引页（自动生成）
│   └── YYYY-MM-DD/
│       ├── <session_id>.md      # 对话记忆卡片
│       ├── <resource_stem>.md   # 资源记忆卡片
│       └── interests.yaml       # auto_dream 产出的兴趣主题
│
└── digest/                      # 深加工层
    ├── personal/                # 用户偏好、长期个人事实
    ├── procedure/               # 流程、方法论、操作经验
    └── wiki/                    # 通用知识、概念、决策先例
```

### 4.3 Registry 与依赖注入机制

ReMe 使用进程级单例 `R = ComponentRegistry()` 实现服务定位器模式：

```python
# 注册：模块 import 时自动执行
@R.register("local")
class LocalFileStore(BaseFileStore):
    ...

# 注册表 key: (ComponentEnum, register_name) -> class
# 例如: (FILE_STORE, "local") -> LocalFileStore
```

组件间的依赖通过 `BaseComponent.bind()` 声明：

```python
class LocalFileStore(BaseFileStore):
    def __init__(self, embedding_store="default", keyword_index="default", ...):
        self.embedding_store = self.bind(embedding_store, BaseEmbeddingStore)
        self.keyword_index = self.bind(keyword_index, BaseKeywordIndex)
        self.file_graph = self.bind(file_graph, BaseFileGraph)
```

启动时 `Application._topological_order()` 按依赖拓扑排序启动所有组件。

Step 通过 `Ref` 声明式访问组件：

```python
class SearchStep(BaseStep):
    file_store: BaseFileStore = Ref(BaseFileStore, ComponentEnum.FILE_STORE)
    # 运行时自动解析为 app_context.components[FILE_STORE]["default"]
```

### 4.4 Application 生命周期

```python
# 启动流程
Application(**kwargs)
→ ApplicationContext(**kwargs)  # 解析 ApplicationConfig
→ _setup_workspace_directories()  # 确保 session/daily/digest/metadata 存在
→ _init_service()  # 创建 HTTP 或 MCP Service
→ _init_components()  # 实例化所有 Component
→ _init_jobs()  # 实例化所有 Job

# 运行流程
run_app()
→ service.run_app(app)  # 启动 HTTP/MCP 服务
→ lifespan: app.start()  # 触发 Application._start()
→ components
拓扑排序启动
→ BaseJob
启动
→ StreamJob
启动
→ BackgroundJob
启动（后台线程）
→ CronJob
启动（定时调度）

# 关闭流程
Application._close()
→ 按
_started_components
反序关闭
→ 保证依赖方先关闭，被依赖方后关闭
```

---

## 5. 核心子系统深度解析

### 5.1 FileStore（文件存储协调层）

`LocalFileStore` 是默认的 file_store 后端，是整个检索系统的核心协调者。

#### 5.1.1 组合架构

```
LocalFileStore
  ├── file_chunks: dict[str, FileChunk]    # 内存中的 chunk 数据库
  ├── embedding_store: BaseEmbeddingStore   # 向量存储（可选）
  ├── keyword_index: BaseKeywordIndex       # BM25 索引
  └── file_graph: BaseFileGraph             # wikilink 图谱
```

#### 5.1.2 持久化机制

- **格式**：JSONL + Zstandard 压缩（`.jsonl.zst`）
- **路径**：`metadata/file_store/file_chunks_{name}_{version}.jsonl.zst`
- **加载时机**：`_start()` 时从磁盘加载到内存
- **持久化时机**：`_close()` 时从内存 dump 到磁盘
- **增量更新**：`upsert()` 和 `delete()` 操作内存数据，索引同步更新

#### 5.1.3 关键操作

```python
# 写入（文件 → chunks → 索引）
async def upsert(self, path, chunks, links):
    # 1. 删除旧 chunks
    old_ids = [c.id for c in self.file_chunks if c.path == path]
    await self._delete_chunks(old_ids)
    # 2. 写入新 chunks
    for chunk in chunks:
        self.file_chunks[chunk.id] = chunk
    # 3. 更新 BM25 索引
    await self.keyword_index.upsert(chunks)
    # 4. 更新向量索引（如果启用）
    if self.embedding_store:
        embeddings = await self.embedding_store.get_embeddings([c.text for c in chunks])
        # 5. 更新 wikilink 图谱
    await self.file_graph.upsert_links(links)


# 检索（BM25 + 向量 + RRF 融合）
async def search(self, query, limit, ...):
    bm25_results = await self.keyword_search(query, limit)
    vector_results = await self.vector_search(query, limit)  # 如果启用
    return rrf_fuse(bm25_results, vector_results)
```

#### 5.1.4 FaissLocalFileStore 变体

当需要更高效的向量检索时，可使用 FAISS 后端：

```yaml
file_store:
  default:
    backend: faiss        # 而非 local
    embedding_store: default
    keyword_index: default
    file_graph: default
```

`FaissLocalFileStore` 继承 `LocalFileStore`，仅替换向量检索部分：

- 使用 `faiss.IndexFlatIP`（内积索引，L2 归一化后等价于余弦相似度）
- 支持增量添加/删除（tombstone 机制）
- 索引持久化为 `.bin` 文件 + id-map `.json` 文件
- 懒加载：仅在需要时 import faiss 库

### 5.2 EmbeddingStore（向量存储）

`LocalEmbeddingStore` 是唯一的 embedding_store 后端：

```
LocalEmbeddingStore
  ├── LRU Cache (默认 10000 条)
  │   └── text_hash → numpy embedding
  ├── as_embedding: BaseASEmbedding
  │   └── 外部 Embedding API 调用
  └── 持久化：metadata/embedding_store/*.npz
```

**关键特性**：

- **LRU 缓存**：内存中维护最近使用的 embedding，淘汰最久未使用的
- **批量计算**：miss 的文本通过外部 Embedding API 批量计算
- **维度校验**：严格校验 embedding 维度，不匹配的自动丢弃
- **健康检查**：启动时检测 Embedding 服务可用性，不可用时自动禁用

**重要**：这不是一个"向量数据库"，而是一个**embedding 缓存层**。真正的向量搜索在 `LocalFileStore.vector_search()` 中通过
numpy 的 `batch_cosine_similarity` 做暴力计算。对于大规模场景，应使用 `FaissLocalFileStore`。

### 5.3 KeywordIndex（BM25 关键词索引）

基于 `rank_bm25` 库的 BM25 倒排索引：

```
BM25Index
  ├── tokenizer: RegexTokenizer
  │   └── 正则分词（默认 pattern: r"\w+"）
  ├── inverted_index: dict[token → list[doc_id]]
  ├── doc_freqs: dict[doc_id → dict[token → freq]]
  └── 持久化：metadata/keyword_index/*.jsonl.zst
```

**BM25 算法参数**：

- `k1 = 1.5`（词频饱和参数）
- `b = 0.75`（文档长度归一化参数）
- 支持 lazy delete + optimize 压缩

**检索流程**：

1. 对 query 分词
2. 查倒排表获取候选 doc_id
3. 对每个候选计算 BM25 分数
4. 按分数排序返回 top-k

### 5.4 FileGraph（wikilink 图谱）

`LocalFileGraph` 管理文件间的 wikilink 关系：

```
LocalFileGraph
  ├── nodes: dict[path → FileNode]
  │   └── FileNode: path, name, description, frontmatter, st_mtime
  ├── edges: list[FileLink]
  │   └── FileLink: source_path, target_path, predicate, anchor
  ├── outlinks: dict[path → list[FileLink]]
  ├── inlinks: dict[path → list[FileLink]]
  └── 持久化：metadata/file_graph/*.jsonl.zst
```

**Wikilink 解析规则**：

- `[[path]]` → 纯链接
- `predicate:: [[path]]` → 带关系名的链接
- `[predicate:: [[path]]]` → 括号内的带关系链接
- `![[path]]` → 嵌入引用（当前不特殊处理）
- 路径语义：字面路径，不自动补 `.md`，不做文件名搜索

**图谱操作**：

- `upsert_nodes(nodes)`：更新节点元数据
- `upsert_links(links)`：添加/更新边
- `delete_by_path(path)`：删除节点及其关联边
- `get_outlinks(path)` / `get_inlinks(path)`：查询邻居

### 5.5 FileChunker（文件分块器）

#### MarkdownFileChunker

使用 `mistletoe` 库解析 Markdown AST：

```
Markdown 文件
  │
  │ mistletoe AST 解析
  ▼
Document
  └─ H1 section
      ├─ paragraph / list / table / code
      └─ H2 section
          └─ ...
  │
  │ 语义分块
  ▼
FileChunk[]
```

**分块规则**（优先级从高到低）：

1. 先解析 frontmatter，正文单独进入 chunker
2. 按标题层级构建章节树
3. 优先让一个完整章节成为一个 chunk
4. 章节过长时，向下递归拆子章节和正文块
5. 表格拆分时重复表头
6. 代码块拆分时重复 fence
7. 列表按 item 打包
8. 最后才按行贪心拆分，并添加 `[Part X/N]`

**每个 chunk 的结构**：

```python
FileChunk:
id: str  # 唯一标识
path: str  # 文件路径
text: str  # chunk 文本（带标题骨架）
start_line: int  # 起始行号
end_line: int  # 结束行号
embedding: ndarray  # 可选的向量表示
score: float  # 检索分数
metadata: dict  # 额外元数据
```

**标题骨架**：每个 chunk 默认带上其在文档中的结构位置：

```
# 一级标题

## 当前章节

命中的正文片段

## 后续章节标题
```

这让 Agent 检索命中时不仅看到孤立段落，还能看到它在原文件中的结构位置。

#### DefaultFileChunker

用于非 Markdown 文件（如 JSONL）：

- 按字节大小切分（默认 chunk_size）
- 保留少量 overlap
- 对 Markdown 内容会避免把 `[[wikilink]]` 从中间切开

### 5.6 Agent Wrapper（Agent 封装）

ReMe 支持两种 Agent wrapper，用于需要 LLM 的步骤：

| Wrapper       | 用途                  | 配置                          |
|---------------|---------------------|-----------------------------|
| `agentscope`  | 通用 AgentScope agent | 用于 auto_memory、auto_dream 等 |
| `claude_code` | Claude Code 集成      | 用于 Claude Code 插件的 ReAct 循环 |

**AgentScope Wrapper 配置**：

```yaml
agent_wrapper:
  default:
    backend: agentscope
    as_llm: default
    permission_mode: bypass          # 工具权限模式
    react_config:
      max_iters: 30                  # ReAct 最大迭代次数
    context_config:
      trigger_ratio: 0.8             # 上下文压缩触发阈值
      reserve_ratio: 0.1             # 保留比例
      tool_result_limit: 50000       # 工具输出截断限制
```

**ReAct 循环**：Agent wrapper 提供 ReAct（Reasoning + Acting）循环，最大迭代次数可配置。每轮迭代中，Agent 可以：

1. 思考（Reasoning）：分析当前状态
2. 行动（Acting）：调用工具（如 read、write、search）
3. 观察（Observation）：获取工具返回结果
4. 重复直到完成或达到最大迭代次数

### 5.7 LLM/Embedding 封装

#### as_llm（LLM 封装）

```yaml
as_llm:
  default:
    backend: ${LLM_BACKEND:-openai}    # OpenAI 兼容接口
    model: ${LLM_MODEL_NAME:-qwen3.7-plus}
    stream: true
    context_size: 200000
    max_retries: 3
    credential:
      api_key: ${LLM_API_KEY:-}
      base_url: ${LLM_BASE_URL:-}
    parameters:
      max_tokens: 65536
      thinking_enable: false
```

#### as_embedding（Embedding 封装）

```yaml
as_embedding:
  default:
    backend: ${EMBEDDING_BACKEND:-openai}
    model: ${EMBEDDING_MODEL_NAME:-text-embedding-v4}
    dimensions: 1024
    credential:
      api_key: ${EMBEDDING_API_KEY:-}
      base_url: ${EMBEDDING_BASE_URL:-https://dashscope.aliyuncs.com/compatible-mode/v1}
```

---

## 6. 记忆流水线详解

### 6.1 完整记忆流转

```text
                        ┌─────────────────────────────────────────┐
                        │           Auto Memory                    │
对话消息 ───────────────→│  session/dialog/*.jsonl (原始对话)        │
                        │  daily/YYYY-MM-DD/<session>.md (卡片)    │
                        │  daily/YYYY-MM-DD.md (当天索引)           │
                        └─────────────────────────────────────────┘
                                         │
                        ┌─────────────────────────────────────────┐
                        │          Auto Resource                   │
外部资源 ───────────────→│  resource/YYYY-MM-DD/<file> (原始材料)   │
                        │  daily/YYYY-MM-DD/<resource>.md (卡片)   │
                        └─────────────────────────────────────────┘
                                         │
                                         ▼
                        ┌─────────────────────────────────────────┐
                        │           Auto Dream                     │
                        │  ┌─────────────────────────────────┐    │
                        │  │ 1. Extract                       │    │
                        │  │    扫描 changed daily → LLM 抽取  │    │
                        │  │    → units[] + topics[]           │    │
                        │  ├─────────────────────────────────┤    │
                        │  │ 2. Integrate                     │    │
                        │  │    对每个 unit:                   │    │
                        │  │    node_search → 去重判断          │    │
                        │  │    → CREATE/CORROBORATE/REFINE/   │    │
                        │  │      CORRECT                      │    │
                        │  │    → digest/*.md                  │    │
                        │  ├─────────────────────────────────┤    │
                        │  │ 3. Topics                        │    │
                        │  │    去重筛选 → interests.yaml       │    │
                        │  ├─────────────────────────────────┤    │
                        │  │ 4. Finish                        │    │
                        │  │    checkpoint → file_catalog      │    │
                        │  └─────────────────────────────────┘    │
                        └─────────────────────────────────────────┘
                                         │
                        ┌─────────────────────────────────────────┐
                        │          Proactive                       │
                        │  读取 interests.yaml                     │
                        │  → 上层 Agent 决定是否主动提醒用户         │
                        └─────────────────────────────────────────┘
```

### 6.2 Auto Memory 详解

**功能**：将对话消息沉淀为 daily 记忆卡片。

**输入**：

```json
{
  "session_id": "chat-demo",
  "messages": [
    {
      "role": "user",
      "content": "我偏好把项目经验沉淀成 Markdown。"
    },
    {
      "role": "assistant",
      "content": "已记录。"
    }
  ],
  "memory_hint": "记录用户偏好",
  "date": "2026-07-14"
}
```

**处理流程**：

1. 保存原始对话到 `session/dialog/<session_id>.jsonl`
2. 调用 LLM 分析对话，提取值得长期记忆的内容
3. 生成 daily 记忆卡片 `daily/YYYY-MM-DD/<session_id>.md`
4. 刷新当天索引页 `daily/YYYY-MM-DD.md`

**记忆卡片示例**：

```markdown
---
name: 用户偏好：Markdown 项目经验
description: 用户偏好将项目经验沉淀为 Markdown 格式
source_conversation: [[session/dialog/chat-demo.jsonl]]
---

用户明确表示偏好将项目经验沉淀为 Markdown 格式。

## 关键事实

- 偏好格式：Markdown
- 适用场景：项目经验沉淀

## 来源

- 对话时间：2026-07-14
- Session ID：chat-demo
```

**记录什么**（不是流水账）：

- 用户偏好：风格、协作习惯、长期要求
- 关键事实：项目背景、数字、结论、限制
- 过程决定：发生了什么、为什么这么选
- 当前状态：做到哪一步、卡在哪里
- 可复用经验：命令、流程、排查方法

### 6.3 Auto Dream 详解

**功能**：将 daily 记忆沉淀为长期 digest 节点。

**四个阶段**：

#### 6.3.1 Extract（提取）

`dream_extract_step` 做三件事：

1. 刷新当天索引页
2. 扫描 daily 输入，与 `file_catalog: dream` 中的 mtime 对比
3. 只把 changed files 交给 LLM，全局抽取 `units` 和 `topics`

**Unit 结构**：

```python
{
    "name": "Markdown 项目经验沉淀偏好",
    "bucket": "personal",  # personal / procedure / wiki
    "summary": "用户偏好将项目经验以 Markdown 格式记录和管理",
    "paths": ["daily/2026-07-14/chat-demo.md"]
}
```

**Topic 结构**：

```python
{
    "title": "记忆格式偏好确认",
    "reason": "用户明确表达了对 Markdown 格式的偏好",
    "evidence": "daily/2026-07-14/chat-demo.md",
    "keywords": ["markdown", "偏好", "项目经验"],
    "paths": ["daily/2026-07-14/chat-demo.md"]
}
```

#### 6.3.2 Integrate（整合）

`dream_integrate_step` 对每个 unit 独立调用 Agent，暴露工具集：

```
node_search, read, frontmatter_read, write, edit, frontmatter_update
```

**四种整合动作**：

| 动作            | 条件          | 操作                       |
|---------------|-------------|--------------------------|
| `CREATE`      | 没有相同抽象      | 创建新的 digest 节点           |
| `CORROBORATE` | 同一记忆再次出现    | 追加 `derived_from::` 来源链接 |
| `REFINE`      | 新材料补充了边界/细节 | 在合适段落插入补充内容              |
| `CORRECT`     | 新材料修正了旧节点   | 用来源链接标出修正依据              |

**链接策略**：

- 来源边：`derived_from:: [[daily/2026-07-14/chat-demo.md]]`
- 关联边：`relates_to:: [[digest/wiki/其他节点.md]]`
- **只增不删**：不删除已有 wikilink 或 derived_from

#### 6.3.3 Topics（主题筛选）

`dream_topics_step` 将 Extract 产生的 topic candidates 变成最终的 `interests.yaml`：

- 读取当天和历史 interests.yaml
- 去重（最近 N 天内出现过的相似主题会被过滤）
- 默认最多写 3 个 topic
- 有 LLM 时做智能选择，无 LLM 时做本地规范化去重

#### 6.3.4 Finish（收尾）

`dream_finish_step` 负责：

1. 将成功处理的 paths 写入 `file_catalog: dream`
2. 将 interests.yaml 和 day-index 也写入 catalog
3. 持久化 dream catalog
4. 返回包含各计数的摘要

**失败重试**：失败路径不会被 checkpoint，下次 auto_dream 仍会重新处理。

### 6.4 Auto Resource 详解

**功能**：解读外部资源文件为 daily 资源卡片。

**支持格式**：md, txt, json, jsonl, csv, yaml, html

**处理流程**：

1. 资源文件放入 `resource/YYYY-MM-DD/`
2. 后台 `resource_watch_loop` 检测变更
3. 调用 LLM 解读资源内容
4. 生成 daily 资源卡片 `daily/YYYY-MM-DD/<生成名>.md`
5. 卡片通过 `source_resource` 关联原始文件

### 6.5 Proactive 详解

**功能**：读取当天兴趣主题，暴露给上层 Agent。

**不做的事**：

- 不重新分析 daily
- 不调用 LLM
- 不修改任何文件
- 不判断是否应该主动打扰用户

**只做的事**：读取 `daily/<date>/interests.yaml`，返回结构化 topics。

---

## 7. 高级检索系统

### 7.1 混合检索架构

```text
query ──→ BM25 keyword_search ──→ candidates[]
query ──→ vector_search (if enabled) ──→ candidates[]
                │
                ▼
        RRF 融合排序 (Reciprocal Rank Fusion)
                │
                ▼
        min_score 过滤 → 截断到 limit → expand_links (wikilink 展开)
                │
                ▼
        Response (answer + metadata)
```

### 7.2 RRF 融合算法

Reciprocal Rank Fusion 不直接比较 BM25 分数和 cosine 分数，而是比较两个列表里的名次：

```
fused_score = vector_weight / (60 + vector_rank)
            + keyword_weight / (60 + keyword_rank)
```

**默认参数**：

- `vector_weight = 0.7`（启用 embedding 后语义召回权重更高）
- `keyword_weight = 0.3`（关键词仍能把精确词命中的 chunk 拉上来）
- `candidate_multiplier = 5.0`（候选数 = limit × 5）

**边界情况**：

- 只有 BM25 有结果 → 直接返回 BM25 排名
- 只有向量有结果 → 直接返回向量排名
- 两边都有 → RRF 融合

### 7.3 渐进式链接展开

Memory Search 的"渐进式"不是一次把全库内容塞进结果，而是分三层展开：

**第一层：chunk 召回**

- 返回最相关的 `limit` 个文本片段
- 每个结果带 `path:start_line-end_line`

**第二层：文件定位**

- 可以继续用 `read` 精读原文件的指定行范围

**第三层：链接邻居**

- 对命中文件调用 `expand_links()`
- 展开最多 `max_links_per_direction=10` 个 outlinks 和 inlinks
- 展开数据来自 `file_graph`，不是重新扫文件

**链接展开的数据流**：

```
命中 chunk
  → chunk.path
  → file_store.get_outlinks(path)  →  FileLink[]
  → file_store.get_inlinks(path)   →  FileLink[]
  → file_store.get_nodes(neighbor_paths)  →  FileNode[]
  → 渲染邻居的 path、name、description、predicate、anchor
```

### 7.4 Node Search（节点级召回）

`node_search` 是为 auto_dream 的 Integrate 阶段设计的 digest-only 召回：

| 特性   | search          | node_search   |
|------|-----------------|---------------|
| 用途   | 面向外部问答          | 面向 dream 集成   |
| 返回内容 | chunk 片段 + 链接展开 | digest 节点级摘要  |
| 展开链接 | 是               | 否             |
| 适用场景 | 用户查询            | 去重判断 + 相关链接发现 |

### 7.5 搜索结果格式

**文本格式**（给人看）：

```
========== daily/2026-07-14/chat-demo.md:12-28 [score=0.0317 keyword=4.8120] ==========
用户明确表示偏好将项目经验沉淀为 Markdown 格式...

  outlinks (2):
    -> digest/wiki/markdown-best-practices.md  name="Markdown 最佳实践"
      via predicate=related
  inlinks (1):
    <- daily/2026-07-13.md  name="2026-07-13 工作记录"
      via plain
```

**元数据格式**（给程序看）：

```json
{
  "results": [
    ...
  ],
  "link_expansion": {
    ...
  },
  "counts": {
    "vector": 0,
    "keyword": 5,
    "hybrid": false,
    "returned": 5
  }
}
```

---

## 8. 服务暴露与集成

### 8.1 HTTP Service

所有非 StreamJob 注册为 `POST /<job.name>`：

```bash
# 请求
curl -s http://127.0.0.1:2333/search \
  -H 'Content-Type: application/json' \
  -d '{"query":"用户偏好","limit":5}'

# 响应
{
  "success": true,
  "answer": "========== ...",
  "metadata": {...}
}
```

StreamJob 注册为 SSE（Server-Sent Events）。

### 8.2 MCP Service

使用 `fastmcp` 库实现，支持三种传输模式：

| 模式             | 启动命令                                                   | 端点      | 适用场景             |
|----------------|--------------------------------------------------------|---------|------------------|
| SSE            | `reme start backend=mcp`                               | `/sse/` | 远程服务、Claude Code |
| StdIO          | `reme start backend=mcp mcp.transport=stdio`           | 标准输入输出  | 本地进程间            |
| StreamableHTTP | `reme start backend=mcp mcp.transport=streamable-http` | `/mcp`  | 新版 MCP 标准        |

**MCP Tool 注册规则**：

- 所有非 StreamJob 且 `enable_serve: true` 的 Job 自动注册为 MCP Tool
- StreamJob 当前不支持 MCP 暴露
- BackgroundJob 构造时强制 `enable_serve=False`，不会暴露

### 8.3 MCP 暴露的工具列表

当 ReMe 以 MCP 模式启动时，以下 Job 自动注册为 MCP Tool：

| MCP Tool Name        | 对应 Job             | 功能                             |
|----------------------|--------------------|--------------------------------|
| `search`             | search             | 混合检索（BM25 + 向量 + wikilink 展开）  |
| `auto_memory`        | auto_memory        | 对话消息 → daily 记忆卡片              |
| `auto_memory_cc`     | auto_memory_cc     | Claude Code session → daily 记忆 |
| `auto_resource`      | auto_resource      | 资源文件 → daily 资源卡片              |
| `auto_dream`         | auto_dream         | daily → digest 长期记忆沉淀          |
| `proactive`          | proactive          | 读取当日兴趣主题                       |
| `read`               | read               | 读取 Markdown 文件                 |
| `write`              | write              | 写入 Markdown 文件                 |
| `daily_write`        | daily_write        | 写入每日笔记                         |
| `edit`               | edit               | 查找替换编辑                         |
| `delete`             | delete             | 删除文件                           |
| `move`               | move               | 移动/重命名文件                       |
| `list`               | list               | 列出目录内容                         |
| `stat`               | stat               | 获取文件状态                         |
| `traverse`           | traverse           | 沿 wikilink 图谱遍历                |
| `node_search`        | node_search        | digest 节点级召回                   |
| `reindex`            | reindex            | 重建索引                           |
| `frontmatter_read`   | frontmatter_read   | 读取 frontmatter                 |
| `frontmatter_update` | frontmatter_update | 更新 frontmatter                 |
| `frontmatter_delete` | frontmatter_delete | 删除 frontmatter 键               |
| `daily_list`         | daily_list         | 列出某天的笔记                        |
| `daily_reindex`      | daily_reindex      | 重建当天索引页                        |
| `version`            | version            | 版本信息                           |
| `health_check`       | health_check       | 健康检查                           |
| `help`               | help               | 列出所有 Job                       |

### 8.4 MCP Channel 机制

MCP 模式下支持 Channel 机制，用于 workspace 变更通知推送到客户端。当文件发生变更时，ReMe 可以通过 MCP Channel 通知连接的客户端。

---

## 9. 部署架构与运维

### 9.1 当前部署结构

```
reme-server/
├── .env                    # API Key、LLM/Embedding 配置
├── scripts/
│   ├── start_mcp.bat       # Windows MCP 启动脚本
│   └── start_mcp.sh        # Linux MCP 启动脚本
├── docker/
│   ├── Dockerfile          # Docker 构建文件
│   └── docker-compose.yml  # Docker Compose 编排
├── requirements.txt        # Python 依赖
└── .reme/                  # 默认 workspace（运行时生成）
    ├── metadata/
    ├── session/
    ├── resource/
    ├── daily/
    └── digest/
```

### 9.2 环境变量

ReMe 配置使用 YAML + `${VAR:-default}` 环境变量展开语法。实际读取的环境变量：

| 变量名                    | 用途                    | 必需                        |
|------------------------|-----------------------|---------------------------|
| `LLM_API_KEY`          | LLM 服务 API Key        | auto_memory/auto_dream 需要 |
| `LLM_BASE_URL`         | LLM 服务 Base URL       | 同上                        |
| `LLM_MODEL_NAME`       | LLM 模型名称              | 否（默认 qwen3.7-plus）        |
| `EMBEDDING_API_KEY`    | Embedding 服务 API Key  | 启用向量搜索时需要                 |
| `EMBEDDING_BASE_URL`   | Embedding 服务 Base URL | 同上                        |
| `EMBEDDING_MODEL_NAME` | Embedding 模型名称        | 否（默认 text-embedding-v4）   |
| `REME_API_KEY`         | 服务访问密钥                | 否                         |

### 9.3 Docker 部署

```yaml
version: '3.8'

services:
  reme-server:
    build:
      context: ..
      dockerfile: docker/Dockerfile
    container_name: hivemind-reme
    ports:
      - "8002:8002"
    volumes:
      - reme-workspace:/app/.reme
    env_file:
      - ../.env
    restart: unless-stopped

volumes:
  reme-workspace:
```

### 9.4 关键运维注意事项

| 注意事项           | 说明                                                              |
|----------------|-----------------------------------------------------------------|
| workspace 持久化  | `.reme/` 目录必须持久化，否则记忆丢失                                         |
| LLM 服务可用性      | auto_memory 和 auto_dream 依赖 LLM，LLM 不可用时仅 read/write/search 可用  |
| Embedding 服务可选 | 默认关闭向量搜索，BM25 独立工作                                              |
| SSE 端点路径       | MCP SSE 端点是 `/sse/`（注意尾部斜杠）                                     |
| 后台 Job         | index_update_loop、resource_watch_loop、digest_watch_loop 在后台持续运行 |
| 定时 Job         | dream_cron 默认每天 23:00 运行 auto_dream                             |

---

## 10. 与 HiveMind 集成分析

### 10.1 当前集成方式

```text
┌──────────────────────────────────────────────────────────┐
│  HiveMind (Java)                                         │
│                                                          │
│  TdAgentReMeService                                      │
│    ├── ReMeClient (agentscope-extensions-reme)           │
│    │     └── HTTP POST → http://localhost:8002/<job>     │
│    └── compactSessionHistory() / add() / retrieve()      │
│                                                          │
│  TdAgentMemoryManager                                    │
│    └── 使用 TdAgentReMeService 管理记忆生命周期            │
│                                                          │
│  ContextCompressor                                       │
│    └── 使用 TdAgentReMeService 压缩对话历史                │
└──────────────────────────────────────────────────────────┘
         │ HTTP
         ▼
┌──────────────────────────────────────────────────────────┐
│  reme-server (Python)                                    │
│                                                          │
│  reme start backend=http                                 │
│    ├── Service: HttpService (FastAPI + uvicorn)          │
│    ├── Jobs: auto_memory, search, read, write, ...       │
│    ├── Components: LocalFileStore, BM25Index, ...        │
│    └── Workspace: .reme/ (session → daily → digest)      │
└──────────────────────────────────────────────────────────┘
```

### 10.2 当前使用的 Job

| Job           | Java 调用方                                     | 用途               |
|---------------|----------------------------------------------|------------------|
| `search`      | `TdAgentReMeService.retrieve()`              | 语义检索长期记忆         |
| `auto_memory` | `TdAgentReMeService.add()`                   | 将对话消息写入 daily 记忆 |
| (内部)          | `TdAgentReMeService.compactSessionHistory()` | 压缩对话历史           |

### 10.3 推荐升级路径：HTTP → MCP

**不替换存储后端**，仅将通信协议从 HTTP 改为 MCP：

```text
┌─────────────────────────────────────────────────┐
│  HiveMind (Java)                                │
│                                                 │
│  TdAgentReMeService                             │
│    ├── McpReMeClient (新增)                     │
│    │     └── MCP SSE → http://localhost:8002/sse│
│    └── ReMeClient (保留, 作为 fallback)         │
└─────────────────────────────────────────────────┘
         │ MCP SSE (首选) / HTTP (降级)
         ▼
┌─────────────────────────────────────────────────┐
│  reme-server (Python)                           │
│                                                 │
│  reme start backend=mcp mcp.transport=sse       │
│    ├── Service: MCPService (FastMCP)            │
│    ├── Jobs: 同上                                │
│    └── Workspace: .reme/ (文件系统不变)           │
└─────────────────────────────────────────────────┘
```

**优势**：

- 零存储改动，不破坏 ReMe 的文件优先设计
- 获得 MCP 生态的所有能力（Tool 注册、Channel 通知等）
- HTTP fallback 保留，降级无风险
- 实现工作量小（Java 侧新增 McpReMeClient + 配置）

### 10.4 AgentScope-Java MCP 客户端能力

| 类                       | 职责                                    |
|-------------------------|---------------------------------------|
| `McpClientBuilder`      | 流式构建器，创建 MCP 客户端                      |
| `McpClientWrapper`      | 抽象基类：生命周期、工具缓存、callTool/listTools     |
| `McpSyncClientWrapper`  | 同步实现                                  |
| `McpAsyncClientWrapper` | 异步/响应式实现                              |
| `McpTool`               | 将 MCP tool 适配为 AgentScope 的 AgentTool |

**使用示例**：

```java
McpClientWrapper client = McpClientBuilder.create("reme-memory")
        .sseTransport("http://localhost:8002/sse")
        .timeout(Duration.ofSeconds(60))
        .buildSync();

client.

initialize().

block();

List<McpSchema.Tool> tools = client.listTools().block();

McpSchema.CallToolResult result = client.callTool(
        "search",
        Map.of("query", "用户偏好", "limit", 5)
).block();

client.

close();
```

### 10.5 关键发现

- **`agentscope-extensions-reme` 不包含 MCP 能力**：仅提供 HTTP 客户端 `ReMeClient`
- **MCP 客户端在 `agentscope-core` 中**：需要直接使用 `McpClientBuilder`
- **HiveMind 当前无任何 MCP 客户端使用**：`SysMcpPO`/`SysMcpController` 仅为 CRUD 配置层

---

## 11. 附录：完整组件清单

### 11.1 ComponentEnum 枚举

| 枚举值               | 说明           | 默认组件                    |
|-------------------|--------------|-------------------------|
| `SERVICE`         | 服务层          | http                    |
| `JOB`             | 任务           | -                       |
| `STEP`            | 步骤           | -                       |
| `FILE_STORE`      | 文件存储协调       | local                   |
| `FILE_GRAPH`      | wikilink 图谱  | local                   |
| `KEYWORD_INDEX`   | 关键词索引        | bm25                    |
| `FILE_CHUNKER`    | 文件分块         | markdown, default       |
| `FILE_CATALOG`    | 文件变更追踪       | local                   |
| `EMBEDDING_STORE` | 向量存储         | local                   |
| `AS_LLM`          | LLM 封装       | openai                  |
| `AS_EMBEDDING`    | Embedding 封装 | openai                  |
| `AGENT_WRAPPER`   | Agent 封装     | agentscope, claude_code |
| `TOKENIZER`       | 分词器          | regex                   |
| `CLIENT`          | 客户端          | http, mcp               |

### 11.2 Job 类型分布

```
default.yaml jobs
  │
  ├── background (后台循环)
  │   ├── index_update_loop     # 文件变更 → 索引更新
  │   ├── resource_watch_loop   # 资源变更 → auto_resource
  │   └── digest_watch_loop     # digest 变更 → catalog 更新
  │
  ├── cron (定时)
  │   └── dream_cron            # 每天 23:00 运行 auto_dream
  │
  └── base (请求型)
      ├── version / help / health_check     # 通用
      ├── search / node_search / traverse   # 检索
      ├── read / write / edit / delete      # 文件 I/O
      ├── move / list / stat                # 文件操作
      ├── daily_list / daily_reindex / daily_write  # Daily 操作
      ├── frontmatter_read / frontmatter_update / frontmatter_delete
      ├── auto_memory / auto_memory_cc      # 对话记忆
      ├── auto_resource                     # 资源解读
      ├── auto_dream                        # 长期沉淀
      └── proactive                         # 主动读取
```

### 11.3 Step 分类

| 目录              | Step                                                                                           | 功能          |
|-----------------|------------------------------------------------------------------------------------------------|-------------|
| `common/`       | version_step, help_step, health_check_step                                                     | 通用工具        |
| `file_io/`      | read_step, write_step, edit_step, delete_step, move_step, list_step, stat_step                 | 文件读写        |
| `file_io/`      | daily_list_step, daily_reindex_step, daily_write_step                                          | Daily 操作    |
| `file_io/`      | frontmatter_read_step, frontmatter_update_step, frontmatter_delete_step                        | Frontmatter |
| `file_io/`      | read_image_step                                                                                | 图片读取        |
| `index/`        | search_step, node_search_step, bm25_search_step, vector_search_step                            | 检索          |
| `index/`        | init_changes_step, watch_changes_step, update_changes_step                                     | 索引维护        |
| `index/`        | traverse_step, clear_store_step, draft_step                                                    | 图谱/重建       |
| `evolve/`       | auto_memory_step, auto_memory_cc_step, auto_resource_step                                      | 自进化         |
| `evolve/dream/` | dream_extract_step, dream_integrate_step, dream_topics_step, dream_finish_step, proactive_step | Dream 流水线   |
| `channel/`      | channel_notify_step, claim_channel_step                                                        | MCP Channel |
| `transfer/`     | upload_step, download_step                                                                     | 传输          |

### 11.4 数据模型

#### FileNode

```python
class FileNode:
    path: str  # workspace-relative 路径
    name: str  # frontmatter name
    description: str  # frontmatter description
    frontmatter: dict  # 完整 frontmatter
    st_mtime: float  # 文件修改时间戳
```

#### FileChunk

```python
class FileChunk:
    id: str  # 唯一标识（基于路径+行号生成）
    path: str  # 文件路径
    text: str  # chunk 文本（带标题骨架）
    start_line: int  # 起始行号（1-based）
    end_line: int  # 结束行号（1-based）
    embedding: ndarray  # 可选的向量表示
    score: float  # 检索分数
    metadata: dict  # 额外元数据
```

#### FileLink

```python
class FileLink:
    source_path: str  # 源文件路径
    target_path: str  # 目标文件路径
    predicate: str  # 关系名（如 "derived_from", "relates_to"）
    anchor: str  # 锚点（如 "#section"）
```

#### Response

```python
class Response:
    answer: str  # 人可读的文本结果
    success: bool  # 是否成功
    metadata: dict  # 结构化元数据
```

---

## 参考资料

| 资源                  | 路径/URL                                                                 |
|---------------------|------------------------------------------------------------------------|
| ReMe 论文             | arXiv:2512.10696v2                                                     |
| ReMe 源码             | `.lib-repo/ReMe/`                                                      |
| ReMe 框架文档           | `.lib-repo/ReMe/docs/zh/framework.md`                                  |
| Memory as File 文档   | `.lib-repo/ReMe/docs/zh/memory_as_file.md`                             |
| Memory Search 文档    | `.lib-repo/ReMe/docs/zh/memory_search.md`                              |
| Auto Memory 文档      | `.lib-repo/ReMe/docs/zh/auto_memory.md`                                |
| Auto Dream 文档       | `.lib-repo/ReMe/docs/zh/auto_dream.md`                                 |
| Auto Link 文档        | `.lib-repo/ReMe/docs/zh/auto_link.md`                                  |
| Auto Resource 文档    | `.lib-repo/ReMe/docs/zh/auto_resource.md`                              |
| Proactive 文档        | `.lib-repo/ReMe/docs/zh/proactive.md`                                  |
| Quick Start 文档      | `.lib-repo/ReMe/docs/zh/quick_start.md`                                |
| 默认配置                | `.lib-repo/ReMe/reme/config/default.yaml`                              |
| HiveMind ReMe 集成    | `hivemind-agent-engine/.../agents/memory/reme/TdAgentReMeService.java` |
| AgentScope-Java MCP | `io.agentscope.core.tool.mcp` 包                                        |
