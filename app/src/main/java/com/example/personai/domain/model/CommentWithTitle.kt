package com.example.personai.domain.model

import androidx.room.Embedded

data class CommentWithTitle(
    @Embedded val comment: Comment,
    val postTitle: String? // 可能为空（虽然逻辑上帖子肯定存在，但防止数据库不一致）
)