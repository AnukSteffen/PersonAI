package com.example.personai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.personai.domain.model.Comment
import com.example.personai.domain.model.CommentWithTitle
import kotlinx.coroutines.flow.Flow

@Dao
interface CommentDao {
    // 获取某个帖子的所有评论
    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY timestamp ASC")
    fun getCommentsForPost(postId: String): Flow<List<Comment>>

    @Insert
    suspend fun insertComment(comment: Comment)

    // 1. 获取我发出的评论(只看回复别人的，排除自言自语)
    @Query("""
        SELECT c.*, p.title AS postTitle
        FROM comments c
        LEFT JOIN posts p ON c.postId = p.id
        LEFT JOIN comments parent ON c.parentCommentId = parent.id
        WHERE c.authorId = :myUserId 
        AND (
            (c.parentCommentId IS NULL AND p.authorId != :myUserId)
            OR 
            (c.parentCommentId IS NOT NULL AND parent.authorId != :myUserId)
        )
        ORDER BY c.timestamp DESC
    """)
    fun getCommentsByAuthor(myUserId: String): Flow<List<CommentWithTitle>>

    // 2. 获取回复我的评论
    @Query("""
        SELECT DISTINCT c.*, p.title AS postTitle
        FROM comments c
        LEFT JOIN comments parent ON c.parentCommentId = parent.id
        LEFT JOIN posts p ON c.postId = p.id
        WHERE c.authorId != :myUserId 
        AND (
            parent.authorId = :myUserId 
            OR 
            p.authorId = :myUserId
        )
        ORDER BY c.timestamp DESC
    """)
    fun getRepliesToMe(myUserId: String): Flow<List<CommentWithTitle>>
}