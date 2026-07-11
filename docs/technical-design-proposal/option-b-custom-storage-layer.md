# 方案 B：保留 ReMe 记忆引擎 + 自定义存储层

> **文档版本**: 1.0
> **创建日期**: 2026-07-12
> **状态**: 设计方案

---

## 1. 方案概述

保留 ReMe 的记忆演化引擎（auto_memory、auto_dream、search 等 Job 和 Step），但将其文件系统存储层（LocalFileStore）替换为基于 MongoDB 的自定义实现。记忆内容以结构化文档形式存储在 MongoDB 中，支持按用户隔离、全文检索和向量检索。

```
┌─────────────────────────────────────────────────────────┐
│  HiveMind (Java)                                        │
│                                                         │
│  TdAgentReMeService                                     │
│    ├── McpReMeClient (MCP SSE)                          │
│    └── 或直接调用 ReMe Step (嵌入模式)                    │
│                                                         │
│  前端管理界面                                             │
│    └── 记忆 CRUD API (直接查询 MongoDB)                   │
└─────────────────────────────────────────────────────────┘
         │ MCP SSE / 内部调用
         ▼
┌─────────────────────────────────────────────────────────┐
│  reme-server (Docker)                                   │
│                                                         │
│  Jobs & Steps (不变)                                     │
│    ├── auto_memory_step → daily_write_step              │
│    ├── auto_dream (extract → integrate → topics)        │
│    ├── search_step                                      │
│    └── read_step / write_step / edit_step               │
│                                                         │
│  Components (替换)                                       │
│    ├── MongoFileStore (替换 LocalFileStore)              │
│    │   ├── MongoKeywordIndex (替换 BM25Index)           │
│    │   ├── MongoFileGraph (替换 LocalFileGraph)         │
│    │   └── MongoEmbeddingStore (替换 LocalEmbeddingStore)│
│    ├── AgentWrapper (不变)                               │
│    └── FileChunker (不变)                                │
│                                                         │
│  文件层 (可选保留)                                        │
│    └── workspace/ (Markdown 文件, 用于可读性)             │
└─────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────┐
│  MongoDB                                                │
│                                                         │
│  数据库: hivemind_memory                                │
│    ├── memory_chunks   (文档分块 + 向量)                  │
│    ├── memory_files    (文件节点 + 元数据)                │
│    ├── memory_links    (wikilink 图谱)                   │
│    └── memory_catalogs (变更检查点)                       │
└─────────────────────────────────────────────────────────┘
```

---

## 2. 核心设计决策

### 2.1 为什么选择 MongoDB

| 需求 | MongoDB 优势 |
|------|-------------|
| 文档存储 | 原生 JSON/BSON，与 ReMe 的 FileChunk/FileNode 结构天然匹配 |
| 全文检索 | MongoDB Atlas Search 或内置 text index，可替代 BM25 |
| 向量检索 | MongoDB Atlas Vector Search（7.0+），可替代 LocalEmbeddingStore |
| 用户隔离 | Collection 级别或 document 级别 `userId` 字段 + 索引 |
| 已有依赖 | HiveMind 已使用 MongoDB（Spring Data MongoDB） |
| 水平扩展 | Replica Set + Sharding |
| 运维 | 成熟的备份、监控、恢复工具 |

### 2.2 双层存储策略

```
┌───────────────────────────────────────────────────┐
│  应用层                                            │
│  Step 调用 file_store.upsert() / search() / ...   │
└───────────────────────┬───────────────────────────┘
                        │
            ┌───────────┴───────────┐
            │                       │
            ▼                       ▼
┌─────────────────────┐  ┌─────────────────────┐
│  MongoDB (主存储)    │  │  文件系统 (可选副本)  │
│                     │  │                     │
│  - 结构化文档        │  │  - Markdown 文件    │
│  - 全文索引          │  │  - 人类可读         │
│  - 向量索引          │  │  - 直接编辑         │
│  - 用户隔离          │  │  - 版本管理         │
│  - 可查询/可聚合     │  │  - 备份/迁移        │
└─────────────────────┘  └─────────────────────┘
```

**策略**：MongoDB 是真相源（Source of Truth），文件系统是可选的只读副本，用于人类浏览和调试。写入操作只通过 MongoDB，文件副本由后台 Job 异步生成。

---

## 3. MongoDB 数据模型

### 3.1 memory_chunks 集合

存储 ReMe 的 FileChunk，这是搜索的核心数据。

```json
{
  "_id": "chunk_<hash>",
  "userId": "user_123",
  "sessionId": "session_abc",
  "workspaceId": "user_123::session_abc",
  "path": "daily/2026-07-12/session_abc.md",
  "startLine": 12,
  "endLine": 28,
  "text": "用户偏好使用中文交流，喜欢简洁的回答风格...",
  "metadata": {
    "name": "Session ABC Memory",
    "description": "对话记忆卡片",
    "date": "2026-07-12",
    "source": "auto_memory"
  },
  "embedding": [0.0123, -0.0456, ...],  // 1024 维 float32 向量
  "scores": {},
  "createdAt": ISODate("2026-07-12T10:30:00Z"),
  "updatedAt": ISODate("2026-07-12T10:30:00Z")
}
```

**索引**：
```javascript
// 用户隔离查询
db.memory_chunks.createIndex({ "workspaceId": 1, "path": 1 })

// 全文检索 (替代 BM25)
db.memory_chunks.createIndex({ "text": "text" })

// 向量检索 (MongoDB Atlas Vector Search)
// 需要 Atlas Search 索引定义:
{
  "fields": [{
    "type": "vector",
    "path": "embedding",
    "numDimensions": 1024,
    "similarity": "cosine"
  }]
}

// 日期范围查询
db.memory_chunks.createIndex({ "metadata.date": 1 })
```

### 3.2 memory_files 集合

存储 ReMe 的 FileNode，表示文件级别的元数据。

```json
{
  "_id": "file_<path_hash>",
  "userId": "user_123",
  "workspaceId": "user_123::session_abc",
  "path": "daily/2026-07-12/session_abc.md",
  "name": "Session ABC Memory",
  "description": "对话记忆卡片",
  "chunkIds": ["chunk_001", "chunk_002", "chunk_003"],
  "stMtime": 1720789800.0,
  "metadata": {
    "date": "2026-07-12",
    "session_id": "session_abc"
  },
  "createdAt": ISODate("2026-07-12T10:30:00Z"),
  "updatedAt": ISODate("2026-07-12T10:30:00Z")
}
```

**索引**：
```javascript
db.memory_files.createIndex({ "workspaceId": 1, "path": 1 }, { unique: true })
db.memory_files.createIndex({ "userId": 1, "metadata.date": 1 })
```

### 3.3 memory_links 集合

存储 wikilink 图谱边。

```json
{
  "_id": "link_<hash>",
  "sourcePath": "daily/2026-07-12/session_abc.md",
  "targetPath": "digest/personal/user_preferences.md",
  "anchor": null,
  "predicate": "related",
  "scope": "real",           // real = 目标文件存在, pending = 目标不存在
  "workspaceId": "user_123::session_abc",
  "createdAt": ISODate("2026-07-12T10:30:00Z")
}
```

**索引**：
```javascript
db.memory_links.createIndex({ "sourcePath": 1 })
db.memory_links.createIndex({ "targetPath": 1 })
db.memory_links.createIndex({ "workspaceId": 1, "scope": 1 })
```

### 3.4 memory_catalogs 集合

存储文件变更检查点（替代 file_catalog）。

```json
{
  "_id": "catalog_<name>_<workspace>",
  "catalogName": "default",
  "workspaceId": "user_123::session_abc",
  "entries": {
    "daily/2026-07-12/session_abc.md": {
      "mtime": 1720789800.0,
      "size": 2048
    }
  },
  "updatedAt": ISODate("2026-07-12T10:30:00Z")
}
```

---

## 4. 核心组件实现

### 4.1 MongoFileStore

替代 `LocalFileStore`，实现 `BaseFileStore` 接口。

```python
@R.register("mongo")
class MongoFileStore(BaseFileStore):
    """MongoDB-backed file store. Replaces LocalFileStore."""

    def __init__(
        self,
        mongo_uri: str = "mongodb://localhost:27017",
        database: str = "hivemind_memory",
        keyword_index: str = "mongo",
        file_graph: str = "mongo",
        embedding_store: str = "mongo",
        enable_file_mirror: bool = True,
        **kwargs,
    ):
        super().__init__(**kwargs)
        self.mongo_uri = mongo_uri
        self.database = database
        self.keyword_index = self.bind(keyword_index, BaseKeywordIndex)
        self.file_graph = self.bind(file_graph, BaseFileGraph)
        self.embedding_store = self.bind(embedding_store, BaseEmbeddingStore)
        self.enable_file_mirror = enable_file_mirror
        self._client = None
        self._db = None

    async def _start(self) -> None:
        import motor.motor_asyncio
        self._client = motor.motor_asyncio.AsyncIOMotorClient(self.mongo_uri)
        self._db = self._client[self.database]
        # 确保索引存在
        await self._ensure_indexes()

    async def _ensure_indexes(self) -> None:
        chunks = self._db["memory_chunks"]
        await chunks.create_index([("workspaceId", 1), ("path", 1)])
        await chunks.create_index([("text", "text")])
        files = self._db["memory_files"]
        await files.create_index([("workspaceId", 1), ("path", 1)], unique=True)
        links = self._db["memory_links"]
        await links.create_index([("sourcePath", 1)])
        await links.create_index([("targetPath", 1)])

    async def upsert(self, files: list[tuple[FileNode, list[FileChunk]]]) -> None:
        """写入文件节点和分块到 MongoDB。"""
        for node, chunks in files:
            # 1. 删除旧 chunks
            await self._db["memory_chunks"].delete_many({
                "workspaceId": node.metadata.get("workspaceId", ""),
                "path": node.path,
            })

            # 2. 写入新 chunks
            if chunks:
                docs = [self._chunk_to_doc(c, node) for c in chunks]
                await self._db["memory_chunks"].insert_many(docs)

            # 3. 更新文件节点
            await self._db["memory_files"].update_one(
                {"workspaceId": node.metadata.get("workspaceId", ""), "path": node.path},
                {"$set": self._node_to_doc(node)},
                upsert=True,
            )

            # 4. 更新图谱
            if self.file_graph:
                await self.file_graph.upsert_nodes([node])

            # 5. 生成 embedding (可选)
            if self.embedding_store and chunks:
                await self.embedding_store.get_node_embeddings(chunks)
                for c in chunks:
                    if c.embedding is not None:
                        await self._db["memory_chunks"].update_one(
                            {"_id": f"chunk_{c.id}"},
                            {"$set": {"embedding": c.embedding.tolist()}},
                        )

            # 6. 文件镜像 (可选)
            if self.enable_file_mirror:
                await self._mirror_to_file(node, chunks)

    async def keyword_search(self, query: str, limit: int, search_filter: dict) -> list[FileChunk]:
        """MongoDB 全文检索。"""
        mongo_filter = self._build_mongo_filter(search_filter)
        mongo_filter["$text"] = {"$search": query}

        cursor = self._db["memory_chunks"].find(
            mongo_filter,
            {"score": {"$meta": "textScore"}},
        ).sort([("score", {"$meta": "textScore"})]).limit(limit)

        results = []
        async for doc in cursor:
            chunk = self._doc_to_chunk(doc)
            chunk.scores = {"keyword": doc.get("score", 0), "score": doc.get("score", 0)}
            results.append(chunk)
        return results

    async def vector_search(self, query: str, limit: int, search_filter: dict) -> list[FileChunk]:
        """MongoDB Atlas Vector Search。"""
        if not self.embedding_store:
            return []

        query_embedding = await self.embedding_store.get_embedding(query)
        if query_embedding is None:
            return []

        # Atlas Vector Search aggregation pipeline
        pipeline = [
            {
                "$vectorSearch": {
                    "index": "vector_index",
                    "path": "embedding",
                    "queryVector": query_embedding.tolist(),
                    "numCandidates": limit * 10,
                    "limit": limit,
                    **self._build_vector_filter(search_filter),
                }
            },
            {"$addFields": {"score": {"$meta": "vectorSearchScore"}}},
        ]

        results = []
        async for doc in self._db["memory_chunks"].aggregate(pipeline):
            chunk = self._doc_to_chunk(doc)
            chunk.scores = {"vector": doc.get("score", 0), "score": doc.get("score", 0)}
            results.append(chunk)
        return results

    # ... 辅助方法
```

### 4.2 MongoKeywordIndex

替代 BM25Index，使用 MongoDB 全文索引。

```python
@R.register("mongo")
class MongoKeywordIndex(BaseKeywordIndex):
    """MongoDB text index-backed keyword index."""

    def __init__(self, mongo_uri: str = "mongodb://localhost:27017",
                 database: str = "hivemind_memory", **kwargs):
        super().__init__(**kwargs)
        self.mongo_uri = mongo_uri
        self.database = database

    async def add_docs(self, docs: dict[str, str]) -> None:
        # 文档已通过 MongoFileStore.upsert() 写入 memory_chunks
        # MongoDB text index 自动更新，无需额外操作
        pass

    async def delete_docs(self, doc_ids: list[str]) -> None:
        # 文档已通过 MongoFileStore.delete() 删除
        pass

    async def retrieve(self, query: str, limit: int = 10) -> dict[str, float]:
        # 由 MongoFileStore.keyword_search() 直接处理
        return {}
```

### 4.3 MongoFileGraph

替代 LocalFileGraph，使用 MongoDB 存储图谱。

```python
@R.register("mongo")
class MongoFileGraph(BaseFileGraph):
    """MongoDB-backed wikilink graph."""

    async def upsert_nodes(self, nodes: list[FileNode]) -> None:
        for node in nodes:
            # 删除旧边
            await self._db["memory_links"].delete_many({"sourcePath": node.path})
            # 写入新边
            if node.links:
                docs = [{
                    "sourcePath": link.source_path,
                    "targetPath": link.target_path,
                    "anchor": link.target_anchor,
                    "predicate": link.predicate,
                    "scope": "real" if await self._node_exists(link.target_path) else "pending",
                    "workspaceId": node.metadata.get("workspaceId", ""),
                } for link in node.links]
                await self._db["memory_links"].insert_many(docs)

    async def get_outlinks(self, path: str, scope: LinkScopeEnum = LinkScopeEnum.REAL) -> list[FileLink]:
        query = {"sourcePath": path}
        if scope == LinkScopeEnum.REAL:
            query["scope"] = "real"
        cursor = self._db["memory_links"].find(query)
        return [self._doc_to_link(doc) async for doc in cursor]

    async def get_inlinks(self, path: str, scope: LinkScopeEnum = LinkScopeEnum.REAL) -> list[FileLink]:
        query = {"targetPath": path}
        if scope == LinkScopeEnum.REAL:
            query["scope"] = "real"
        cursor = self._db["memory_links"].find(query)
        return [self._doc_to_link(doc) async for doc in cursor]
```

### 4.4 MongoEmbeddingStore

替代 LocalEmbeddingStore，使用 MongoDB 存储 embedding。

```python
@R.register("mongo")
class MongoEmbeddingStore(BaseEmbeddingStore):
    """MongoDB-backed embedding store."""

    async def get_embeddings(self, input_text: list[str], **kwargs) -> list[np.ndarray | None]:
        results = [None] * len(input_text)
        misses = []

        # 1. 从 MongoDB 缓存中查找
        for idx, text in enumerate(input_text):
            cached = await self._db["memory_embeddings"].find_one({"text_hash": self._hash(text)})
            if cached:
                results[idx] = np.array(cached["embedding"], dtype=np.float16)
            else:
                misses.append((idx, text))

        # 2. 批量计算缺失的 embedding
        if misses:
            texts = [t for _, t in misses]
            embeddings = await self.as_embedding(texts, **kwargs)
            for (idx, text), emb in zip(misses, embeddings):
                if emb is not None:
                    arr = np.asarray(emb, dtype=np.float16)
                    results[idx] = arr
                    # 缓存到 MongoDB
                    await self._db["memory_embeddings"].update_one(
                        {"text_hash": self._hash(text)},
                        {"$set": {"embedding": arr.tolist(), "dimensions": len(arr)}},
                        upsert=True,
                    )

        return results
```

---

## 5. 文件操作生命周期

### 5.1 写入流程

```
Java 客户端                    ReMe (Docker)              MongoDB
    │                              │                        │
    │ callTool("write", {          │                        │
    │   path, name, content        │                        │
    │ })                           │                        │
    │ ──── MCP SSE ─────────────> │                        │
    │                              │                        │
    │                              ├─ write_step            │
    │                              │   └─ MongoFileStore     │
    │                              │       .upsert()        │
    │                              │                        │
    │                              │   1. chunker.chunk()  │
    │                              │      → FileNode +      │
    │                              │        FileChunk[]     │
    │                              │                        │
    │                              │   2. delete old ──────>│ memory_chunks
    │                              │      chunks            │
    │                              │                        │
    │                              │   3. insert new ──────>│ memory_chunks
    │                              │      chunks            │
    │                              │                        │
    │                              │   4. upsert node ─────>│ memory_files
    │                              │                        │
    │                              │   5. upsert links ────>│ memory_links
    │                              │                        │
    │                              │   6. embed + update ──>│ memory_chunks
    │                              │      (可选)             │ (embedding 字段)
    │                              │                        │
    │                              │   7. mirror to file    │
    │                              │      (可选)             │
    │                              │                        │
    │ <── ToolResult ──────────────│                        │
```

### 5.2 搜索流程

```
Java 客户端                    ReMe (Docker)              MongoDB
    │                              │                        │
    │ callTool("search", {         │                        │
    │   query, limit               │                        │
    │ })                           │                        │
    │ ──── MCP SSE ─────────────> │                        │
    │                              │                        │
    │                              ├─ search_step           │
    │                              │   ├─ keyword_search()  │
    │                              │   │   └─ $text ───────>│ text index
    │                              │   │   <── results ─────│
    │                              │   │                    │
    │                              │   ├─ vector_search()   │
    │                              │   │   └─ $vectorSearch>│ vector index
    │                              │   │   <── results ─────│
    │                              │   │                    │
    │                              │   ├─ RRF 融合          │
    │                              │   ├─ expand_links()    │
    │                              │   │   └─ get_outlinks>│ memory_links
    │                              │   │   └─ get_inlinks─>│ memory_links
    │                              │   └─ 构建 Response     │
    │                              │                        │
    │ <── ToolResult ──────────────│                        │
```

---

## 6. 前端记忆管理 API

由于数据在 MongoDB 中，HiveMind Java 侧可以直接查询，无需通过 MCP 代理：

```java
@RestController
@RequestMapping("/api/memory")
@RequiredArgsConstructor
public class MemoryController {

    private final MongoTemplate mongoTemplate;

    /**
     * 列出用户的所有记忆文件。
     */
    @GetMapping("/files")
    public Result<List<MemoryFileVO>> listFiles(
            @RequestParam String userId,
            @RequestParam(defaultValue = "") String pathPrefix) {
        Query query = Query.query(Criteria.where("workspaceId").regex("^" + userId));
        if (!pathPrefix.isBlank()) {
            query.addCriteria(Criteria.where("path").regex("^" + pathPrefix));
        }
        List<MemoryFile> files = mongoTemplate.find(query, MemoryFile.class, "memory_files");
        return Result.success(toVOList(files));
    }

    /**
     * 读取记忆文件内容（从 chunks 重组）。
     */
    @GetMapping("/files/content")
    public Result<String> readFileContent(
            @RequestParam String userId,
            @RequestParam String path) {
        Query query = Query.query(
                Criteria.where("workspaceId").regex("^" + userId)
                        .and("path").is(path)
        ).with(Sort.by("startLine"));
        List<MemoryChunk> chunks = mongoTemplate.find(query, MemoryChunk.class, "memory_chunks");

        StringBuilder content = new StringBuilder();
        for (MemoryChunk chunk : chunks) {
            content.append(chunk.getText()).append("\n");
        }
        return Result.success(content.toString());
    }

    /**
     * 编辑记忆文件（查找替换）。
     */
    @PutMapping("/files/edit")
    public Result<Boolean> editFile(
            @RequestParam String userId,
            @RequestBody EditMemoryRequest request) {
        // 查找包含 oldText 的 chunk
        Query query = Query.query(
                Criteria.where("workspaceId").regex("^" + userId)
                        .and("path").is(request.getPath())
                        .and("text").regex(Pattern.quote(request.getOldText()))
        );
        List<MemoryChunk> chunks = mongoTemplate.find(query, MemoryChunk.class, "memory_chunks");

        for (MemoryChunk chunk : chunks) {
            String newText = chunk.getText().replace(request.getOldText(), request.getNewText());
            Update update = Update.update("text", newText).set("updatedAt", new Date());
            mongoTemplate.updateFirst(
                    Query.query(Criteria.where("_id").is(chunk.getId())),
                    update, "memory_chunks"
            );
        }
        return Result.success(!chunks.isEmpty());
    }

    /**
     * 搜索记忆（直接使用 MongoDB text search）。
     */
    @GetMapping("/search")
    public Result<List<MemorySearchResultVO>> search(
            @RequestParam String userId,
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {
        Query mongoQuery = Query.query(
                Criteria.where("workspaceId").regex("^" + userId)
                        .and("text").regex(query, "i")
        ).limit(limit);
        List<MemoryChunk> results = mongoTemplate.find(mongoQuery, MemoryChunk.class, "memory_chunks");
        return Result.success(toSearchResultVOList(results));
    }

    /**
     * 聚合统计：用户记忆概览。
     */
    @GetMapping("/stats")
    public Result<MemoryStatsVO> getStats(@RequestParam String userId) {
        long chunkCount = mongoTemplate.count(
                Query.query(Criteria.where("workspaceId").regex("^" + userId)),
                "memory_chunks"
        );
        long fileCount = mongoTemplate.count(
                Query.query(Criteria.where("workspaceId").regex("^" + userId)),
                "memory_files"
        );
        return Result.success(new MemoryStatsVO(chunkCount, fileCount));
    }
}
```

---

## 7. 优势与弊端分析

### 7.1 优势

| 优势 | 说明 |
|------|------|
| **真正的数据隔离** | MongoDB 文档级 userId 过滤，物理隔离 |
| **可查询/可聚合** | 直接用 MongoDB 查询用户记忆、统计数据 |
| **水平扩展** | MongoDB Replica Set + Sharding |
| **统一数据层** | HiveMind 已有 MongoDB，无需引入新依赖 |
| **向量检索** | Atlas Vector Search 替代暴力 cosine 计算 |
| **全文检索** | MongoDB text index 替代 BM25，无需维护 pickle 文件 |
| **前端直连** | Java 侧直接查询 MongoDB，无需 MCP 代理 |
| **运维友好** | 成熟的备份、监控、恢复工具链 |

### 7.2 弊端

| 弊端 | 说明 | 缓解措施 |
|------|------|----------|
| **开发工作量大** | 需要重写 3 个核心组件 (FileStore, FileGraph, EmbeddingStore) | 分阶段实施，先实现 FileStore |
| **wikilink 语义变化** | MongoDB 中的图谱查询与内存图谱性能特征不同 | 建立合适的索引；小规模下性能可接受 |
| **文件可读性降低** | 记忆存在 MongoDB 中，不再是直接可读的 Markdown | 可选保留文件镜像层 |
| **ReMe 升级风险** | 自定义组件可能与 ReMe 新版本不兼容 | 锁定 ReMe 版本；接口层隔离 |
| **失去 Memory as File 优势** | 不能直接在文件系统中浏览、编辑记忆 | 前端 API 提供等价的浏览/编辑能力 |
| **Embedding 存储成本** | MongoDB 存储向量比 numpy 文件更大 | 仅对需要向量检索的 chunk 存储 embedding |
| **需要 Atlas** | Vector Search 仅在 MongoDB Atlas 中可用 | 自建 MongoDB 使用 text index 替代 |

### 7.3 适用场景

- ✅ 用户量较大（千级以上）
- ✅ 需要严格数据隔离
- ✅ 需要直接查询/聚合记忆数据
- ✅ 需要水平扩展
- ✅ 已有 MongoDB 基础设施
- ❌ 不适合快速上线（开发周期长）
- ❌ 不适合需要保留"文件即记忆"哲学的场景

---

## 8. 实施步骤

### Phase 1: MongoFileStore (1 周)

1. 实现 `MongoFileStore` 核心（upsert, delete, keyword_search）
2. 实现 `MongoFileGraph`（upsert_nodes, get_outlinks, get_inlinks）
3. 配置 MongoDB 索引
4. 单元测试

### Phase 2: Embedding + Search (3-5 天)

1. 实现 `MongoEmbeddingStore`
2. 实现 `vector_search`（Atlas Vector Search 或自定义实现）
3. RRF 融合搜索集成
4. 性能测试

### Phase 3: 文件镜像 + 编辑 API (3-5 天)

1. 可选：实现 Markdown 文件镜像层
2. 前端 MemoryController API
3. 前端记忆浏览/编辑页面

### Phase 4: 集成测试 + 部署 (3-5 天)

1. ReMe Job 集成测试（auto_memory, auto_dream, search）
2. 多用户隔离测试
3. 性能基准测试
4. 部署文档

**总工期**: 约 3-4 周

---

## 9. 最佳存储引擎设计

如果从零设计一个替代 ReMe file_store 的存储引擎，最佳方案是：

### 9.1 核心原则

1. **文档即 chunk**：每个 FileChunk 对应一个 MongoDB 文档，天然支持 CRUD
2. **workspace 即隔离单元**：通过 `workspaceId` 字段实现逻辑隔离
3. **索引即检索**：MongoDB 的 text index 和 vector index 替代自建的 BM25 和 numpy
4. **图谱即集合**：wikilink 关系存储在独立集合中，支持双向查询
5. **文件可选**：Markdown 文件作为可选的只读镜像，用于人类浏览

### 9.2 最终架构

```
┌─────────────────────────────────────────────────┐
│  应用层                                          │
│  ReMe Steps (auto_memory, search, ...)          │
│       │                                          │
│       ▼                                          │
│  MongoFileStore (统一接口)                        │
│    ├── MongoDB: memory_chunks (文本 + 向量)       │
│    ├── MongoDB: memory_files (文件元数据)         │
│    ├── MongoDB: memory_links (wikilink 图谱)     │
│    └── 文件系统: workspace/ (可选镜像)            │
│                                                  │
│  优势:                                           │
│  ✅ 用户隔离 (workspaceId 索引)                   │
│  ✅ 全文检索 (MongoDB text index)                 │
│  ✅ 向量检索 (Atlas Vector Search)                │
│  ✅ 可查询/可聚合                                 │
│  ✅ 水平扩展                                      │
│  ✅ 文件可读 (可选镜像)                            │
└─────────────────────────────────────────────────┘
```

### 9.3 关键接口

```python
class BaseFileStore(ABC):
    """存储引擎接口 - 所有实现必须遵守。"""

    @abstractmethod
    async def upsert(self, files: list[tuple[FileNode, list[FileChunk]]]) -> None:
        """写入/更新文件和分块。"""

    @abstractmethod
    async def delete(self, path: str | list[str]) -> None:
        """删除文件和分块。"""

    @abstractmethod
    async def get_nodes(self, paths: list[str] | None = None) -> list[FileNode]:
        """获取文件节点。"""

    @abstractmethod
    async def keyword_search(self, query: str, limit: int, search_filter: dict) -> list[FileChunk]:
        """全文检索。"""

    @abstractmethod
    async def vector_search(self, query: str, limit: int, search_filter: dict) -> list[FileChunk]:
        """向量检索。"""

    @abstractmethod
    async def get_outlinks(self, path: str, scope: LinkScopeEnum) -> list[FileLink]:
        """获取出链。"""

    @abstractmethod
    async def get_inlinks(self, path: str, scope: LinkScopeEnum) -> list[FileLink]:
        """获取入链。"""
```

---

## 10. 两种方案对比

| 维度 | 方案 A: Docker + MCP | 方案 B: MongoDB 存储层 |
|------|---------------------|---------------------|
| **开发工作量** | 5-9 天 | 3-4 周 |
| **存储扩展性** | 单机文件系统 | MongoDB 水平扩展 |
| **数据隔离** | 逻辑隔离 (workspace_id) | 物理隔离 (文档级) |
| **可查询性** | 通过 MCP 工具间接查询 | 直接 MongoDB 查询 |
| **文件可读性** | 直接可读 (Markdown) | 需要镜像层或前端 |
| **记忆演化** | 完整保留 | 完整保留 |
| **运维复杂度** | Docker + volume | MongoDB + Docker |
| **向量检索** | 可选 (本地 numpy) | Atlas Vector Search |
| **全文检索** | BM25 (pickle) | MongoDB text index |
| **前端编辑** | MCP 工具代理 | 直接 MongoDB 操作 |
| **适用用户规模** | 百级 | 千级+ |
| **风险** | 低 | 中 (组件重写) |

**建议**：当前阶段采用**方案 A**，快速上线；当用户规模增长到需要严格隔离和水平扩展时，再演进到**方案 B**。方案 A 的 MCP 接口层可以平滑过渡到方案 B——只需将 ReMe 的底层组件从 LocalFileStore 替换为 MongoFileStore，上层 MCP 工具接口不变。
