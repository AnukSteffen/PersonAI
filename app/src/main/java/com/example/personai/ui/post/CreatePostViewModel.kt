package com.example.personai.ui.post

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personai.data.manager.MediaStorageManager
import com.example.personai.domain.repository.PersonaRepository
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreatePostViewModel @Inject constructor(
    private val repository: PersonaRepository,
    private val mediaManager: MediaStorageManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // 表单状态
    var title by mutableStateOf("")
    var content by mutableStateOf("")

    // 已选图片列表
    var selectedImages = mutableStateListOf<String>()

    var currentTagInput by mutableStateOf("") // 输入框里的内容
    var selectedTags = mutableStateListOf<String>() // 已添加的 Tag

    // 定义一个简单的内部数据类用于序列化
    private data class PostDraftData(
        val title: String,
        val content: String,
        val images: List<String>,
        val tags: List<String>
    )

    init {
        loadDraft()
    }

    private fun loadDraft() {
        viewModelScope.launch {
            val json = repository.getDraft(type = 2) // 2 = Post
            if (json != null) {
                try {
                    val data = Gson().fromJson(json, PostDraftData::class.java)
                    title = data.title
                    content = data.content
                    selectedImages.clear()
                    selectedImages.addAll(data.images)
                    selectedTags.clear()
                    selectedTags.addAll(data.tags)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun saveDraft(onSaved: () -> Unit) {
        viewModelScope.launch {
            val data = PostDraftData(title, content, selectedImages.toList(), selectedTags.toList())
            val json = Gson().toJson(data)
            repository.saveDraft(type = 2, contentJson = json)
            onSaved()
        }
    }

    // 判断是否有未保存的内容
    fun hasContent(): Boolean {
        return title.isNotBlank() || content.isNotBlank() || selectedImages.isNotEmpty()
    }

    // 上传多媒体
    fun onMediaSelected(uri: Uri) {
        viewModelScope.launch {
            try {
                // 1. 检查 MIME 类型
                val mimeType = context.contentResolver.getType(uri) ?: ""

                if (!mimeType.startsWith("image/") && !mimeType.startsWith("video/")) {
                    return@launch
                }

                // 2. 保存文件
                val savedPath = mediaManager.saveMediaToInternalStorage(uri)
                selectedImages.add(savedPath)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearData() {
        title = ""
        content = ""
        selectedImages.clear()
        selectedTags.clear()
        currentTagInput = ""
    }

    fun discardDraft() {
        viewModelScope.launch {
            // 1. 从数据库物理删除草稿
            repository.clearDraft(type = 2)

            // 2. 清空内存状态
            clearData()
        }
    }

    fun removeImage(url: String) {
        selectedImages.remove(url)
    }

    fun addTag() {
        if (currentTagInput.isNotBlank()) {
            // 简单的去重处理
            val tag = currentTagInput.trim()
            if (!selectedTags.contains(tag)) {
                selectedTags.add(tag)
            }
            currentTagInput = "" // 清空输入框
        }
    }

    fun removeTag(tag: String) {
        selectedTags.remove(tag)
    }

    // 发布逻辑
    fun submitPost(onSuccess: (String) -> Unit) {
        if (title.isBlank() || content.isBlank()) return

        viewModelScope.launch {
            val newId = repository.createUserPost(
                title = title,
                content = content,
                imageUrls = selectedImages.toList(),
                tags = selectedTags.toList()
            )
            repository.clearDraft(type = 2)

            if (newId.isNotBlank()) onSuccess(newId)
        }
    }
}