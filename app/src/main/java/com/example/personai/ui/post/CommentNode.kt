package com.example.personai.ui.post

import com.example.personai.domain.model.Comment

// 这是一个树节点，包含评论本体和它的子评论
data class CommentNode(
    val comment: Comment,
    val children: List<CommentNode> = emptyList(),
    val isExpanded: Boolean = true // 默认展开还是收起
)