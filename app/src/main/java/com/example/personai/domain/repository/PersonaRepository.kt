package com.example.personai.domain.repository

import com.example.personai.domain.model.ChatMessage
import com.example.personai.domain.model.ChatSession
import com.example.personai.domain.model.Comment
import com.example.personai.domain.model.CommentWithTitle
import com.example.personai.domain.model.HistoryItem
import com.example.personai.domain.model.Persona
import com.example.personai.domain.model.Post
import com.example.personai.domain.model.SavedAccount
import com.example.personai.domain.model.User
import com.example.personai.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface PersonaRepository {
    // 用户相关
    suspend fun login(phone: String, password: String): User?

    suspend fun checkUserExists(phone: String): Boolean

    suspend fun register(user: User): User?

    suspend fun logout()

    fun getCurrentUser(): Flow<User?>

    suspend fun getUserById(userId: String): User?

    // 修改昵称
    suspend fun updateNickname(userId: String, newNickname: String)

    fun getBrowsingHistory(): Flow<List<HistoryItem>>

    suspend fun addBrowsingHistory(postId: String)

    fun getAllPersonas(): Flow<List<Persona>>
    fun searchPersonas(query: String): Flow<List<Persona>>

    // 仅获取"当前登录用户"创建的角色
    fun getUserPersonas(): Flow<List<Persona>>
    suspend fun getPersonaById(id: String): Persona?
    suspend fun addPersona(persona: Persona)

    suspend fun getMessageCount(personaId: String): Int
    suspend fun getMessages(personaId: String): List<ChatMessage>
    fun getMessagesFlow(personaId: String): Flow<List<ChatMessage>>
    suspend fun getRecentChats(): List<ChatSession>
    fun chatStream(personaId: String, userContent: String, lengthLevel: Int): Flow<String>
    // 发送非文本消息 (图片/语音/帖子分享)
    // type: 0=文本，1=图片, 2=视频, 3=PostShare, 4=语音
    suspend fun sendMediaMessage(personaId: String, content: String, type: Int)

    val generatingStatus: StateFlow<String?>

    fun getAllPosts(): Flow<List<Post>>
    fun searchPosts(query: String): Flow<List<Post>>
    fun searchFollowedPosts(query: String): Flow<List<Post>>
    suspend fun createPost(post: Post)
    suspend fun getPostById(postId: String): Post?
    fun getMyPosts(): Flow<List<Post>>
    fun getPostsByAuthor(authorId: String): Flow<List<Post>>
    fun getFeedPosts(type: String): Flow<List<Post>>
    suspend fun hidePost(postId: String)
    suspend fun unhidePost(postId: String)
    fun getHiddenPosts(): Flow<List<Post>>
    fun getCommentsForPost(postId: String): Flow<List<Comment>>
    suspend fun addComment(comment: Comment)

    suspend fun createUserPost(title: String, content: String, imageUrls: List<String>, tags: List<String>): String

    suspend fun createUserComment(postId: String, content: String, parentCommentId: String? = null)

    // 点赞/取消点赞
    suspend fun toggleLike(postId: String)

    fun getMyLikedPostIds(): Flow<List<String>>

    // --- 草稿相关 ---
    // type: 1=Persona，2=Post
    suspend fun saveDraft(type: Int, contentJson: String)
    suspend fun getDraft(type: Int): String?
    suspend fun clearDraft(type: Int)

    // 关注系统
    fun isFollowing(personaId: String): Flow<Boolean>
    suspend fun follow(personaId: String)
    suspend fun unfollow(personaId: String)
    fun getFollowedPersonas(): Flow<List<Persona>>
    fun getMyFollowers(): Flow<List<User>>

    // --- 账号管理 ---
    fun getSavedAccounts(): Flow<List<SavedAccount>>
    suspend fun saveAccount(user: User, password: String)
    suspend fun removeSavedAccount(phone: String)
    suspend fun updateUserAvatar(userId: String, avatarUrl: String)

    // --- 用户数据统计 ---
    suspend fun getUserFollowerCount(userId: String): Int
    fun getMySentComments(): Flow<List<CommentWithTitle>>
    fun getRepliesToMe(): Flow<List<CommentWithTitle>>
    fun getAppTheme(): Flow<AppThemeMode>
    suspend fun setAppTheme(mode: AppThemeMode)

    // AI生成
    suspend fun generatePersonaAttributes(seedName: String, voiceOptions: String): Map<String, String>
    fun updatePersonaStatus(personaId: String)
    suspend fun generateAvatarImage(prompt: String): String?
    suspend fun triggerPersonaPost(personaId: String, mode: Int): String
    suspend fun generateAndSavePersonaEmbedding(personaId: String)
    suspend fun getRecommendedPersonas(page: Int, pageSize: Int = 10): List<Persona>
    suspend fun updatePersonaEvolution(personaId: String)
    fun forwardPostToPersonas(postId: String, targetPersonaIds: List<String>, comment: String)

    // 网络状态
    fun isDeviceOnline(): Flow<Boolean>
    fun isForceOffline(): Flow<Boolean>
    suspend fun setForceOffline(enable: Boolean)
}