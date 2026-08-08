# AI-Composer
Intelligent passage composer is consist of multiple agents

# 技术栈
## 后端
* Springboot 3.5
* MyBatis Flex 1.11
* Redis
* jdk21

# 适用群体和需求
可以通过提供数据和资料来自动生成想要的文章或文档, 适用于资讯写手, 开发者等需要创造的工作者

# 大体流程

前端事件流 + 后端多智能体流水线，通过 SSE 长连接单向链式串联（`① → ② → ③ → ...`）：

1. 前端点击「生成文章」，携带选题 `topic`
   ↓
2. 发送 `POST /api/article/create`
   ↓
3. 后端 `ArticleController.createArticle`：校验选题 → 获取登录用户 → 创建文章任务（生成 UUID `taskId`，状态 `PENDING`）→ 提交到异步线程池 `articleExecutor`
   ↓
4. 接口立即返回 `taskId`（同步接口不阻塞，生成在后台进行）
   ↓
5. 前端拿到 `taskId` 后立刻建立 SSE 长连接：`GET /api/article/progress/{taskId}`
   ↓
6. 后端 `getProgress`：鉴权 → `SseEmitterManager.createEmitter(taskId)` 将 emitter 注册进内存 Map → 返回 `SseEmitter`，连接建立
   ↓
7. 异步任务 `ArticleAsyncService.executeArticleGeneration` 启动：状态更新为 `PROCESSING`，创建 `ArticleState`（taskId + topic）
   ↓
8. 多智能体流水线 `ArticleAgentService.executeArticleGeneration`，每完成一步就推一条 SSE 进度事件：

   - 智能体1 · 生成主标题/副标题（同步调用）→ 推送 `AGENT1_COMPLETE`
   - 智能体2 · 生成文章大纲（流式）→ 推送 `AGENT2_STREAMING` 分片 + `AGENT2_COMPLETE`
   - 智能体3 · 生成正文 Markdown（流式）→ 推送 `AGENT3_STREAMING` 分片 + `AGENT3_COMPLETE`
   - 智能体4 · 分析配图需求（1 张封面 + 2~4 张内图）→ 推送 `AGENT4_COMPLETE`
   - 智能体5 · 逐张检索配图（Pexels 图库，失败降级 Picsum 随机图）→ 每张推送 `IMAGE_COMPLETE`，全部完成推送 `AGENT5_COMPLETE`
   - 图文合成 · 按 `## 章节标题` 把内图插入正文 → 推送 `MERGE_COMPLETE`

   ↓
9. 保存文章到数据库 → 状态更新为 `COMPLETED` → 推送 `ALL_COMPLETE`
   ↓
10. 前端收到 `ALL_COMPLETE` 后停止监听 → 后端 `SseEmitterManager.complete(taskId)` 关闭连接 → 流程结束

> 异常分支：任意环节抛错 → 状态更新为 `FAILED` → 推送 `ERROR` 事件 → 同样关闭连接。

> 注意：第 5 步必须尽快建立 SSE 连接，若在连接建立前流水线已推送事件，这些事件会被丢弃（后端只打 WARN 日志），前端将收不到。
