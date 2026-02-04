package com.example.personai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.personai.domain.model.HistoryItem
import com.example.personai.domain.model.Post
import com.example.personai.domain.model.PostHidden
import com.example.personai.domain.model.PostHistory
import com.example.personai.domain.model.PostLike
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    // 获取所有帖子 (广场流)
    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    fun getAllPosts(): Flow<List<Post>>

    // 获取某个 Persona 发的所有帖子 (个人主页用)
    @Query("SELECT * FROM posts WHERE authorId = :personaId ORDER BY timestamp DESC")
    suspend fun getPostsByPersona(personaId: String): List<Post>

    // 获取单条帖子详情
    @Query("SELECT * FROM posts WHERE id = :postId")
    suspend fun getPostById(postId: String): Post?

    // 发布帖子
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: Post)

    @Query("""
        SELECT p.*, MAX(h.timestamp) as browsedTime 
        FROM posts p 
        INNER JOIN post_history h ON p.id = h.postId 
        WHERE h.userId = :userId 
        GROUP BY p.id 
        ORDER BY browsedTime DESC
    """)
    fun getBrowsingHistory(userId: String): Flow<List<HistoryItem>>

    @Insert
    suspend fun insertHistory(history: PostHistory)

    // 点赞事务：1. 插入点赞记录 2. 帖子点赞数+1
    @Transaction
    suspend fun likePost(postId: String, userId: String) {
        if (!hasLiked(postId, userId)) {
            insertPostLike(PostLike(postId, userId))
            incrementLikeCount(postId)
        }
    }

    suspend fun unlikePost(postId: String, userId: String) {
        if (hasLiked(postId, userId)) {
            deletePostLike(postId, userId)
            decrementLikeCount(postId)
        }
    }

    @Query("SELECT EXISTS(SELECT 1 FROM post_likes WHERE postId = :postId AND userId = :userId)")
    suspend fun hasLiked(postId: String, userId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPostLike(like: PostLike)

    @Query("DELETE FROM post_likes WHERE postId = :postId AND userId = :userId")
    suspend fun deletePostLike(postId: String, userId: String)

    @Query("UPDATE posts SET likeCount = likeCount + 1 WHERE id = :postId")
    suspend fun incrementLikeCount(postId: String)

    @Query("UPDATE posts SET likeCount = likeCount - 1 WHERE id = :postId")
    suspend fun decrementLikeCount(postId: String)

    // 获取某用户赞过的所有帖子ID (用于UI判断是否显示红心)
    @Query("SELECT postId FROM post_likes WHERE userId = :userId")
    fun getLikedPostIds(userId: String): Flow<List<String>>

    @Query("UPDATE posts SET commentCount = commentCount + 1 WHERE id = :postId")
    suspend fun incrementCommentCount(postId: String)

    // 广场搜索逻辑：同时匹配 标题、内容、作者名
    @Query("""
        SELECT * FROM posts 
        WHERE title LIKE '%' || :query || '%' 
           OR content LIKE '%' || :query || '%' 
           OR authorName LIKE '%' || :query || '%' 
        ORDER BY timestamp DESC
    """)
    fun searchPosts(query: String): Flow<List<Post>>

    // 只在关注的人里搜索
    @Query("""
        SELECT p.* FROM posts p
        INNER JOIN follows f ON p.authorId = f.personaId
        WHERE f.userId = :userId 
        AND (p.title LIKE '%' || :query || '%' OR p.content LIKE '%' || :query || '%' OR p.authorName LIKE '%' || :query || '%')
        ORDER BY p.timestamp DESC
    """)
    fun searchFollowedPosts(userId: String, query: String): Flow<List<Post>>

    // tag搜索
    @Query("SELECT * FROM posts WHERE tags LIKE '%' || :tag || '%' ORDER BY timestamp DESC")
    fun searchPostsByTag(tag: String): Flow<List<Post>>

    // 获取推荐流帖子，排除我不喜欢的
    @Query("""
        SELECT * FROM posts 
        WHERE id NOT IN (SELECT postId FROM post_hidden WHERE userId = :userId)
        ORDER BY timestamp DESC
    """)
    fun getRecommendedPosts(userId: String): Flow<List<Post>>

    // 只获取我关注的 Persona (或用户) 发的帖子
    @Query("""
        SELECT p.* FROM posts p
        INNER JOIN follows f ON p.authorId = f.personaId
        WHERE f.userId = :userId
        AND p.id NOT IN (SELECT postId FROM post_hidden WHERE userId = :userId)
        ORDER BY p.timestamp DESC
    """)
    fun getFollowedPosts(userId: String): Flow<List<Post>>

    // 插入“不喜欢”记录
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHiddenPost(hidden: PostHidden)

    // 取消屏蔽
    @Query("DELETE FROM post_hidden WHERE userId = :userId AND postId = :postId")
    suspend fun deleteHiddenPost(userId: String, postId: String)

    // 获取我屏蔽的所有帖子 (用于在"我的-设置"里管理)
    @Query("SELECT * FROM posts WHERE id IN (SELECT postId FROM post_hidden WHERE userId = :userId)")
    fun getHiddenPosts(userId: String): Flow<List<Post>>

     @Query("SELECT * FROM posts WHERE authorId = :personaId ORDER BY timestamp DESC")
     fun getPostsByPersonaFlow(personaId: String): Flow<List<Post>>
}