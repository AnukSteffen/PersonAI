package com.example.personai.domain.model

import androidx.room.Embedded

data class HistoryItem(
    @Embedded val post: Post,
    val browsedTime: Long // 专门用来存浏览时间
)