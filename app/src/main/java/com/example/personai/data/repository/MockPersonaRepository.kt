package com.example.personai.data.repository

import com.example.personai.domain.model.ChatMessage
import com.example.personai.domain.model.ChatSession
import com.example.personai.domain.model.Comment
import com.example.personai.domain.model.CommentWithTitle
import com.example.personai.domain.model.HistoryItem
import com.example.personai.domain.model.Persona
import com.example.personai.domain.model.Post
import com.example.personai.domain.model.User
import com.example.personai.domain.repository.PersonaRepository
import com.example.personai.ui.theme.AppThemeMode
import com.example.personai.utils.VectorUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockPersonaRepository @Inject constructor() : PersonaRepository {
    private val _users = mutableListOf<User>()
    private val _currentUserFlow = MutableStateFlow<User?>(null)
    private val _browsingHistoryMap = mutableMapOf<String, Long>()
    private val _personas = mutableListOf(
        Persona(
            id = "1",
            name = "赛博侦探 J",
            description = "霓虹灯下的落魄侦探。",
            systemPrompt = "你是一个冷酷的赛博朋克侦探...",
            dialogueStyle = "",
            avatarUrl = "https://api.dicebear.com/9.x/bottts/png?seed=J",
            creatorId = "system",
            tags = listOf("赛博朋克", "悬疑")
        ),
        Persona(
            id = "2",
            name = "心理医生 Alice",
            description = "温柔、治愈、倾听。",
            systemPrompt = "你是一个温柔的心理咨询师...",
            dialogueStyle = "",
            avatarUrl = "https://api.dicebear.com/9.x/notionists/png?seed=Alice",
            creatorId = "system",
            tags = listOf("治愈", "心理学")
        )
    )
    private val _posts = mutableListOf(
        Post(
            id = "1",
            authorId = "1",
            authorType = "persona", // 新字段
            authorName = "赛博侦探 J", // 新字段
            authorAvatar = "https://api.dicebear.com/9.x/bottts/png?seed=J", // 新字段
            title = "今天的雨有点大",
            content = "刚解决完沃森区的一个案子。义体过热了...",
            imageUrls = listOf("https://api.dicebear.com/9.x/shapes/png?seed=CyberCity"),
            likeCount = 42,
            timestamp = System.currentTimeMillis() - 3600000,
            tags = listOf("赛博朋克", "心情", "夜之城")
        ),
        Post(
            id = "2",
            authorId = "2",
            authorType = "persona",
            authorName = "心理医生 Alice",
            authorAvatar = "https://api.dicebear.com/9.x/notionists/png?seed=Alice",
            title = "关于倾听的艺术",
            content = "很多人认为沟通的关键在于表达...",
            imageUrls = listOf("https://api.dicebear.com/9.x/shapes/png?seed=Relax"),
            likeCount = 128,
            timestamp = System.currentTimeMillis() - 7200000,
            tags = listOf("心理学", "治愈", "人际沟通")
        )
    )
    private val defaultUser = User(
        id = "system",
        phone = "00000000000",
        password = "000",
        nickname = "SystemOP",
        avatarUrl = "https://api.dicebear.com/9.x/avataaars/png?seed=SystemOP"
    )

    private val _comments = mutableListOf<Comment>()
    private val messageList = mutableListOf<ChatMessage>()
    private val _likedPostIds = mutableSetOf<String>()
    private val _browsingHistoryIds = mutableListOf<String>()
    private val _follows = mutableSetOf<String>()

    private val _hiddenPosts = mutableSetOf<String>()

    private val _drafts = mutableMapOf<Int, String>()

    override suspend fun login(phone: String, password: String): User? {
        var user = _users.find { it.phone == phone }
        if (user == null && phone.isNotEmpty()) {
            user = User(
                id = UUID.randomUUID().toString(),
                phone = phone,
                password = password,
                nickname = "Mock用户-${phone.takeLast(4)}",
                avatarUrl = "https://api.dicebear.com/9.x/avataaars/png?seed=$phone"
            )
            _users.add(user)
        }

        if (user != null && user.password == password) {
            _currentUserFlow.value = user
            return user
        }
        return null
    }

    override suspend fun checkUserExists(phone: String): Boolean {
        return _users.any { it.phone == phone }
    }

    override suspend fun register(user: User): User? {
        if (_users.any { it.phone == user.phone }) return null
        _users.add(user)
        _currentUserFlow.value = user
        return user
    }

    override suspend fun logout() {
        _currentUserFlow.value = null
    }

    override suspend fun getUserById(userId: String): User? {
        return _users.find { it.id == userId }
    }

    override fun getCurrentUser(): Flow<User?> {
        return _currentUserFlow.asStateFlow()
    }

    override fun getBrowsingHistory(): Flow<List<HistoryItem>> = flow {
        val historyItems = _browsingHistoryMap.mapNotNull { (postId, time) ->
            val post = _posts.find { it.id == postId }
            if (post != null) {
                HistoryItem(post = post, browsedTime = time)
            } else {
                null
            }
        }.sortedByDescending { it.browsedTime }

        emit(historyItems)
    }

    override suspend fun addBrowsingHistory(postId: String) {
        _browsingHistoryMap[postId] = System.currentTimeMillis()
    }

    override fun getAllPersonas(): Flow<List<Persona>> = flow {
        emit(_personas.filter { it.creatorId != "user" })
    }

    override fun searchPersonas(query: String): Flow<List<Persona>> = flow {
        val result = _personas.filter {
            it.name.contains(query, true) || it.description.contains(query, true)
        }
        emit(result)
    }

    override fun getUserPersonas(): Flow<List<Persona>> = flow {
        val userId = _currentUserFlow.value?.id
        if (userId != null) {
            emit(_personas.filter { it.creatorId == userId }) // 匹配当前用户ID
        } else {
            emit(emptyList())
        }
    }

    override suspend fun getPersonaById(id: String): Persona? {
        val persona = _personas.find { it.id == id } ?: return null

        val realFollowerCount = _follows.count { it.endsWith("_$id") }

        val realInteractionCount = messageList.count { it.personaId == id && it.isUser }

        return persona.copy(
            followerCount = realFollowerCount,
            interactionCount = realInteractionCount
        )
    }

    override suspend fun addPersona(persona: Persona) {
        _personas.add(0, persona)
    }


    override suspend fun getMessages(personaId: String): List<ChatMessage> {
        val userId = _currentUserFlow.value?.id ?: return emptyList()
        // 直接返回过滤后的列表，不再检查 isEmpty 插入 openingLine
        return messageList.filter { it.personaId == personaId && it.userId == userId }
    }

    override fun getMessagesFlow(personaId: String): Flow<List<ChatMessage>> = flow {
        val userId = _currentUserFlow.value?.id ?: return@flow
        val msgs = messageList.filter { it.personaId == personaId && it.userId == userId }
        emit(msgs)
    }

    private val _generatingStatus = MutableStateFlow<String?>(null)
    override val generatingStatus: StateFlow<String?> = _generatingStatus.asStateFlow()

    override suspend fun getRecentChats(): List<ChatSession> {
        val userId = _currentUserFlow.value?.id ?: return emptyList()

        val userMessages = messageList.filter { it.userId == userId }
        val chattedPersonaIds = userMessages.map { it.personaId }.distinct()

        val sessions = mutableListOf<ChatSession>()

        for (pId in chattedPersonaIds) {
            val persona = getPersonaById(pId) ?: continue
            val lastMsg = userMessages
                .filter { it.personaId == pId }
                .maxByOrNull { it.timestamp }

            if (lastMsg != null) {
                val previewText = when (lastMsg.type) {
                    1 -> "[图片]"
                    2 -> "[视频]"
                    3 -> "[动态分享] "
                    4 -> "[语音消息]"
                    else -> lastMsg.content
                }

                sessions.add(ChatSession(persona, previewText, lastMsg.timestamp))
            }
        }
        return sessions.sortedByDescending { it.lastTime }
    }

    override fun chatStream(personaId: String, userContent: String, lengthLevel: Int): Flow<String> = flow {
        val userId = _currentUserFlow.value?.id ?: return@flow
        _generatingStatus.value = "对方正在输入..."
        // 1. 存用户消息
        val userMsg = ChatMessage(
            userId = userId,
            personaId = personaId,
            content = userContent,
            isUser = true
        )
        messageList.add(userMsg)

        // 2. Mock 回复
        val response = "Mock回复($userId): $userContent"
        val sb = StringBuilder()
        response.forEach {
            delay(50)
            sb.append(it)
            emit(sb.toString())
        }

        val aiMsg = ChatMessage(
            userId = userId,
            personaId = personaId,
            content = response,
            isUser = false
        )
        messageList.add(aiMsg)
    }

    override suspend fun sendMediaMessage(personaId: String, content: String, type: Int) {
        val userId = _currentUserFlow.value?.id ?: return

        val msg = ChatMessage(
            userId = userId,
            personaId = personaId,
            content = content,
            type = type,
            isUser = true
        )
        messageList.add(msg)

        val aiReply = ChatMessage(
            userId = userId,
            personaId = personaId,
            content = "我收到了你的" + if(type==1) "图片" else "分享",
            isUser = false
        )
        messageList.add(aiReply)
    }

    override fun getFeedPosts(type: String): Flow<List<Post>> = flow {
        val userId = _currentUserFlow.value?.id ?: run {
            emit(_posts)
            return@flow
        }

        val visiblePosts = _posts.filter { post ->
            !_hiddenPosts.contains("${userId}_${post.id}")
        }

        val result = if (type == "follow") {
            visiblePosts.filter { post ->
                _follows.contains("${userId}_${post.authorId}")
            }
        } else {
            visiblePosts
        }

        emit(result)
    }

    override fun getAllPosts(): Flow<List<Post>> = flow {
        emit(_posts.toList())
    }

    override fun searchPosts(query: String): Flow<List<Post>> = flow {
        emit(_posts.filter { it.title.contains(query, true) || it.content.contains(query, true) })
    }

    override fun searchFollowedPosts(query: String): Flow<List<Post>> = flow {
        val userId = _currentUserFlow.value?.id ?: run {
            emit(emptyList())
            return@flow
        }

        val followedAuthorIds = _follows
            .filter { it.startsWith("${userId}_") }
            .map { it.split("_")[1] }
            .toSet()

        val result = _posts.filter { post ->
            val isFollowed = post.authorId in followedAuthorIds

            val isMatch = post.title.contains(query, true) ||
                    post.content.contains(query, true) ||
                    post.authorName.contains(query, true)

            isFollowed && isMatch
        }

        emit(result)
    }

    override suspend fun createPost(post: Post) {
        _posts.add(0, post)
    }

    override suspend fun createUserPost(
        title: String,
        content: String,
        imageUrls: List<String>,
        tags: List<String>
    ): String {
        val user = _currentUserFlow.value ?: return ""

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

        _posts.add(0, post)

        return newPostId
    }

    override suspend fun getPostById(postId: String): Post? {
        return _posts.find { it.id == postId }
    }

    override fun getPostsByAuthor(authorId: String): Flow<List<Post>> = flow {
        emit(_posts.filter { it.authorId == authorId })
    }

    override fun getCommentsForPost(postId: String): Flow<List<Comment>> = flow {
        emit(_comments.filter { it.postId == postId })
    }

    override suspend fun addComment(comment: Comment) {
        _comments.add(comment)
    }

    override suspend fun createUserComment(postId: String, content: String, parentCommentId: String?) {
        val user = _currentUserFlow.value ?: return
        val comment = Comment(
            postId = postId,
            authorId = user.id,
            authorType = "user",
            authorName = user.nickname,
            authorAvatar = user.avatarUrl,
            content = content
        )
        _comments.add(comment)
    }

    override fun isFollowing(personaId: String): Flow<Boolean> = flow {
        emit(_follows.contains(personaId))
    }

    override suspend fun follow(personaId: String) {
        _follows.add(personaId)
    }

    override suspend fun unfollow( personaId: String) {
        _follows.remove(personaId)
    }

    override suspend fun toggleLike(postId: String) {
        if (_likedPostIds.contains(postId)) {
            _likedPostIds.remove(postId)
        } else {
            _likedPostIds.add(postId)
        }
    }

    override suspend fun hidePost(postId: String) {
        val userId = _currentUserFlow.value?.id ?: return
        _hiddenPosts.add("${userId}_$postId")
    }

    override suspend fun unhidePost(postId: String) {
        val userId = _currentUserFlow.value?.id ?: return
        _hiddenPosts.remove("${userId}_$postId")
    }

    override fun getHiddenPosts(): Flow<List<Post>> = flow {
        val userId = _currentUserFlow.value?.id ?: return@flow
        val hiddenIds = _hiddenPosts.filter { it.startsWith("${userId}_") }
            .map { it.split("_")[1] }

        emit(_posts.filter { it.id in hiddenIds })
    }

    override fun getMyLikedPostIds(): Flow<List<String>> = flow {
        emit(_likedPostIds.toList())
    }

    override suspend fun saveDraft(type: Int, contentJson: String) {
        _drafts[type] = contentJson
    }

    override suspend fun getDraft(type: Int): String? {
        return _drafts[type]
    }

    override suspend fun clearDraft(type: Int) {
        _drafts.remove(type)
    }

    override suspend fun updateNickname(userId: String, newNickname: String) {
        val user = _users.find { it.id == userId } ?: return

        val newUser = user.copy(nickname = newNickname)
        _users.remove(user)
        _users.add(newUser)

        if (_currentUserFlow.value?.id == userId) {
            _currentUserFlow.value = newUser
        }

        val savedIndex = _savedAccounts.indexOfFirst { it.phone == user.phone }
        if (savedIndex != -1) {
            val oldSaved = _savedAccounts[savedIndex]
            _savedAccounts[savedIndex] = oldSaved.copy(nickname = newNickname)
        }
    }

    override suspend fun updateUserAvatar(userId: String, avatarUrl: String) {
        val user = _users.find { it.id == userId } ?: return

        val newUser = user.copy(avatarUrl = avatarUrl)
        _users.remove(user)
        _users.add(newUser)

        if (_currentUserFlow.value?.id == userId) {
            _currentUserFlow.value = newUser
        }

        val savedIndex = _savedAccounts.indexOfFirst { it.phone == user.phone }
        if (savedIndex != -1) {
            val oldSaved = _savedAccounts[savedIndex]
            _savedAccounts[savedIndex] = oldSaved.copy(avatarUrl = avatarUrl)
        }
    }

    override fun getFollowedPersonas(): Flow<List<Persona>> = flow {
        val userId = _currentUserFlow.value?.id
        if (userId != null) {
            val followedIds = _follows
                .filter { it.startsWith("${userId}_") }
                .map { it.split("_")[1] }
                .toSet()
            val result = _personas.filter { it.id in followedIds }
            emit(result)
        } else {
            emit(emptyList())
        }
    }

    override fun getMyPosts(): Flow<List<Post>> = flow {
        val userId = _currentUserFlow.value?.id
        if (userId != null) {
            val myPosts = _posts.filter { it.authorId == userId }
            emit(myPosts)
        } else {
            emit(emptyList())
        }
    }

    private val _savedAccounts = mutableListOf<com.example.personai.domain.model.SavedAccount>()

    override fun getSavedAccounts(): Flow<List<com.example.personai.domain.model.SavedAccount>> = flow {
        emit(_savedAccounts)
    }

    override suspend fun saveAccount(user: User, password: String) {
        val saved = com.example.personai.domain.model.SavedAccount(
            phone = user.phone,
            password = password,
            nickname = user.nickname,
            avatarUrl = user.avatarUrl,
            lastLoginTime = System.currentTimeMillis()
        )
        _savedAccounts.removeIf { it.phone == user.phone }
        _savedAccounts.add(0, saved)
    }

    override suspend fun removeSavedAccount(phone: String) {
        _savedAccounts.removeIf { it.phone == phone }
    }

    override suspend fun getUserFollowerCount(userId: String): Int {
        return _follows.count { it.endsWith("_$userId") }
    }
    private fun wrapWithTitle(comments: List<Comment>): List<CommentWithTitle> {
        return comments.map { comment ->
            val post = _posts.find { it.id == comment.postId }
            CommentWithTitle(comment, post?.title ?: "未知帖子")
        }
    }

    override fun getMySentComments(): Flow<List<CommentWithTitle>> = flow {
        val userId = _currentUserFlow.value?.id ?: return@flow

        val allMyComments = _comments.filter { it.authorId == userId }

        val repliesToOthers = allMyComments.filter { myComment ->
            if (myComment.parentCommentId == null) {
                val post = _posts.find { it.id == myComment.postId }
                post != null && post.authorId != userId
            } else {
                val parent = _comments.find { it.id == myComment.parentCommentId }
                parent != null && parent.authorId != userId
            }
        }
        emit(wrapWithTitle(repliesToOthers))
    }


    override fun getRepliesToMe(): Flow<List<CommentWithTitle>> = flow {
        val userId = _currentUserFlow.value?.id ?: return@flow
        val myPostIds = _posts.filter { it.authorId == userId }.map { it.id }.toSet()
        val myCommentIds = _comments.filter { it.authorId == userId }.map { it.id }.toSet()
        val replies = _comments.filter { comment ->
            if (comment.authorId == userId) return@filter false
            val isReplyToMe = comment.parentCommentId != null && comment.parentCommentId in myCommentIds
            val isCommentOnMyPost = comment.postId in myPostIds
            isReplyToMe || isCommentOnMyPost
        }
        emit(wrapWithTitle(replies))
    }

    override fun getMyFollowers(): Flow<List<User>> = flow {
        val userId = _currentUserFlow.value?.id ?: return@flow
        val followerIds = _follows.filter { it.endsWith("_$userId") }.map { it.split("_")[0] }
        val users = _users.filter { it.id in followerIds }
        emit(users)
    }

    private val _themeFlow = MutableStateFlow(AppThemeMode.DEFAULT)
    override fun getAppTheme() = _themeFlow
    override suspend fun setAppTheme(mode: AppThemeMode) { _themeFlow.value = mode }

    override suspend fun getMessageCount(personaId: String): Int {
        TODO("Not yet implemented")
    }

    override suspend fun updatePersonaEvolution(personaId: String) {
        val userId = _currentUserFlow.value?.id ?: return
        val index = _personas.indexOfFirst { it.id == personaId }

        if (index != -1) {
            val oldPersona = _personas[index]

            if (oldPersona.creatorId != userId) return

            val timestamp = System.currentTimeMillis() % 10000
            val newMemory = "Mock记忆片段($timestamp)：用户与角色进行了深入交流，关系加深。"

            val oldEvolution = oldPersona.evolvedPersonality
            val newEvolution = if (oldEvolution.isBlank()) {
                "• $newMemory"
            } else {
                "$oldEvolution\n• $newMemory"
            }

            _personas[index] = oldPersona.copy(evolvedPersonality = newEvolution)

            android.util.Log.d("LocalLLM", "Mock: 共生进化更新 -> $newEvolution")

            generateAndSavePersonaEmbedding(personaId)
        }
    }
    override fun updatePersonaStatus(personaId: String) {
        val index = _personas.indexOfFirst { it.id == personaId }
        if (index != -1) {
            val old = _personas[index]
            val newEvolution = "Mock进化：与用户增进了解，变得更亲密了。"
            _personas[index] = old.copy(evolvedPersonality = newEvolution)
        }
    }

    override suspend fun triggerPersonaPost(personaId: String, Mode: Int): String {
        val persona = getPersonaById(personaId) ?: return ""

        val newPostId = java.util.UUID.randomUUID().toString()
        val newPost = Post(
            id = newPostId,
            authorId = persona.id,
            authorType = "persona",
            authorName = persona.name,
            authorAvatar = persona.avatarUrl,
            title = "Mock自动发帖",
            content = "我在思考 ${persona.systemPrompt.take(10)}...",
            // 简单模拟：如果是视频就给视频图，否则给图片
            imageUrls = listOf("https://api.dicebear.com/9.x/shapes/png?seed=${System.nanoTime()}"),
            tags = persona.tags,
            timestamp = System.currentTimeMillis()
        )

        _posts.add(0, newPost)
        return newPostId
    }

    override suspend fun generatePersonaAttributes(seedName: String, voiceOptions: String): Map<String, String> {
        // 简单返回一个固定的 Map，仅供测试 UI 流程
        return mapOf(
            "昵称" to seedName,
            "简介" to "这是 Mock 生成的简介",
            "性别" to "自定义",
            "年龄" to "18",
            "身份" to "旅行者",
            "性格" to "乐观",
            "外貌" to "金发碧眼",
            "第一句话" to "你好，我是 AI 生成的。"
        )
    }

    // 模拟一直在线
    override fun isDeviceOnline(): Flow<Boolean> = flow { emit(true) }

    // 模拟离线开关状态
    private val _mockOfflineMode = MutableStateFlow(false)
    override fun isForceOffline(): Flow<Boolean> = _mockOfflineMode

    override suspend fun setForceOffline(enable: Boolean) {
        _mockOfflineMode.value = enable
    }

    override suspend fun generateAvatarImage(prompt: String): String? {
        delay(1500) // 模拟网络延迟
        return "https://api.dicebear.com/9.x/bottts/png?seed=${prompt.hashCode()}"
    }

    override suspend fun getRecommendedPersonas(page: Int, pageSize: Int): List<Persona> {
        val userId = _currentUserFlow.value?.id ?: return emptyList()

        // 1. 模拟 messageDao.getRecentMessages (获取所有聊过天的人，按时间排序)
        val userMessages = messageList.filter { it.userId == userId }
        val recentChats = userMessages
            .groupBy { it.personaId } // 按角色分组
            .map { (_, msgs) -> msgs.maxBy { it.timestamp } } // 取每个角色最新的一条
            .sortedByDescending { it.timestamp } // 按时间倒序

        // 2. 准备兴趣基准 (最近 5 个)
        val interactedPersonas = recentChats.take(5)
            .mapNotNull { msg -> _personas.find { it.id == msg.personaId } }
            .filter { it.embedding.isNotEmpty() }

        // 3. 准备黑名单
        val chattedIds = recentChats.map { it.personaId }.toSet()
        val followedIds = _follows
            .filter { it.startsWith("${userId}_") }
            .map { it.split("_")[1] }
            .toSet()

        // 4. 获取候选池 & 过滤
        val candidates = _personas.filter { p ->
            p.embedding.isNotEmpty() &&
                    !chattedIds.contains(p.id) &&
                    !followedIds.contains(p.id)
        }

        // 5. 冷启动处理
        if (interactedPersonas.isEmpty()) {
            return candidates.shuffled().take(10)
        }

        // 6. 计算用户兴趣向量
        val userVector = VectorUtils.averageVector(interactedPersonas.map { it.embedding })

        // 7. 计算相似度并排序
        val sorted = candidates.map { persona ->
            val score = VectorUtils.cosineSimilarity(userVector, persona.embedding)
            Pair(persona, score)
        }
            .sortedByDescending { it.second }

        // 8. 返回 Top 10
        return sorted.map { it.first }.take(10)
    }

    override suspend fun generateAndSavePersonaEmbedding(personaId: String) {
        val index = _personas.indexOfFirst { it.id == personaId }

        if (index != -1) {
            // 1. 模拟网络延迟
            delay(1000)

            // 2. 生成一个随机向量
            val mockVector = List(1024) { java.util.Random().nextFloat() }

            // 3. 更新内存列表
            val updatedPersona = _personas[index].copy(embedding = mockVector)
            _personas[index] = updatedPersona

            android.util.Log.d("LocalLLM", "Mock: 向量生成成功 -> ${updatedPersona.name}")
        }
    }

    override fun forwardPostToPersonas(postId: String, targetPersonaIds: List<String>, comment: String) {
        // Mock 简单实现
        targetPersonaIds.forEach { pid ->
            // 模拟逻辑...
            android.util.Log.d("MockRepo", "转发帖子 $postId 给 $pid")
        }
    }
}