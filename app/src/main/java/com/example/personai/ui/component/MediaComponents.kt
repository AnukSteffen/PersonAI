package com.example.personai.ui.component

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.URL

/**
 * 全屏图片查看器
 * @param mediaUrls 图片链接列表
 * @param initialPage 初始显示哪一张
 * @param onDismiss 关闭回调
 */
@Composable
fun FullScreenMediaDialog(
    mediaUrls: List<String>,
    initialPage: Int = 0,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // 保存图片的 Loading 状态
    var isSaving by remember { mutableStateOf(false) }

    // 设置全屏 Dialog 属性
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val pagerState = rememberPagerState(
                initialPage = initialPage,
                pageCount = { mediaUrls.size }
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val path = mediaUrls[page]
                val isVideo = path.endsWith(".mp4", true) || path.endsWith(".mov", true)
                // 点击图片也可以关闭
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (isVideo) {
                        VideoPlayer(mediaPath = path, modifier = Modifier.fillMaxSize())
                    } else {
                        AsyncImage(
                            model = path,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }

                IconButton(
                    onClick = {
                        if (!isSaving) {
                            isSaving = true
                            scope.launch {
                                // 获取当前正在看的这张图片
                                val currentUrl = mediaUrls[pagerState.currentPage]
                                saveMediaToGallery(context, currentUrl)
                                isSaving = false
                            }
                        }
                    },
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Download, contentDescription = "Save", tint = Color.White)
                    }
                }
            }

            // 3. 底部指示器
            if (mediaUrls.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 40.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(mediaUrls.size) { iteration ->
                        val color = if (pagerState.currentPage == iteration) Color.White else Color.Gray
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(color)
                                .size(8.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 保存媒体到系统相册
 * 无需 WRITE_EXTERNAL_STORAGE 权限
 */
private suspend fun saveMediaToGallery(context: Context, path: String) {
    withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val lowerPath = path.lowercase()
            // 1. 判断文件类型
            val isVideo = lowerPath.endsWith(".mp4") || lowerPath.endsWith(".mov") || lowerPath.endsWith(".mkv") || lowerPath.endsWith(".webm")
            val isGif = lowerPath.endsWith(".gif")
            val extension = when {
                isVideo -> "mp4"
                isGif -> "gif"
                else -> "jpg"
            }
            // 2. 确定 MimeType 和保存目录
            val mimeType = when {
                isVideo -> "video/mp4"
                isGif -> "image/gif"
                else -> "image/jpeg"
            }

            val directory = if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
            val filename = "PersonaAI_${System.currentTimeMillis()}.$extension"

            // 3. 准备 MediaStore 信息
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "$directory/PersonaAI")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            // 4. 获取插入位置的 URI
            val collection = if (isVideo) {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val insertUri = resolver.insert(collection, contentValues)

            // 5. 流对流复制
            insertUri?.let { destinationUri ->
                // 打开输出流
                resolver.openOutputStream(destinationUri)?.use { outputStream ->
                    val inputStream: InputStream = if (path.startsWith("http")) {
                        URL(path).openStream() // 网络文件
                    } else {
                        FileInputStream(File(path)) // 本地文件
                    }

                    // 执行复制
                    inputStream.use { input ->
                        input.copyTo(outputStream)
                    }
                }

                // 6. 解除 Pending 状态
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(destinationUri, contentValues, null, null)
                }

                // 切回主线程显示 Toast
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "已保存到相册", Toast.LENGTH_SHORT).show()
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "保存失败: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}