package com.example.personai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.personai.domain.model.Follow
import kotlinx.coroutines.flow.Flow

@Dao
interface FollowDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun follow(follow: Follow)

    @Query("DELETE FROM follows WHERE userId = :userId AND personaId = :personaId")
    suspend fun unfollow(userId: String, personaId: String)

    // 检查是否已关注
    @Query("SELECT EXISTS(SELECT 1 FROM follows WHERE userId = :userId AND personaId = :personaId)")
    fun isFollowing(userId: String, personaId: String): Flow<Boolean>

    // 获取我关注的 Persona ID 列表
    @Query("SELECT personaId FROM follows WHERE userId = :userId")
    fun getFollowedPersonaIds(userId: String): Flow<List<String>>

    // 统计粉丝数
    @Query("SELECT COUNT(*) FROM follows WHERE personaId = :personaId")
    suspend fun getFollowerCount(personaId: String): Int

    // 查关注
    @Query("SELECT personaId FROM follows WHERE userId = :userId")
    fun getFollowingIds(userId: String): Flow<List<String>>

    // 查粉丝
    @Query("SELECT userId FROM follows WHERE personaId = :myUserId")
    fun getFollowerIds(myUserId: String): Flow<List<String>>
}