# PersonAI
技术栈：Kotlin、Jetpack Compose、Coroutines/Flow、Room、DataStore、Hilt、Retrofit、MediaPipe GenAI、Coil、Media3

项目描述：
基于 Jetpack Compose 开发的 AI 角色社交 App，支持角色创建、多模态聊天、评论点赞、关注关系与本地数据持久化。项目探索端侧 LLM 与云端大模型结合的混合 AI 交互方案，并围绕离线可用、流式响应和角色状态演化进行实践。

核心工作：
1. 端云混合 AI 聊天
- 基于 MediaPipe LlmInference 接入端侧 Gemma 模型，使用 callbackFlow 将异步生成回调封装为 Flow，实现流式回复。
- 使用 Mutex 控制本地模型推理并发，避免多会话同时访问模型导致资源竞争。
- 在推理结束时关闭 LlmInferenceSession，降低长时间聊天场景下的资源占用风险。
- 封装云端对话、图片生成、视频任务和 TTS 接口，为多模态消息生成提供统一调用入口。

2. 角色状态与上下文演化
- 设计角色 system prompt、人设字段、对话风格和动态状态等数据结构。
- 根据聊天轮次触发角色状态摘要和人设演化逻辑，用于提升连续对话中的上下文一致性。
- 为角色生成 embedding，并结合向量相似度实现基础推荐与语义匹配能力。

3. Compose 聊天与动态流
- 使用 Jetpack Compose 构建聊天页、动态广场、角色主页和创建向导等页面。
- 通过 ViewModel + StateFlow 管理页面状态，支持文本、图片、视频、语音和帖子分享等不同消息类型展示。
- 对动态列表、搜索、关注流等场景使用 Flow、combine、flatMapLatest 实现响应式数据更新和搜索请求切换。

4. 本地优先的数据架构
- 基于 Room 设计 Persona、Message、Post、Comment、User、Follow、Draft 等本地实体。
- 使用 DataStore 保存登录用户、主题模式等轻量状态。
- 实现草稿箱、浏览历史、点赞、关注、隐藏帖子等本地优先功能。
- 设计本地同步队列原型，在网络恢复时消费待同步操作，为后续服务端同步预留扩展点。

5. Android 平台适配
- 使用 MediaStore 保存图片/视频，适配 Android 10+ 分区存储机制。
- 使用 IS_PENDING 保证媒体写入过程的安全性。
- 使用 Coil/Media3 支持图片、GIF、视频缩略图和视频播放。