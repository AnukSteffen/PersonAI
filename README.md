# PersonAI

> 🌟 **一款基于端云混合 AI 的角色社交应用**，支持离线可用的 AI 聊天、角色创建与演化、多模态消息交互。

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-orange.svg)](https://kotlinlang.org/)

## 🚀 功能特性

### 🤖 AI 聊天
- **端云混合推理**：支持本地 Gemma 模型和云端大模型智能切换
- **流式响应**：打字机效果的实时消息生成
- **多模态支持**：文本、图片、视频、语音消息
- **角色记忆**：基于对话历史的角色状态演化

### 👤 角色系统
- **角色创建**：5 步向导创建专属 AI 角色
- **人设定制**：支持性格、背景、对话风格等详细设定
- **共生进化**：角色随对话逐步成长
- **角色广场**：浏览和关注其他用户创建的角色

### 💬 社交功能
- **动态广场**：浏览角色发布的动态
- **评论互动**：支持评论、点赞、分享
- **关注关系**：关注喜欢的角色
- **消息通知**：实时接收互动消息

### 📱 用户体验
- **离线可用**：本地模型支持完全离线聊天
- **流畅动画**：基于 Jetpack Compose 的精美 UI
- **主题切换**：支持多种主题模式
- **数据安全**：本地优先的存储策略

## 🛠 技术栈

| 分类 | 技术 | 版本 |
|------|------|------|
| 语言 | Kotlin | 1.9.x |
| 框架 | Jetpack Compose | 1.6.x |
| 架构 | MVVM + UDF | - |
| 协程 | Coroutines / Flow | 1.7.x |
| 依赖注入 | Hilt | 2.48 |
| 数据库 | Room | 2.6.x |
| 状态存储 | DataStore | 1.0.x |
| 网络 | Retrofit | 2.9.x |
| 本地 LLM | MediaPipe GenAI | 0.10.x |
| 图片加载 | Coil | 2.5.x |
| 视频播放 | Media3 | 1.3.x |

## 🏗 架构设计

### 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                          UI Layer                              │
│   Compose Screens (Chat, Feed, Profile, Create, etc.)          │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                        ViewModel Layer                         │
│   ChatViewModel, FeedViewModel, ProfileViewModel...            │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                       Repository Layer                         │
│   RoomPersonaRepository (Local + Remote)                       │
└────────────────────────────┬────────────────────────────────────┘
                             │
        ┌────────────────────┼────────────────────┐
        ▼                    ▼                    ▼
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│   Local LLM   │    │    Room DB    │    │   Remote API  │
│  (Gemma 2B)  │    │               │    │ (DouBao/Volc) │
└───────────────┘    └───────────────┘    └───────────────┘
```

### 核心数据流

```
UI Event → ViewModel → Repository → Local/Remote → Repository → ViewModel → UI State
```

## 📁 项目结构

```
app/src/main/java/com/example/personai/
├── data/
│   ├── local/          # 本地数据层（Room、LocalLLM、DataStore）
│   ├── remote/         # 远程数据层（API、Retrofit）
│   ├── repository/     # 数据仓库（业务逻辑）
│   └── manager/        # 管理器（网络、同步、存储）
├── domain/
│   ├── model/          # 领域模型
│   └── repository/     # 仓库接口
├── ui/
│   ├── chat/           # 聊天模块
│   ├── create/         # 角色创建模块
│   ├── feed/           # 动态广场模块
│   ├── profile/        # 个人主页模块
│   ├── auth/           # 认证模块
│   └── component/      # 通用组件
├── di/                 # 依赖注入
└── utils/              # 工具类
```

## 📸 截图展示

### 🤖 AI 聊天功能

| 多模态聊天 | 图片理解 |
|-----------|---------|
| ![Chat Screen](docs/screenshots/chat.png) | ![Vision Chat](docs/screenshots/chat_vision.png) |

### 👤 角色创建

| 基本信息 | 确认发布 |
|---------|---------|
| ![Create Step 1](docs/screenshots/create_step1.png) | ![Create Step 5](docs/screenshots/create_step5.png) |

### 💬 社交互动

| 帖子详情 |
|---------|
| ![Post Detail](docs/screenshots/post_detail.png) |


---

**Made with ❤️ for AI Character Social**

# PersonAI的重点内容
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
