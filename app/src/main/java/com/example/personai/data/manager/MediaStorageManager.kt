package com.example.personai.data.manager

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun saveImageToInternalStorage(uri: Uri): String = withContext(Dispatchers.IO) {
        // 1. 创建文件名
        val fileName = "img_${UUID.randomUUID()}.jpg"

        // 2. 打开输入流
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("无法打开图片流")

        // 3. 创建输出文件
        val file = File(context.filesDir, fileName)

        // 4. 复制数据
        inputStream.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        // 5. 返回文件的绝对路径
        file.absolutePath
    }

    suspend fun saveMediaToInternalStorage(uri: Uri): String = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver

        // 1. 尝试获取 MimeType
        val mimeType = contentResolver.getType(uri)

        // 2. 根据 MimeType 获取后缀，如果获取失败，尝试从 URI 推断，最后默认 jpg
        var extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)

        if (extension == null) {
            if (mimeType?.startsWith("video") == true) {
                extension = "mp4"
            } else if (mimeType == "image/gif") {
                extension = "gif"
            } else {
                extension = "jpg"
            }
        }

        // 3. 创建文件名
        val fileName = "media_${UUID.randomUUID()}.$extension"

        // 4. 复制流
        val inputStream = contentResolver.openInputStream(uri)
            ?: throw Exception("无法打开媒体流")

        val file = File(context.filesDir, fileName)

        inputStream.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        file.absolutePath
    }

    // 判断是否为视频
    fun isVideo(path: String): Boolean {
        val lower = path.lowercase()
        return lower.endsWith(".mp4") || lower.endsWith(".mov") ||
                lower.endsWith(".mkv") || lower.endsWith(".webm")
    }
}