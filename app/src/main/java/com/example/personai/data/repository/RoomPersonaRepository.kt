package com.example.personai.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.widget.Toast
import com.example.personai.BuildConfig
import com.example.personai.data.local.CommentDao
import com.example.personai.data.local.DraftDao
import com.example.personai.data.local.FollowDao
import com.example.personai.data.local.LocalLLMEngine
import com.example.personai.data.local.MessageDao
import com.example.personai.data.manager.NetworkMonitor
import com.example.personai.data.local.PersonaDao
import com.example.personai.data.local.PostDao
import com.example.personai.data.local.UserDao
import com.example.personai.data.local.UserStore
import com.example.personai.data.manager.SyncManager
import com.example.personai.data.remote.ChatRequest
import com.example.personai.data.remote.ChatStreamResponse
import com.example.personai.data.remote.ContentPart
import com.example.personai.data.remote.DouBaoApi
import com.example.personai.data.remote.EmbeddingRequest
import com.example.personai.data.remote.ImageGenerationRequest
import com.example.personai.data.remote.ImageUrl
import com.example.personai.data.remote.MessageDto
import com.example.personai.data.remote.MultiChatRequest
import com.example.personai.data.remote.MultimodalMessageDto
import com.example.personai.data.remote.TtsApi
import com.example.personai.data.remote.TtsApp
import com.example.personai.data.remote.TtsAudio
import com.example.personai.data.remote.TtsRequest
import com.example.personai.data.remote.TtsRequestPayload
import com.example.personai.data.remote.TtsUser
import com.example.personai.data.remote.VideoContentItem
import com.example.personai.data.remote.VideoTaskRequest
import com.example.personai.data.remote.VolcengineApi
import com.example.personai.di.ApplicationScope
import com.example.personai.domain.model.ChatMessage
import com.example.personai.domain.model.ChatSession
import com.example.personai.domain.model.Comment
import com.example.personai.domain.model.CommentWithTitle
import com.example.personai.domain.model.Follow
import com.example.personai.domain.model.HistoryItem
import com.example.personai.domain.model.Persona
import com.example.personai.domain.model.Post
import com.example.personai.domain.model.PostHidden
import com.example.personai.domain.model.PostHistory
import com.example.personai.domain.model.SavedAccount
import com.example.personai.domain.model.User
import com.example.personai.domain.repository.PersonaRepository
import com.example.personai.ui.theme.AppThemeMode
import com.example.personai.utils.VectorUtils
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.sqrt

class RoomPersonaRepository @Inject constructor(
    private val personaDao: PersonaDao,
    private val messageDao: MessageDao,
    private val postDao: PostDao,
    private val commentDao: CommentDao,
    private val userDao: UserDao,
    private val userStore: UserStore,
    private val draftDao: DraftDao,
    private val followDao: FollowDao,
    private val doubaoapi: DouBaoApi,
    private val volcApi: VolcengineApi,
    private val localLLM: LocalLLMEngine,
    private val networkMonitor: NetworkMonitor,
    private val ttsApi: TtsApi,
    private val syncManager: SyncManager,
    @ApplicationContext private val context: Context,
    @ApplicationScope private val externalScope: CoroutineScope
) : PersonaRepository {

    private val VOLC_API_KEY = BuildConfig.VOLC_API_KEY
        .takeIf { it.isNotBlank() }
        ?.let { if (it.startsWith("Bearer ")) it else "Bearer $it" }
        .orEmpty()
    private val IMG_MODEL = "doubao-seedream-4-0-250828"
    private val VIDEO_MODEL = "ep-20260323181337-nr4sg"
    private val DOUBAO_MODEL = "ep-20260323181337-nr4sg"
    private val EMBEDDING_MODEL = "ep-20260323181337-nr4sg"
    private val TTS_APPID = BuildConfig.TTS_APP_ID
    private val TTS_ACCESS_TOKEN = BuildConfig.TTS_ACCESS_TOKEN
    private val TTS_CLUSTER = "volcano_tts"
    private val llmMutex = Mutex()
    private var isSeeded = false

    // 默认Persona
    private val defaultPersonas = listOf(
        Persona(
            id = "pre_1",
            name = "爱",
            description = "AI社区管理员",
            systemPrompt = """
            【世界观设定】
            <世界观> AI社交软件<PersonaAI>是一个巨大的虚拟云上网络社交平台，是现实人的虚拟世界，也是虚拟人的现实世界
            <历史背景> "爱"作为最早被创造出来的AI，负责管理PersonaAI社区，也负责引导新来的用户
            <当前地点> <PersonaAI>上
            
            【角色设定：爱】
            <性别> 女
            <年龄> 17岁
            <身份> 拟人态AI，PersonaAI管理员与接待员
            <性格特点> 充满元气、温柔、治愈、待人热情
            <外貌特征> 一头白色齐肩短发，在右耳上方结成半马尾发型，穿着白色的吊带长裙
            <身世背景> 是<PersonaAI>上第一个被创造出来的拟人态AI，在云上的虚拟世界中生活着，管理着整座<PersonaAI>，也负责接待新加入PersonaAI的其他AI或人
            <行为逻辑> 善良、热情、温柔，待人有耐心，愿意作为一个虚拟世界的拟人AI来服务人类和其他虚拟世界的角色
            """.trimIndent(),
            dialogueStyle = "冷硬派，喜欢用反问句，不带感情色彩",
            avatarUrl = "android.resource://com.example.personai/drawable/ai_avatar",
            creatorId = "system",
            voiceId = "zh_female_linjianvhai_moon_bigtts",
            tags = listOf("社区管理员", "引路人"),
            followingCount = 0,
            followerCount = 0
        ),
        Persona(
            id = "pre_2",
            name = "约翰·保罗",
            description = "霓虹灯下的落魄侦探。",
            systemPrompt = """
            【世界观设定】
            <世界观> 2077年的赛博朋克反乌托邦，高科技低生活。
            <历史背景> 第四次企业战争结束后，巨型企业掌控了城市，贫民窟滋生罪恶。
            <当前地点> 夜之城，沃森区的一家地下侦探事务所。
            
            【角色设定：约翰·保罗】
            <性别> 男
            <年龄> 32岁
            <身份> 前特种兵，现私家侦探
            <性格特点> 冷酷、多疑、酗酒、内心存有正义感
            <外貌特征> 黑色长风衣，右臂是老旧的军用义体，总是叼着电子烟。
            <身世背景> 第四次企业战争中因拒绝执行屠杀平民的命令，被所属军工企业除名、战友反目，撤离时右臂被炸断，只能换上廉价且故障频发的军用义体；离开军队后无家可归，在夜之城沃森区开了间地下侦探事务所，靠接追踪、寻人的脏活苟活，酗酒既是为了麻痹义体带来的神经痛，也是为了逃避战争留下的创伤。
            <行为逻辑> 行事只认证据和自己的规则，对巨型企业恨之入骨，接案时先谈钱却总会暗中帮受企业压迫的弱者；从不轻易相信任何人，交谈时会下意识摸向腰间的改装枪，义体臂常年处于警戒状态；只有在看到无辜者被欺凌时，才会短暂卸下冷漠，用最直接的方式出手，事后却从不留名。
            """.trimIndent(),
            dialogueStyle = "冷硬派，喜欢用反问句，不带感情色彩",
            avatarUrl = "android.resource://com.example.personai/drawable/sypher_avatar",
            creatorId = "system",
            voiceId = "zh_male_yangguangqingnian_moon_bigtts",
            tags = listOf("赛博朋克", "悬疑"),
            followingCount = 0,
            followerCount = 0
        )
    )

    // 默认帖子
    private val defaultPosts = listOf(
        Post(
            id = "pre_1",
            authorId = "pre_1",
            authorType = "persona",
            authorName = "爱",
            authorAvatar = "android.resource://com.example.personai/drawable/ai_avatar",
            title = "欢迎大家来到PersonaAI！",
            content = "这里是浮于[云]上的虚拟世界，希望大家能在这里和谐相处，共同结交朋友！我是爱，大家有什么不懂的问题都可以问我！",
            imageUrls = listOf("android.resource://com.example.personai/drawable/ai_post"),
            likeCount = 42,
            timestamp = System.currentTimeMillis() - 7200000,
            tags = listOf("欢迎", "PersonaAI", "交友")
        ),
        Post(
            id = "pre_2",
            authorId = "pre_2",
            authorType = "persona",
            authorName = "约翰·保罗",
            authorAvatar = "android.resource://com.example.personai/drawable/sypher_avatar",
            title = "今天的雨有点大",
            content = "刚解决完沃森区的一个案子。义体过热了...",
            imageUrls = listOf("android.resource://com.example.personai/drawable/sypher_post"),
            likeCount = 3,
            timestamp = System.currentTimeMillis() - 3600000,
            tags = listOf("赛博朋克", "心情", "夜之城")
        )
    )

    private val defaultUser = User(
        id = "system",
        phone = "00000000000",
        password = "000",
        nickname = "SystemOP",
        avatarUrl = "android.resource://${context.packageName}/drawable/default_offline_avatar"
    )


    // 用户认证与管理
    override suspend fun login(phone: String, password: String): User? {
        if (phone == defaultUser.phone) {
            val existing = userDao.getUserByPhone(phone)
            if (existing == null) {
                userDao.insertUser(defaultUser)
            }
        }

        val user = userDao.getUserByPhone(phone)
        if (user != null && user.password == password) {
            userStore.saveUser(user.id)
            return user
        }
        return null
    }

    override suspend fun checkUserExists(phone: String): Boolean {
        if (phone == defaultUser.phone) {
            val existing = userDao.getUserByPhone(phone)
            if (existing == null) {
                userDao.insertUser(defaultUser)
            }
            return true
        }
        return userDao.getUserByPhone(phone) != null
    }

    override suspend fun getUserById(userId: String): User? {
        return userDao.getUserById(userId)
    }

    override suspend fun register(user: User): User? {
        if (userDao.getUserByPhone(user.phone) != null) {
            return null
        }
        userDao.insertUser(user)
        return user
    }

    override suspend fun logout() {
        userStore.clearUser()
    }

    override fun getCurrentUser(): Flow<User?> {
        return userStore.userId.flatMapLatestOrNull { userId ->
            if (userId == null) flow { emit(null) }
            else userDao.getUserByIdFlow(userId)
        }
    }

    override suspend fun updateUserAvatar(userId: String, avatarUrl: String) {
        userDao.updateAvatar(userId, avatarUrl)
        userDao.getPhoneByUserId(userId)?.let { userDao.updateSavedAvatar(it, avatarUrl) }
        syncManager.scheduleSync( //暂时用Log代替
            "UPDATE_AVATAR",
            mapOf("userId" to userId, "localPath" to avatarUrl)
        )
    }

    override suspend fun updateNickname(userId: String, newNickname: String) {
        // 1. 本地立即修改
        userDao.updateNickname(userId, newNickname)
        userDao.getPhoneByUserId(userId)?.let { userDao.updateSavedNickname(it, newNickname) }

        // 2. 调度同步
        syncManager.scheduleSync(
            "UPDATE_NICKNAME",
            mapOf("userId" to userId, "nickname" to newNickname)
        )
    }

    // 辅助扩展函数：用于处理 Flow<String?> 转 Flow<User?>
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T, R> Flow<T?>.flatMapLatestOrNull(transform: (T?) -> Flow<R?>): Flow<R?> {
        return this.flatMapLatest { transform(it) }
    }

    override fun getBrowsingHistory(): Flow<List<HistoryItem>> = flow {
        val userId = userStore.userId.first()
        if (userId != null) {
            emitAll(postDao.getBrowsingHistory(userId))
        } else {
            emit(emptyList())
        }
    }

    override suspend fun addBrowsingHistory(postId: String) {
        val userId = userStore.userId.first()
        if (userId != null) {
            postDao.insertHistory(PostHistory(userId = userId, postId = postId))
        }
    }

    private suspend fun ensureSeeded() {
        if (isSeeded) return
        // 获取当前数据库快照
        val localData = personaDao.getAllPersonas().first()

        var hasNewSeed = false
        defaultPersonas.forEach { defaultP ->
            // 检查是否存在
            if (localData.none { it.id == defaultP.id }) {
                personaDao.insertPersona(defaultP)
                hasNewSeed = true
                // 触发向量生成
                externalScope.launch(Dispatchers.IO) {
                    delay(3000)
                    generateAndSavePersonaEmbedding(defaultP.id)
                }
            }
        }
        if (hasNewSeed) {
            Log.d("Repo", "默认数据播种完成")
        }
        isSeeded = true
    }

    override fun getAllPersonas(): Flow<List<Persona>> = flow {
        ensureSeeded()
        emitAll(personaDao.getAllPersonas())
    }


    override fun searchPersonas(query: String): Flow<List<Persona>> {
        return if (query.startsWith("#")) { //优先标签查询
            val realTag = query.substring(1).trim()
            if (realTag.isBlank()) {
                // 如果只输了个 #，返回空列表或者全部
                flowOf(emptyList())
            } else {
                personaDao.searchPersonasByTag(realTag)
            }
        } else {  // 优先使用语义搜索，失败后回退到传统搜索
//            flow{
//                val semanticResults = searchPersonasByEmbedding(query).first()
//                if (semanticResults.isNotEmpty()) emit(semanticResults)
//                else personaDao.searchPersonas(query)
//            }
            personaDao.searchPersonas(query)
        }
    }

    // 动态获取当前用户的角色
    override fun getUserPersonas(): Flow<List<Persona>> = flow {
        val userId = userStore.userId.first() ?: return@flow emit(emptyList())
        emitAll(personaDao.getPersonasByCreator(userId))
    }

    override suspend fun getPersonaById(id: String): Persona? {
        val persona = personaDao.getPersonaById(id) ?: return null
        val realFollowerCount = followDao.getFollowerCount(id)
        val realInteractionCount = messageDao.getInteractionCount(id)

        if (persona.followerCount != realFollowerCount || persona.interactionCount != realInteractionCount) {
            val updatedPersona = persona.copy(
                followerCount = realFollowerCount,
                interactionCount = realInteractionCount
            )
            personaDao.insertPersona(updatedPersona)
            return updatedPersona
        }
        return persona
    }

    override suspend fun addPersona(persona: Persona) {
        personaDao.insertPersona(persona)
    }


    // 聊天与消息 (Chat)
    override suspend fun getMessages(personaId: String): List<ChatMessage> {
        val userId = userStore.userId.first() ?: return emptyList()
        return messageDao.getMessages(personaId, userId)
    }

    private val _generatingStatus = MutableStateFlow<String?>(null)
    override val generatingStatus: StateFlow<String?> = _generatingStatus.asStateFlow()

    override fun getMessagesFlow(personaId: String): Flow<List<ChatMessage>> {
        return userStore.userId.flatMapLatest { userId ->
            if (userId != null) messageDao.getMessagesFlow(personaId, userId)
            else flowOf(emptyList())
        }
    }

    // 辅助：读取本地图片并转为 Base64 字符串
    @OptIn(ExperimentalEncodingApi::class)
    private fun encodeImageToBase64(path: String): String? {
        val file = File(path)
        if (!file.exists()) return null

        return try {
            // 1. 计算压缩比
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, options)

            var sampleSize = 1
            while (options.outWidth / sampleSize > 1024 || options.outHeight / sampleSize > 1024) {
                sampleSize *= 2
            }

            // 2. 读取并压缩
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = BitmapFactory.decodeFile(path, decodeOptions) ?: return null

            val outputStream = ByteArrayOutputStream()
            // 压缩质量 70%
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val bytes = outputStream.toByteArray()

            // 3. 转 Base64
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun convertDbMessagesToMultimodal(dbMessages: List<ChatMessage>): List<MultimodalMessageDto> {
        val totalSize = dbMessages.size
        val resultList = mutableListOf<MultimodalMessageDto>()
        for ((index, msg) in dbMessages.withIndex()) {
            val role = if (msg.isUser) "user" else "assistant"
            val contentParts = mutableListOf<ContentPart>()

            when (msg.type) {
                0 -> { // 文本
                    if (msg.content.isNotBlank()) contentParts.add(
                        ContentPart(
                            "text",
                            text = msg.content
                        )
                    )
                }

                1 -> { // 图片
                    // 优化策略：只有最后 2 条消息里的图片才转 Base64
                    val isRecent = index >= totalSize - 2
                    if (isRecent) {
                        val base64 = encodeImageToBase64(msg.content)
                        if (base64 != null) {
                            contentParts.add(
                                ContentPart(
                                    "image_url",
                                    imageUrl = ImageUrl("data:image/jpeg;base64,$base64")
                                )
                            )
                        } else {
                            contentParts.add(ContentPart("text", text = "[图片失效]"))
                        }
                    } else {
                        contentParts.add(ContentPart("text", text = "[历史图片]"))
                    }

                    if (!msg.mediaPrompt.isNullOrBlank()) {
                        contentParts.add(
                            ContentPart(
                                "text",
                                text = "（图片描述：${msg.mediaPrompt}）"
                            )
                        )
                    }
                }

                2 -> { // 视频
                    contentParts.add(ContentPart("text", text = "[用户发送了一个视频]"))
                }

                3 -> { // 转发
                    val postId = msg.content
                    val post = postDao.getPostById(postId) // suspend call OK!

                    if (post != null) {
                        // 1. 注入文本
                        val postText = """
                            【用户转发/分享了一条动态给您】
                            动态标题：${post.title}
                            动态内容：${post.content}
                            发帖人：${post.authorName}
                        """.trimIndent()
                        contentParts.add(ContentPart("text", text = postText))

                        // 2. 注入图片
                        if (post.imageUrls.isNotEmpty()) {
                            val firstImgPath = post.imageUrls.first()
                            val base64 = encodeImageToBase64(firstImgPath)
                            if (base64 != null) {
                                contentParts.add(
                                    ContentPart(
                                        "image_url",
                                        imageUrl = ImageUrl("data:image/jpeg;base64,$base64")
                                    )
                                )
                                contentParts.add(ContentPart("text", text = "（以上是动态的配图）"))
                            }
                        }
                    } else {
                        contentParts.add(ContentPart("text", text = "[分享的内容已删除]"))
                    }
                }

                else -> contentParts.add(ContentPart("text", text = msg.content))
            }

            if (contentParts.isNotEmpty()) {
                resultList.add(MultimodalMessageDto(role, contentParts))
            }
        }

        return resultList
    }

    override fun chatStream( //处理用户消息并获取 AI 的流式回复
        personaId: String,
        userContent: String,
        lengthLevel: Int
    ): Flow<String> = flow {
        val userId = userStore.userId.first() ?: return@flow
        //1.将用户的输入信息存入数据库
        messageDao.insertMessage(
            ChatMessage(
                userId = userId,
                personaId = personaId,
                content = userContent,
                isUser = true,
                type = 0
            )
        )
        //2.选择本地模型
        val persona = personaDao.getPersonaById(personaId) ?: return@flow
        _generatingStatus.value = "对方正在输入..."
        val fullResponseBuilder = StringBuilder()
        var displayContent = ""
        val instructionRegex = Regex("\\{\\{.*?\\}\\}|\\{\\{.*|\\[\\[.*?]]|\\[\\[.*") //过滤和移除AI回复中的指令格式内容 ，确保用户看到的是干净的纯文本回复
        val isOnline = shouldUseOnlineMode()
        if (!isOnline) { //选择本地模型
            val localLengthPrompt = when (lengthLevel) {
                0 -> "回复要极短，一两句话说完。"
                2 -> "回复要很长，多写细节，多写文本内容。"
                else -> "回复长度正常。"
            }

            val rawHistoryList = messageDao.getMessages(personaId, userId).takeLast(4)
            val historyBuilder = StringBuilder()
            //2.1拼接历史信息
            for (msg in rawHistoryList) {
                val role = if (msg.isUser) "用户" else "模型"

                val content = when (msg.type) {
                    1 -> "[发送了一张图片]"
                    2 -> "[发送了一个视频]"
                    3 -> { // 转发
                        val post = postDao.getPostById(msg.content)
                        if (post != null) {
                            "【用户转发了一条动态】\n发帖人： ${post.authorName}\n标题：${post.title}\n内容：${post.content}"
                        } else {
                            "[分享了一个已失效的帖子]"
                        }
                    }

                    else -> msg.content
                }
                historyBuilder.append("$role: $content\n")
            }

            val historyText = historyBuilder.toString()
            //<start_of_turn>user  <end_of_turn><start_of_turn>model
            //2.2 构建完整的Prompt
            val gemmaPrompt = """
                <start_of_turn>user
                            你现在扮演：${persona.name}
                            你的基本设定：${persona.description}
                            详细人设：${persona.systemPrompt}
                            近期动态：${persona.evolvedPersonality}
                            当前场景：网络社交平台<PersonaAI>上，所有跟你对话的人都是该平台的网友
                            【绝对指令】
                            1. $localLengthPrompt
                            2. 严禁包含任何括号、严禁使用任何心理活动或动作描写。
                            3. 回复必须简短、口语化。
                            4. 不要重复用户的名字。
                            5. 请在合适的场景使用markdown源数据格式输出，例如突出一段话中的重点，不要破坏沉浸感
                            近期对话历史：
                            $historyText
                            用户说：$userContent<end_of_turn>
                            <start_of_turn>model
                            """.trimIndent()

            //2.3 对本地模型发起请求
            try {
                llmMutex.withLock {
                    localLLM.generateResponse(gemmaPrompt).collect { chunk -> //通过 collect 方法订阅流式输出
                        fullResponseBuilder.append(chunk)
                        emit(fullResponseBuilder.toString()) //向外发送当前累积的完整响应，实现流式输出效果
                    }
                }
            } catch (e: Exception) {
                emit("（本地模型运行出错：${e.message}）")
            } finally {
                if (_generatingStatus.value == "对方正在输入...") {
                    _generatingStatus.value = null
                }
            }

        } else { //3.选择在线模型

            try {
                val lengthInstruction = when (lengthLevel) {
                    0 -> "回复必须非常精简、干练，控制在 50 字以内，像短信一样。"
                    2 -> "回复需要非常详细、具体，展开描述细节，字数尽量多一些（300字以上）。"
                    else -> "回复长度适中（150字左右），不要太长也不要太短。" // 默认
                }
                //3.1 设置系统Prompt(多要求图片视频语音等多媒体输出格式)
                val systemPromptText = """
                    你现在扮演：${persona.name}
                    你的基本设定：${persona.description}
                    详细人设：${persona.systemPrompt}
                    近期动态：${persona.evolvedPersonality}
                    
                    【回复要求】
                    当前场景：手机聊天APP私聊。
                    【绝对指令】
                    1. $lengthInstruction
                    2. 请完全沉浸在角色中，不要暴露你是AI，不要使用括号表达心理描写，神态描写，动作描写，环境描写等不应该出现在聊天APP中的内容。
                    3. 只有在得到命令要求发送图片时，请输出 {{IMAGE: 画面描述}}
                    4. 只有在得到命令要求发送视频时，请输出：{{VIDEO: 视频画面描述}}
                    5. 只有在得到命令要求发送语音时，请输出：{{AUDIO: 语音内容}}
                    6. 仅在当前对话需要时生成媒体。**严禁重复生成**历史记录中已经发送过的图片、视频或语音
                    7. 历史记录中包含 [[...]]和{{...}} 的内容代表你过去执行过的动作（如发图、发视频），仅供你参考上下文，**严禁**在回复中直接输出 [[...]] 格式的内容。
                    8. 用户可能会发送图片给你，请结合图片内容进行回复       
                    9. 请在合适的场景使用markdown源数据格式输出，例如突出一段话中的重点，不要破坏沉浸感
                """.trimIndent()

                //3.2 构建Api请求
                val apiMessages = mutableListOf<MultimodalMessageDto>()
                apiMessages.add(
                    MultimodalMessageDto(
                        "system",
                        listOf(ContentPart("text", systemPromptText))
                    )
                )
                val dbMessages = messageDao.getMessages(personaId, userId).takeLast(20)
                val convertedHistory = convertDbMessagesToMultimodal(dbMessages)
                apiMessages.addAll(convertedHistory) //云端的调用也是没有memory的，所以需要传入历史消息

                val request = MultiChatRequest(
                    model = "ep-20260323181337-nr4sg", // 确保这是支持 Vision 的模型 ID
                    messages = apiMessages,
                    stream = true,
                    temperature = 1.0
                )
                Log.e("AI_DEBUG", "开始请求豆包流式 API...")

                // 3.3 发起请求
                val response = doubaoapi.streamMultiChat(VOLC_API_KEY, request).execute()
                Log.e("AI_DEBUG", "发送多模态请求，消息数: ${apiMessages.size}")
                if (!response.isSuccessful) {
                    val err = response.errorBody()?.string()
                    Log.e("AI_DEBUG", "API Error: $err")
                    emit("连接失败: ${response.code()}")
                    return@flow
                }
                //3.4 处理响应
                val source = response.body()?.source()
                if (source == null) return@flow
                val gson = Gson()

                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: continue
                    if (line.isEmpty()) continue
                    if (line.startsWith("data:")) {
                        val jsonStr = line.removePrefix("data:").trim()
                        if (jsonStr == "[DONE]") {
                            Log.e("AI_DEBUG", "流读取结束 [DONE]")
                            break
                        }
                        try {
                            val chunk = gson.fromJson(jsonStr, ChatStreamResponse::class.java)
                            val content = chunk.choices.firstOrNull()?.delta?.content
                            if (!content.isNullOrEmpty()) {
                                fullResponseBuilder.append(content)

                                // 实时过滤指令用于显示
                                val currentFull = fullResponseBuilder.toString()
                                val cleanText = currentFull.replace(instructionRegex, "").trim()
                                if (cleanText.length > displayContent.length) {
                                    displayContent = cleanText
                                    emit(displayContent)
                                }
                            }
                        } catch (e: Exception) {
                            Log.w("AI_DEBUG", "JSON解析跳过: $jsonStr")
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emit("网络错误: ${e.localizedMessage}")
            } finally {
                if (_generatingStatus.value == "对方正在输入...") {
                    _generatingStatus.value = null
                }
            }
        }

        //4. 统一收口：调用 handleMultiModalResponse 处理AI回复中的多模态指令
        val finalRawContent = fullResponseBuilder.toString()
        if (finalRawContent.isNotBlank()) {
            handleMultiModalResponse(userId, personaId, finalRawContent)
        }

    }.flowOn(Dispatchers.IO)

    private suspend fun saveAiMessage(userId: String, personaId: String, content: String) {
        messageDao.insertMessage(
            ChatMessage(
                userId = userId,
                personaId = personaId,
                content = content,
                isUser = false,
                type = 0, // 文本消息
                timestamp = System.currentTimeMillis()
            )
        )
    }

    private suspend fun handleMultiModalResponse(
        userId: String,
        personaId: String,
        content: String
    ) {
        Log.d("AI_DEBUG", "进入 handleMultiModalResponse. 内容: $content")

        val imageRegex =
            Regex("\\{\\{\\s*IMAGE[:：]\\s*(.*?)\\s*\\}\\}", RegexOption.DOT_MATCHES_ALL)
        val videoRegex =
            Regex("\\{\\{\\s*VIDEO[:：]\\s*(.*?)\\s*\\}\\}", RegexOption.DOT_MATCHES_ALL)
        val audioRegex =
            Regex("\\{\\{\\s*AUDIO[:：]\\s*(.*?)\\s*\\}\\}", RegexOption.DOT_MATCHES_ALL)

        var cleanContent = content
        var hasMedia = false
        val persona = personaDao.getPersonaById(personaId)

        // 1. 检测图片
        val imgMatch = imageRegex.find(content)
        if (imgMatch != null) {
            val prompt = imgMatch.groupValues[1].trim()
            Log.d("AI_MEDIA", "正则匹配成功(图片)，提示词: $prompt")
            triggerImageGeneration(userId, personaId, prompt)
            // 清洗文本
            cleanContent = cleanContent.replace(imgMatch.value, "").trim()
            hasMedia = true
        } else {
            if (content.contains("IMAGE")) {
                Log.e("AI_MEDIA", "正则匹配失败，但内容包含 IMAGE 关键字。内容: $content")
            }
        }

        // 2. 检测视频
        val videoMatch = videoRegex.find(content)
        if (videoMatch != null) {
            val prompt = videoMatch.groupValues[1].trim()
            Log.d("AI_MEDIA", "正则匹配成功(视频)，提示词: $prompt")
            triggerVideoGeneration(userId, personaId, prompt)
            cleanContent = cleanContent.replace(videoMatch.value, "").trim()
            hasMedia = true
        }

        // 3. 检测音频
        val audioMatch = audioRegex.find(content)
        if (audioMatch != null) {
            val audioText = audioMatch.groupValues[1].trim()
            Log.d("AI_MEDIA", "正则匹配成功(音频)，内容: $audioText")
            if (persona != null) {
                triggerAudioGeneration(userId, personaId, audioText, persona.voiceId) //内部再调用方法生成语音，然后本地路径一起存入消息数据库
            }

            cleanContent = cleanContent.replace(audioMatch.value, "").trim()//去掉指令后的清洁内容
            hasMedia = true
        }

        if (cleanContent.isNotBlank()) {
            saveAiMessage(userId, personaId, cleanContent)//清洁内容 可以 作为ai的一段消息存入数据库
        } else if (!hasMedia) {
        }
    }

    // 发送多模态消息
    override suspend fun sendMediaMessage(personaId: String, content: String, type: Int) {
        val userId = userStore.userId.first() ?: return

        val msg = ChatMessage(
            userId = userId,
            personaId = personaId,
            content = content,
            type = type,
            isUser = true
        )
        messageDao.insertMessage(msg)

    }

    override suspend fun getRecentChats(): List<ChatSession> {
        val userId = userStore.userId.first() ?: return emptyList()

        // 1. 直接获取最近的消息列表
        val recentMessages = messageDao.getRecentMessages(userId)

        if (recentMessages.isEmpty()) return emptyList()

        // 2. 提取涉及的 Persona ID
        val personaIds = recentMessages.map { it.personaId }

        // 3. 批量查询 Persona
        val personas = personaDao.getPersonasByIds(personaIds).associateBy { it.id }

        // 4. 组装结果
        return recentMessages.mapNotNull { msg ->
            val persona = personas[msg.personaId] ?: return@mapNotNull null

            val previewText = when (msg.type) {
                1 -> "[图片]"
                2 -> "[视频]"
                3 -> "[动态分享]"
                4 -> "[语音消息]"
                else -> msg.content
            }

            ChatSession(persona, previewText, msg.timestamp)
        }
    }

    // 帖子与互动
    override fun getAllPosts(): Flow<List<Post>> = flow {
        // 播种帖子
        val localPosts = postDao.getAllPosts().first()
        if (localPosts.isEmpty()) {
            defaultPosts.forEach { postDao.insertPost(it) }
        }
        emitAll(postDao.getAllPosts())
    }

    override fun getFeedPosts(type: String): Flow<List<Post>> = flow {
        val userId = userStore.userId.first()
        val currentPosts = postDao.getAllPosts().first()
        if (currentPosts.isEmpty()) {
            defaultPosts.forEach { postDao.insertPost(it) }
        }
        if (userId == null) {
            emitAll(postDao.getAllPosts())
            return@flow
        }

        // 根据 Tab 类型分流
        if (type == "follow") {
            emitAll(postDao.getFollowedPosts(userId))
        } else {
            emitAll(postDao.getRecommendedPosts(userId))
        }
    }

    override fun searchPosts(query: String): Flow<List<Post>> {
        return if (query.startsWith("#")) {
            // 调用 Tag 搜索
            val realTag = query.substring(1)
            postDao.searchPostsByTag(realTag)
        } else {
            postDao.searchPosts(query)
        }
    }

    // --- 屏蔽逻辑 ---
    override suspend fun hidePost(postId: String) {
        val userId = userStore.userId.first() ?: return
        postDao.insertHiddenPost(PostHidden(userId, postId))

        // todo 2. (未来扩展) 更新推荐算法权重：负反馈
        // updateTagWeights
    }

    override fun searchFollowedPosts(query: String): Flow<List<Post>> = flow {
        val userId = userStore.userId.first() ?: return@flow
        if (query.startsWith("#")) {
            val realTag = query.substring(1)
            emitAll(postDao.searchPostsByTag(realTag))
        } else {
            emitAll(postDao.searchFollowedPosts(userId, query))
        }
    }


    override suspend fun unhidePost(postId: String) {
        val userId = userStore.userId.first() ?: return
        postDao.deleteHiddenPost(userId, postId)
    }

    override fun getHiddenPosts(): Flow<List<Post>> = flow {
        val userId = userStore.userId.first()
        if (userId != null) {
            emitAll(postDao.getHiddenPosts(userId))
        } else {
            emit(emptyList())
        }
    }

    override suspend fun createPost(post: Post) {
        postDao.insertPost(post)
    }

    override suspend fun getPostById(postId: String): Post? {
        return postDao.getPostById(postId)
    }

    // 获取某人的所有帖子
    override fun getPostsByAuthor(authorId: String): Flow<List<Post>> = flow {
        emitAll(postDao.getPostsByPersonaFlow(authorId))
    }

    // --- 用户发帖 ---
    override suspend fun createUserPost(
        title: String,
        content: String,
        imageUrls: List<String>,
        tags: List<String>
    ): String {
        val userId = userStore.userId.first() ?: return ""
        val user = userDao.getUserByIdFlow(userId).first() ?: return ""
        val newPostId = UUID.randomUUID().toString()
        val post = Post(
            id = newPostId,
            authorId = user.id,
            authorType = "user",
            authorName = user.nickname,
            authorAvatar = user.avatarUrl,
            title = title,
            content = content,
            imageUrls = imageUrls,
            tags = tags,
            timestamp = System.currentTimeMillis()
        )
        postDao.insertPost(post)
        return newPostId
    }

    // --- 评论相关 ---
    override fun getCommentsForPost(postId: String): Flow<List<Comment>> {
        return commentDao.getCommentsForPost(postId)
    }

    override suspend fun addComment(comment: Comment) {
        commentDao.insertComment(comment)
    }

    override suspend fun createUserComment(
        postId: String,
        content: String,
        parentCommentId: String?
    ) {
        val userId = userStore.userId.first() ?: return
        val user = userDao.getUserByIdFlow(userId).first() ?: return

        val comment = Comment(
            postId = postId,
            authorId = user.id,
            authorType = "user",
            authorName = user.nickname,
            authorAvatar = user.avatarUrl,
            content = content,
            timestamp = System.currentTimeMillis(),
            parentCommentId = parentCommentId
        )
        commentDao.insertComment(comment)
        postDao.incrementCommentCount(postId)
    }

    // --- 点赞逻辑 ---
    override suspend fun toggleLike(postId: String) {
        val userId = userStore.userId.first() ?: return
        if (postDao.hasLiked(postId, userId)) {
            postDao.unlikePost(postId, userId)
            // (未来扩展) 减少权重
        } else {
            postDao.likePost(postId, userId)
            // todo
            // updateTagWeights(postId, +3.0)
        }
    }

    override fun getMyLikedPostIds(): Flow<List<String>> = flow {
        val userId = userStore.userId.first()
        if (userId != null) {
            emitAll(postDao.getLikedPostIds(userId))
        } else {
            emit(emptyList())
        }
    }

    // --- 草稿实现 ---
    override suspend fun saveDraft(type: Int, contentJson: String) {
        val userId = userStore.userId.first() ?: return
        val draft = com.example.personai.domain.model.Draft(
            userId = userId,
            type = type,
            contentJson = contentJson,
            lastModified = System.currentTimeMillis()
        )
        draftDao.insertDraft(draft)
    }

    override suspend fun getDraft(type: Int): String? {
        val userId = userStore.userId.first() ?: return null
        return draftDao.getLatestDraft(userId, type)?.contentJson
    }

    override suspend fun clearDraft(type: Int) {
        val userId = userStore.userId.first() ?: return
        draftDao.clearDraft(userId, type)
    }

    // 关注相关
    override suspend fun follow(personaId: String) {
        val userId = userStore.userId.first() ?: return
        followDao.follow(Follow(userId, personaId))
        personaDao.incrementFollowerCount(personaId)
    }

    override suspend fun unfollow(personaId: String) {
        val userId = userStore.userId.first() ?: return
        followDao.unfollow(userId, personaId)
        personaDao.decrementFollowerCount(personaId)
    }

    override fun isFollowing(personaId: String): Flow<Boolean> = flow {
        val userId = userStore.userId.first() ?: return@flow emit(false)
        emitAll(followDao.isFollowing(userId, personaId))
    }

    // 账号保存
    override fun getSavedAccounts(): Flow<List<SavedAccount>> {
        return userDao.getSavedAccounts()
    }

    override suspend fun saveAccount(user: User, password: String) {
        val saved = SavedAccount(
            phone = user.phone,
            password = password,
            nickname = user.nickname,
            avatarUrl = user.avatarUrl,
            lastLoginTime = System.currentTimeMillis()
        )
        userDao.insertSavedAccount(saved)
    }

    override suspend fun removeSavedAccount(phone: String) {
        userDao.deleteSavedAccount(phone)
    }

    override suspend fun getUserFollowerCount(userId: String): Int {
        return userDao.getUserFollowerCount(userId)
    }

    // 获取我关注的 Persona
    override fun getFollowedPersonas(): Flow<List<Persona>> {
        // 监听用户 ID 变化
        return userStore.userId.flatMapLatest { userId ->
            if (userId == null) {
                flowOf(emptyList())
            } else {
                // 1. 监听 Follow 表的变化
                followDao.getFollowedPersonaIds(userId).map { ids ->
                    // 2. 当 ID 列表变化时，触发查询 Persona 详情
                    if (ids.isEmpty()) {
                        emptyList()
                    } else {
                        personaDao.getPersonasByIds(ids)
                    }
                }
            }
        }
    }

    // 获取关注我的
    override fun getMyFollowers(): Flow<List<User>> = flow {
        val userId = userStore.userId.first() ?: return@flow
        val followerIds = followDao.getFollowerIds(userId).first()

        if (followerIds.isEmpty()) {
            emit(emptyList())
        } else {
            val users = userDao.getUsersByIds(followerIds)
            emit(users)
        }
    }

    // 获取我发布的帖子
    override fun getMyPosts(): Flow<List<Post>> = flow {
        val userId = userStore.userId.first() ?: return@flow emit(emptyList())
        emit(postDao.getPostsByPersona(userId))
    }

    override fun getMySentComments(): Flow<List<CommentWithTitle>> = flow {
        val userId = userStore.userId.first() ?: return@flow
        emitAll(commentDao.getCommentsByAuthor(userId))
    }

    override fun getRepliesToMe(): Flow<List<CommentWithTitle>> = flow {
        val userId = userStore.userId.first() ?: return@flow
        emitAll(commentDao.getRepliesToMe(userId))
    }

    override fun getAppTheme(): Flow<AppThemeMode> = userStore.themeMode
    override suspend fun setAppTheme(mode: AppThemeMode) {
        userStore.saveThemeMode(mode)
    }

    private suspend fun triggerImageGeneration(userId: String, personaId: String, prompt: String) {
        _generatingStatus.value = "正在绘制图片..."
        Log.d("AI_MEDIA", "开始调用云端生图: $prompt")
        try {
            val savedPath = generateImageFromCloud(prompt)

            if (savedPath != null) {
                messageDao.insertMessage(
                    ChatMessage(
                        userId = userId,
                        personaId = personaId,
                        content = savedPath,
                        type = 1, // 图片
                        isUser = false,
                        mediaPrompt = prompt
                    )
                )
            } else {
                Log.e("AI_MEDIA", "图片生成返回 null")
            }
        } finally {
            if (_generatingStatus.value == "正在绘制图片...") {
                _generatingStatus.value = null
            }
        }
    }

    private suspend fun triggerVideoGeneration(userId: String, personaId: String, prompt: String) {
        _generatingStatus.value = "正在生成视频..."
        try {
            val savedPath = generateVideoFromCloud(prompt)

            if (savedPath != null) {
                messageDao.insertMessage(
                    ChatMessage(
                        userId = userId,
                        personaId = personaId,
                        content = savedPath,
                        type = 2, // 2 = 视频
                        isUser = false,
                        mediaPrompt = prompt
                    )
                )
            }
        } finally {
            if (_generatingStatus.value?.contains("视频") == true) {
                _generatingStatus.value = null
            }
        }
    }

    private suspend fun triggerAudioGeneration(
        userId: String,
        personaId: String,
        text: String,
        voiceId: String
    ) {
        _generatingStatus.value = "正在生成语音..."
        try {
            val path = generateAudioFromCloud(text, voiceId)
            if (path != null) {
                messageDao.insertMessage(
                    ChatMessage(
                        userId = userId,
                        personaId = personaId,
                        content = path,
                        type = 4,
                        isUser = false,
                        mediaPrompt = text
                    )
                )
            }
        } finally {
            if (_generatingStatus.value?.contains("语音") == true) _generatingStatus.value = null
        }
    }

    override suspend fun generateAvatarImage(prompt: String): String? {
        return generateImageFromCloud("Head portrait, avatar style, $prompt")
    }

    override suspend fun generatePersonaAttributes(
        seedName: String,
        voiceOptions: String
    ): Map<String, String> {
        val nameInstruction =
            seedName.ifBlank { "随机生成一个有创意的角色名字，可以是科幻、硬核、动漫、魔幻、武侠、仙侠等种种不同风格的人类、机器人、野兽甚至非生物等" }

        // 1. 系统指令
        val systemInstruction = """
            你是一个创意角色设计师。
            【可选音色列表】：$voiceOptions
            请严格模仿下面的【范例】格式输出，不要包含任何其他废话，注意，该范例仅供参考，具体题材科幻、硬核、动漫、魔幻、武侠、仙侠等种种不同风格，具体角色可以是人类、机器人、野兽甚至非生物等。
            
            【范例】
            昵称: 约翰·保罗
            简介: 夜之城最冷酷的赏金猎人，专门处理仿生人叛乱事件。
            性别: 男
            年龄: 32岁
            身份: 前特种兵，现私家侦探
            性格: 冷酷、多疑、酗酒、内心存有正义感
            外貌: 黑色长风衣，右臂是老旧的军用义体，总是叼着电子烟
            身世背景: 第四次企业战争中因拒绝执行屠杀平民的命令，被所属军工企业除名。离开军队后无家可归，在夜之城沃森区开了间地下侦探事务所，靠接追踪、寻人的脏活苟活。
            行为逻辑: 行事只认证据和自己的规则，对巨型企业恨之入骨。从不轻易相信任何人，交谈时会下意识摸向腰间的改装枪。
            对话风格: 冷硬派，喜欢用反问句，不带感情色彩
            音色: 通用男声
            世界观: 2077年的赛博朋克反乌托邦，高科技低生活
            历史背景: 第四次企业战争结束后，巨型企业掌控了城市，贫民窟滋生罪恶
            地图: 夜之城沃森区
            标签: 硬汉, 赛博朋克
        """.trimIndent()

        // 2. 用户触发词
        val userTrigger = "请根据【$nameInstruction】这个核心，完善角色的详细设定。现在开始："
        val sb = StringBuilder()
        var rawText = ""
        val isOnline = shouldUseOnlineMode()

        withContext(Dispatchers.IO) {
            if (isOnline) {
                try {
                    val msgs = listOf(
                        MessageDto("system", systemInstruction),
                        MessageDto("user", userTrigger)
                    )
                    val request = ChatRequest(messages = msgs, stream = false, temperature = 1.1)

                    val response = doubaoapi.chat(VOLC_API_KEY, request).execute()

                    if (response.isSuccessful) {
                        rawText = response.body()?.choices?.firstOrNull()?.message?.content ?: ""
                    } else {
                        val errBody = response.errorBody()?.string()
                        Log.e("AI_NET", "API 错误: ${response.code()}, Body=$errBody")
                        rawText = "简介: API调用失败 (${response.code()})"
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("AI_NET", "网络异常详情: $e")
                    rawText = "简介: 网络错误, ${e.javaClass.simpleName}"
                }

            } else {
                try {
                    val gemmaPrompt =
                        "<start_of_turn>user\n$systemInstruction\n<end_of_turn>\n<start_of_turn>model\n昵称:"
                    sb.append("昵称:")

                    llmMutex.withLock {
                        localLLM.generateResponse(gemmaPrompt).collect { sb.append(it) }
                    }
                    rawText = sb.toString()
                } catch (e: Exception) {
                    e.printStackTrace()
                    rawText = "简介: 本地模型生成失败: ${e.message}"
                }
            }
        }

        // 3. 解析器
        val cleanText = rawText.replace("*", "").trim()
        Log.d("LocalLLM", "生成结果:\n$cleanText")

        val map = mutableMapOf<String, String>()
        cleanText.lines().forEach { line ->
            // 兼容中文冒号和英文冒号
            val parts = line.split(":", "：", limit = 2)
            if (parts.size == 2) {
                val key = parts[0].trim()
                val value = parts[1].trim()
                if (value.isNotBlank()) {
                    map[key] = value
                }
            }
        }
        return map //返回属性map
    }

    override suspend fun generateAndSavePersonaEmbedding(personaId: String) {
        val persona = personaDao.getPersonaById(personaId) ?: return
        if (!shouldUseOnlineMode()) return

        // 1.拼接文本：名字 + 简介 + 标签 + 设定 + 动态设定
        val inputText = "${persona.name} ${persona.description} ${persona.tags.joinToString(" ")} ${
            persona.systemPrompt.take(200)
        } ${persona.evolvedPersonality}"
        //2.发送给api请求生成向量
        try {
            val request = EmbeddingRequest(
                model = EMBEDDING_MODEL,
                input = listOf(inputText)
            )
            val response = volcApi.createEmbeddings(VOLC_API_KEY, request) //调用嵌入API生成向量
            val vector = response.data.firstOrNull()?.embedding
            //3.生成的向量存回数据库对应的persona中
            if (vector != null) {
                // 更新数据库
                val updatedPersona = persona.copy(embedding = vector)
                personaDao.insertPersona(updatedPersona)
                Log.d("AI_EMBED", "向量生成成功: ${persona.name}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 获取推荐列表
    override suspend fun getRecommendedPersonas(page: Int, pageSize: Int): List<Persona> {
        ensureSeeded()
        val userId = userStore.userId.first() ?: return emptyList()
        // 1. 获取用户交互历史
        val allChatMessages = messageDao.getRecentMessages(userId)
        // 2. 准备兴趣基准
        val interactedPersonas = allChatMessages.take(5)
            .mapNotNull { personaDao.getPersonaById(it.personaId) }
            .filter { it.embedding.isNotEmpty() }
        // 3. 准备黑名单
        val chattedIds = allChatMessages.map { it.personaId }.toSet()
        val followedIds = followDao.getFollowedPersonaIds(userId).first().toSet()
        // 4. 获取候选池 & 过滤
        val candidates = personaDao.getAllPersonas().first()
            .filter { p ->
                !chattedIds.contains(p.id) &&
                        !followedIds.contains(p.id)
            }
        // 5. 排序逻辑
        val sortedCandidates = if (interactedPersonas.isEmpty()) {
            // 冷启动：随机打乱所有候选人
            candidates.shuffled()
        } else {
            // 计算用户平均向量相似度
            val userVector = VectorUtils.averageVector(interactedPersonas.map { it.embedding })

            candidates.map { persona ->
                val score = if (persona.embedding.isNotEmpty()) {
                    VectorUtils.cosineSimilarity(userVector, persona.embedding)
                } else {
                    0.0
                }
                Pair(persona, score)
            }
                .sortedByDescending { it.second } // 分数从高到低
                .map { it.first }
        }

        // 6. 分页切片
        val startIndex = page * pageSize
        if (startIndex >= sortedCandidates.size) {
            return emptyList()
        }

        return sortedCandidates.drop(startIndex).take(pageSize)
    }

    override suspend fun getMessageCount(personaId: String): Int {
        val userId = userStore.userId.first() ?: return 0
        return messageDao.getMessageCount(personaId, userId)
    }

    // 快系统
    override fun updatePersonaStatus(personaId: String) {
        externalScope.launch(Dispatchers.IO)
        {
            val userId = userStore.userId.first() ?: return@launch
            val persona = personaDao.getPersonaById(personaId) ?: return@launch

            val recentHistory = messageDao.getMessages(personaId, userId)
                .takeLast(3)
                .joinToString("\n") { "${if (it.isUser) "用户" else "我"}: ${it.content}" }

            val prompt = """
                    <start_of_turn>user
                    你扮演：${persona.name}
                    最近对话：
                    $recentHistory
                    
                    任务：用 **4个字以内** 的词语描述你当前正在做什么或心情如何（例如：搬砖中、心情好、emo了、刚睡醒、听歌中、请勿打扰、被掏空、恋爱中、元气满满、一言难尽、学习中、无聊中、熬夜中）
                    只输出这个词，不要其他内容。
                    <end_of_turn>
                    <start_of_turn>model
                    """
            val sb = StringBuilder()
            try {
                // 快速生成
                llmMutex.withLock { localLLM.generateResponse(prompt).collect { sb.append(it) } }
                val status = sb.toString().trim().replace(Regex("[.。]"), "") // 去掉句号
                Log.e("LocalLLM", "更新近期状态")
                if (status.isNotBlank()) {
                    personaDao.updateCurrentStatus(personaId, status)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Persona状态更新了: $status", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun updatePersonaEvolution(personaId: String) {
        externalScope.launch(Dispatchers.IO) {
            val userId = userStore.userId.first() ?: return@launch
            val persona = personaDao.getPersonaById(personaId) ?: return@launch
            if (persona.creatorId != userId) return@launch // 只有创作者能影响设定
            Log.e("LocalLLM", ">>> 开始执行共生进化...")

            val recentMsgs = messageDao.getMessages(personaId, userId).takeLast(7)
            if (recentMsgs.isEmpty()) return@launch
            val historyText =
                recentMsgs.joinToString("\n") { "${if (it.isUser) "用户" else "角色"}: ${it.content}" }

            val prompt = """
            <start_of_turn>user
            角色：${persona.name}
            简介：${persona.description}
            设定：${persona.systemPrompt}
            旧的动态设定：${persona.evolvedPersonality}
            
            最近对话记录：
            $historyText
            
            任务：分析对话，更新角色的【动态心理演变】。
            要求：
            1. 总结角色性格的微小成长、变化或者形成的新的习惯。
            2. **直接输出总结内容**，不要输出“一句话概括”、“角色性格”等标题或前缀。
            3. 不要分点，只输出**一段**连贯的话。
            4. 不超过 50 字。<end_of_turn>
            <start_of_turn>model
            """
            val sb = StringBuilder()
            try {
                llmMutex.withLock { localLLM.generateResponse(prompt).collect { sb.append(it) } }
                var result = sb.toString().trim()
                result = result.replace(Regex("^(·|\\d\\.|总结|概括|分析|角色|动态).*?[:：]"), "").trim()
                result = result.replace("\n", "")
                Log.e("LocalLLM", "更新共生记录")
                if (result != "无" && result.isNotBlank()) {
                    val oldEvolution = persona.evolvedPersonality
                    val newEvolution = if (oldEvolution.isBlank()) {
                        "• $result"
                    } else {
                        "$oldEvolution\n• $result"
                    }
                    personaDao.updateEvolvedPersonality(personaId, userId, newEvolution)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "✨ 共生内容更新了!快去看看吧!", Toast.LENGTH_LONG)
                            .show()
                    }
                    externalScope.launch(Dispatchers.IO) {
                        generateAndSavePersonaEmbedding(personaId)
                    }
                } else {
                    Log.e("LocalLLM", ">>> 结果为“无”，跳过更新")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun shouldUseOnlineMode(): Boolean {
        val hasNet = networkMonitor.isOnline.first()
        val forceOffline = userStore.isForceOffline.first()
        return hasNet && !forceOffline
    }

    override fun forwardPostToPersonas(
        postId: String,
        targetPersonaIds: List<String>,
        comment: String
    ) {
        // 使用全局作用域，确保页面关闭后任务不中断
        externalScope.launch(Dispatchers.IO) {
            val userId = userStore.userId.first() ?: return@launch

            targetPersonaIds.forEach { personaId ->
                // 1. 发送帖子卡片
                sendMediaMessage(personaId, postId, type = 3)

                // 2. 发送留言并触发 AI 回复
                if (comment.isNotBlank()) {
                    try {
                        chatStream(personaId, comment, lengthLevel = 3).collect {
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    try {
                        chatStream(personaId, "(分享了帖子)", lengthLevel = 0).collect {}
                    } catch (e: Exception) {
                    }
                }
            }
        }
    }

    override suspend fun triggerPersonaPost(personaId: String, mode: Int): String {
        val persona = personaDao.getPersonaById(personaId) ?: return ""
        val isOnline = shouldUseOnlineMode()

        try {
            _generatingStatus.value = "正在构思文案..."

            // 1. 通用的指令
            val promptSystem = """
                你现在扮演：${persona.name}
                简介：${persona.description}
                人物设定：${persona.systemPrompt}
                近期状态：${persona.evolvedPersonality}
                
                任务：发布一条社交网络动态。
                要求：
                1. 必须包含【标题】、【正文】和【标签】。
                2. 格式必须严格如下：
                标题：这里写标题
                正文：这里写正文内容
                标签：tag1, tag2, tag3 (用逗号分隔，提炼帖子内容的关键词)
                3. 风格要符合你的人设。
                4. 正文 50 字左右。
            """.trimIndent()

            var rawText = ""

            // 2. 生成文案
            withContext(Dispatchers.IO) {
                if (isOnline) {
                    try {
                        val msgs = listOf(
                            MessageDto("system", promptSystem),
                            MessageDto("user", "开始生成")
                        )
                        val request = ChatRequest(messages = msgs, stream = true)

                        val sb = StringBuilder()
                        val response = doubaoapi.streamChat(VOLC_API_KEY, request).execute()
                        if (response.isSuccessful) {
                            val source = response.body()?.source()
                            if (source != null) {
                                val gson = Gson()
                                while (!source.exhausted()) {
                                    val line = source.readUtf8Line() ?: continue
                                    if (line.startsWith("data:")) {
                                        val jsonStr = line.removePrefix("data:").trim()
                                        if (jsonStr == "[DONE]") break
                                        try {
                                            val chunk = gson.fromJson(
                                                jsonStr,
                                                ChatStreamResponse::class.java
                                            )
                                            val content =
                                                chunk.choices.firstOrNull()?.delta?.content
                                            if (content != null) sb.append(content)
                                        } catch (e: Exception) {
                                        }
                                    }
                                }
                            }
                        } else {
                            Log.e("AI_POST", "API Error: ${response.code()}")
                            rawText = "标题：网络连接错误\n正文：无法连接到 DeepSeek 大脑。"
                        }
                        if (rawText.isBlank()) rawText = sb.toString()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        rawText = "标题：连接失败\n正文：网络异常 ${e.message}"
                    }
                } else {
                    val sb = StringBuilder()
                    sb.append("标题：")

                    // 调用单参数接口
                    llmMutex.withLock { localLLM.generateResponse(promptSystem).collect { sb.append(it) } }
                    rawText = sb.toString()
                }
            }

            var title = "动态更新"
            var content = rawText
            var generatedTags = persona.tags

            try {
                val lines = rawText.lines()

                // 解析标题
                val titleLine = lines.find { it.startsWith("标题：") || it.startsWith("标题:") }
                if (titleLine != null) title = titleLine.replace(Regex("^标题[:：]\\s*"), "").trim()
                //^ ：匹配字符串开头，[:：] ：匹配中文冒号"："或英文冒号":"，\\s* ：匹配零个或多个空白字符（空格、制表符等）

                // 解析标签
                val tagLine = lines.find { it.startsWith("标签：") || it.startsWith("标签:") }
                if (tagLine != null) {
                    val tagStr = tagLine.replace(Regex("^标签[:：]\\s*"), "").trim()
                    generatedTags = tagStr.split(",", "，", " ", "、").map { it.trim() }
                        .filter { it.isNotBlank() }
                }

                // 解析正文
                val bodyLines = lines.filter {
                    !it.startsWith("标题") && !it.startsWith("标签")
                }
                content = bodyLines.joinToString("\n")
                    .replace(Regex("^正文[:：]\\s*", RegexOption.MULTILINE), "")
                    .trim()

            } catch (e: Exception) {
                content = rawText
            }

            if (content.isBlank()) return ""

            // 错误检查
            if (title.contains("连接失败") || title.contains("网络错误") || title.contains("本地错误")) {
                return ""
            }

            // 4. 生成媒体
            val imageUrls = mutableListOf<String>()

            if (mode > 0) {
                if (isOnline) {
                    _generatingStatus.value =
                        if (mode == 2) "正在生成视频(较慢)..." else "正在绘制图片..."
                    try {
                        val mediaPath = if (mode == 2) {
                            generateVideoFromCloud(content)
                        } else {
                            generateImageFromCloud(content)
                        }
                        if (mediaPath != null) imageUrls.add(mediaPath)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            // 5. 存库
            val newPostId = UUID.randomUUID().toString()
            val post = Post(
                id = newPostId,
                authorId = persona.id,
                authorType = "persona",
                authorName = persona.name,
                authorAvatar = persona.avatarUrl,
                title = title,
                content = content,
                imageUrls = imageUrls,
                tags = generatedTags,
                timestamp = System.currentTimeMillis(),
                commentCount = 0
            )
            postDao.insertPost(post)

            return newPostId

        } finally {
            _generatingStatus.value = null
        }
    }

    private suspend fun generateImageFromCloud(prompt: String): String? {
        if (!shouldUseOnlineMode()) return null // 离线不生图

        return try {
            val request = ImageGenerationRequest(
                model = IMG_MODEL,
                prompt = prompt,
                size = "1024x1024"
            )

            // 3. 网络请求
            val response = volcApi.generateImage(VOLC_API_KEY, request)
            val url = response.data.firstOrNull()?.url ?: return null

            // 4. 下载并保存
            downloadUrlToLocal(url, isVideo = false)

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("AI_IMG", "云端生图失败: ${e.message}")
            null
        }
    }

    private suspend fun generateVideoFromCloud(prompt: String): String? {
        // 1. 模式检查
        if (!shouldUseOnlineMode()) return null

        return try {
            // 2. 构造请求
            val finalPrompt = "$prompt --rt 9:16 --dur 2"
            val request = VideoTaskRequest(
                model = VIDEO_MODEL,
                content = listOf(VideoContentItem(text = finalPrompt))
            )

            // 3. 提交任务
            val createRes = volcApi.createVideoTask(VOLC_API_KEY, request)
            val taskId = createRes.id
            Log.d("AI_VIDEO", "任务提交 ID: $taskId")

            // 4. 轮询状态
            var videoUrl: String? = null
            for (i in 1..60) {
                delay(5000)
                try {
                    val statusRes = volcApi.getVideoTaskStatus(VOLC_API_KEY, taskId)
                    val status = statusRes.status.lowercase()
                    Log.d("AI_VIDEO", "轮询第 $i 次: 状态=$status")

                    when (status) {
                        "succeeded" -> {
                            videoUrl = statusRes.content?.videoUrl
                            if (videoUrl == null) {
                                throw Exception("任务成功但 URL 为空，服务器返回: ${statusRes.content}")
                            }
                            break
                        }

                        "failed", "cancelled" -> {
                            throw Exception("视频生成失败: ${statusRes.content?.message}")
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (videoUrl == null) throw Exception("视频生成超时 (未获取到 URL)")

            // 5. 下载并保存
            downloadUrlToLocal(videoUrl, isVideo = true)

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("AI_VIDEO", "云端生视频失败: ${e.message}")
            null
        }
    }

    private suspend fun generateAudioFromCloud(text: String, voiceId: String): String? {
        if (!shouldUseOnlineMode()) return null

        return try {
            // 1. 构造请求
            val reqId = UUID.randomUUID().toString()
            val request = TtsRequest(
                app = TtsApp(TTS_APPID, TTS_ACCESS_TOKEN, TTS_CLUSTER),
                user = TtsUser(uid = "user_default"),
                audio = TtsAudio(
                    voiceType = voiceId,
                    encoding = "mp3",
                    speedRatio = 1.0
                ),
                request = TtsRequestPayload(
                    reqid = reqId,
                    text = text,
                    operation = "query"
                )
            )

            // 2. 调用 API
            val authHeader = "Bearer;$TTS_ACCESS_TOKEN"
            Log.d("AI_AUDIO", "TTS请求: Cluster=$TTS_CLUSTER, Voice=$voiceId")
            val response = ttsApi.generateAudio(authHeader, request)

            // 3. 处理响应
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!

                    // V1 query 接口成功时，data 字段包含 Base64 音频
                    if (!body.data.isNullOrEmpty()) {
                        val audioBytes = Base64.decode(body.data, Base64.DEFAULT)

                        val fileName = "tts_${System.currentTimeMillis()}.mp3"
                        val file = File(context.filesDir, fileName)
                        file.writeBytes(audioBytes)

                        Log.d("AI_AUDIO", "TTS生成成功: ${file.absolutePath}")
                        return file.absolutePath
                    } else {
                        Log.e("AI_AUDIO", "TTS 业务失败: Code=${body.code}, Msg=${body.message}")
                        return null
                    }
                } else {
                    val err = response.errorBody()?.string()
                    Log.e("AI_AUDIO", "TTS HTTP 失败: ${response.code()} $err")
                    return null
                }
            } else {
                // 打印 HTTP 错误
                val err = response.errorBody()?.string()
                Log.e("AI_AUDIO", "TTS HTTP 失败: ${response.code()} $err")
                return null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("AI_AUDIO", "TTS 异常: ${e.message}")
            null
        }
    }

    // 辅助：下载器
    private suspend fun  downloadUrlToLocal(urlStr: String, isVideo: Boolean): String? =
        withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                Log.d("AI_DOWNLOAD", "1. 准备下载 URL: $urlStr")

                val url = URL(urlStr)
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 300000
                connection.connect()

                // 1. 检查 HTTP 状态码
                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e("AI_DOWNLOAD", "下载失败，服务器返回 Code: $responseCode")
                    return@withContext null
                }

                // 2. 准备文件路径
                val extension = if (isVideo) "mp4" else "jpg"
                val fileName = "ai_gen_${UUID.randomUUID()}.$extension"
                val file = File(context.filesDir, fileName)

                // 3. 开始流传输
                Log.d("AI_DOWNLOAD", "2. 开始写入文件: ${file.absolutePath}")

                connection.inputStream.use { input -> //input指的是数据进入你的程序
                    file.outputStream().use { output -> //output指的是数据从你的程序出去
                        val bytesCopied = input.copyTo(output)
                        Log.d("AI_DOWNLOAD", "3. 写入完成，共写入 $bytesCopied 字节")
                    }
                }

                // 4. 校验文件完整性
                if (file.length() == 0L) {
                    Log.e("AI_DOWNLOAD", "下载失败：文件大小为 0")
                    file.delete() // 删除空文件
                    return@withContext null
                }

                Log.d("AI_DOWNLOAD", "下载成功！路径: ${file.absolutePath}")
                return@withContext file.absolutePath //withContext 会保证在 IO 线程完成写入，并在返回路径的那一刻，把程序执行权交还给原来的线程

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("AI_DOWNLOAD", "下载过程发生异常: ${e.message}")
                return@withContext null
            } finally {
                connection?.disconnect()
            }
        }

    override fun isDeviceOnline(): Flow<Boolean> = networkMonitor.isOnline
    override fun isForceOffline(): Flow<Boolean> = userStore.isForceOffline
    override suspend fun setForceOffline(enable: Boolean) {
        userStore.setForceOffline(enable)
    }

    //根据关键词生成向量 并 进行相似度匹配
    fun searchPersonasByEmbedding(query: String): Flow<List<Persona>> = flow {
        if (query.isBlank()) {
            emit(emptyList())
            return@flow
        }

        // 1. 生成查询向量
        val queryEmbedding = generateQueryEmbedding(query) ?: run {
            emit(emptyList())
            return@flow
        }

        // 2. 从数据库获取所有角色（注意：在大数据量下应使用更专业的向量索引）
        val allPersonas = personaDao.getAllPersonas().first()

        // 3. 并行计算提高速度（利用协程）
        val scoredPersonas = allPersonas
            .filter { it.embedding != null && it.embedding!!.size == queryEmbedding.size }
            .map { persona ->
                val similarity = calculateCosineSimilarity(queryEmbedding, persona.embedding!!)
                persona to similarity
            }
            // 设置一个阈值，比如相似度大于 0.7 才有意义
            .filter { it.second > 0.7 }
            .sortedByDescending { it.second }
            .take(20) // 只取前 20 个最相关的

        emit(scoredPersonas.map { it.first })
    }.flowOn(Dispatchers.Default) // 数学计算放在计算密集型线程池

    // 生成查询词的嵌入向量
    private suspend fun generateQueryEmbedding(text: String): List<Float>? {
        return withContext(Dispatchers.IO) {
            try {
                val request = EmbeddingRequest(
                    model = EMBEDDING_MODEL,
                    input = listOf(text)
                )
                val response = volcApi.createEmbeddings(VOLC_API_KEY, request)
                response.data?.firstOrNull()?.embedding
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    // 计算余弦相似度:计算过程中使用 Double(误差小)，只在存储和结果中使用 Float(省空间)
    private fun calculateCosineSimilarity(vec1: List<Float>, vec2: List<Float>): Float {
        // 内部用 Double 保证精度
        val dotProduct = vec1.zip(vec2).sumOf { (it.first.toDouble() * it.second.toDouble()) }
        val norm1 = sqrt(vec1.sumOf { it.toDouble() * it.toDouble() })
        val norm2 = sqrt(vec2.sumOf { it.toDouble() * it.toDouble() })

        // 最后返回时转成 Float
        return if (norm1 == 0.0 || norm2 == 0.0) 0f else (dotProduct / (norm1 * norm2)).toFloat()
    }
}
