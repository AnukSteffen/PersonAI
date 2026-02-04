package com.example.personai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.personai.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    // 获取某个角色的聊天记录
    @Query("SELECT * FROM messages WHERE personaId = :personaId ORDER BY timestamp ASC")
    suspend fun getMessages(personaId: String): List<ChatMessage>

    @Query("SELECT * FROM messages WHERE personaId = :personaId AND userId = :userId ORDER BY timestamp ASC")
    fun getMessagesFlow(personaId: String, userId: String): Flow<List<ChatMessage>>

    @Query("SELECT * FROM messages WHERE personaId = :personaId AND userId = :userId ORDER BY timestamp ASC")
    suspend fun getMessages(personaId: String, userId: String): List<ChatMessage>

    @Insert
    suspend fun insertMessage(message: ChatMessage)

    // 获取最近一条消息
    @Query("SELECT * FROM messages WHERE personaId = :personaId AND userId = :userId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastMessage(personaId: String, userId: String): ChatMessage?

    // 统计会话数
    @Query("SELECT COUNT(*) FROM messages WHERE personaId = :personaId AND isUser = 1")
    suspend fun getInteractionCount(personaId: String): Int

    @Query("SELECT COUNT(*) FROM messages WHERE personaId = :personaId AND userId = :userId")
    suspend fun getMessageCount(personaId: String, userId: String): Int

    // 获取所有会话的最后一条消息
    @Query("""
        SELECT * FROM messages 
        WHERE userId = :userId 
        GROUP BY personaId 
        HAVING timestamp = MAX(timestamp) 
        ORDER BY timestamp DESC
    """)
    suspend fun getRecentMessages(userId: String): List<ChatMessage>
}