package com.example.personai.ui.post

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personai.domain.model.Comment
import com.example.personai.domain.model.Post
import com.example.personai.domain.repository.PersonaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val repository: PersonaRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // 从路由参数获取 postId
    private val postId: String = checkNotNull(savedStateHandle["postId"])

    // 帖子详情状态
    private val _post = MutableStateFlow<Post?>(null)
    val post = _post.asStateFlow()

    // 保留原始的扁平列表用于计算总楼层数
    private val rawComments = repository.getCommentsForPost(postId)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 对外暴露的总评论数
    val totalCommentCount = rawComments.map { it.size }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    // 构建扁平化的树
    val commentTree = rawComments
        .map { flatList -> buildFlattenedCommentTree(flatList) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 所有后代归于根节点
    private fun buildFlattenedCommentTree(flatList: List<Comment>): List<CommentNode> {
        val commentMap = flatList.associateBy { it.id }

        val roots = flatList.filter {
            it.parentCommentId == null || !commentMap.containsKey(it.parentCommentId)
        }.sortedBy { it.timestamp } // 根节点按时间排序

        val childrenMap = mutableMapOf<String, MutableList<Comment>>()

        flatList.forEach { comment ->
            if (comment.parentCommentId != null && commentMap.containsKey(comment.parentCommentId)) {
                val rootId = findRootId(comment, commentMap)
                if (rootId != null) {
                    if (!childrenMap.containsKey(rootId)) {
                        childrenMap[rootId] = mutableListOf()
                    }
                    childrenMap[rootId]?.add(comment)
                }
            }
        }

        // 组装
        return roots.map { root ->
            val children = childrenMap[root.id]
                ?.sortedBy { it.timestamp } // 子节点按时间排序
                ?: emptyList()

            CommentNode(root, children.map { CommentNode(it) })
        }
    }

    private fun findRootId(current: Comment, allComments: Map<String, Comment>): String? {
        if (current.parentCommentId == null || !allComments.containsKey(current.parentCommentId)) {
            return current.id // 自己就是根（或者是孤儿）
        }
        // 递归向上找
        val parent = allComments[current.parentCommentId]!!
        // 如果父节点就是根 (parentId == null)，那 parent.id 就是我们要找的
        if (parent.parentCommentId == null || !allComments.containsKey(parent.parentCommentId)) {
            return parent.id
        }
        return findRootId(parent, allComments)
    }

    private val _isLiked = MutableStateFlow(false)
    val isLiked = _isLiked.asStateFlow()

    init {
        loadPost()
        checkLikeStatus()
        // 记录浏览历史
        viewModelScope.launch {
            repository.addBrowsingHistory(postId)
        }
    }

    private fun loadPost() {
        viewModelScope.launch {
            _post.value = repository.getPostById(postId)
        }
    }

    private fun checkLikeStatus() {
        viewModelScope.launch {
            // 获取我点赞过的所有 ID，判断当前是否在其中
            val likedIds = repository.getMyLikedPostIds().first()
            _isLiked.value = likedIds.contains(postId)
        }
    }

    fun toggleLike() {
        viewModelScope.launch {
            // 1. 更新数据库
            repository.toggleLike(postId)
            // 2. 更新本地 UI 状态 (反转)
            _isLiked.value = !_isLiked.value
            // 3. 重新拉取帖子信息 (因为点赞数变了)
            loadPost()
        }
    }

    private val _replyingTo = MutableStateFlow<Comment?>(null)
    val replyingTo = _replyingTo.asStateFlow()
    fun onReplyClick(comment: Comment) {
        _replyingTo.value = comment
    }

    fun onCancelReply() {
        _replyingTo.value = null
    }

    fun sendComment(content: String) {
        if (content.isBlank()) return

        viewModelScope.launch {
            val parentId = _replyingTo.value?.id

            val finalContent = if (parentId != null) {
                "回复 @${_replyingTo.value!!.authorName} : $content"
            } else {
                content
            }

            // 3. 调用升级后的接口
            repository.createUserComment(
                postId = postId,
                content = finalContent,
                parentCommentId = parentId
            )

            _replyingTo.value = null
        }
    }

    // 刷新状态
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            kotlinx.coroutines.delay(500)
            _isRefreshing.value = false
        }
    }
}