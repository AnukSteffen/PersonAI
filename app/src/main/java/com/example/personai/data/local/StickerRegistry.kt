package com.example.personai.data.local

import com.example.personai.R

data class Sticker(
    val key: String,   // 语义标签
    val resId: Int     // 资源 ID
)

object StickerRegistry {
    val stickers = listOf(
        Sticker("开心", R.drawable.sticker_happy),
        Sticker("生气", R.drawable.sticker_angry),
        Sticker("哭泣", R.drawable.sticker_cry),
        Sticker("疑惑", R.drawable.sticker_confused),

    )

    // 根据 key 获取资源 ID
    fun getResId(key: String): Int? = stickers.find { it.key == key }?.resId
}