package com.example.personai.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.example.personai.data.manager.MemoryManager
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.genai.llminference.GraphOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalLLMEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val memoryManager: MemoryManager
) {
    private var llmInference: LlmInference? = null

    private val MODEL_PATH = "/data/local/tmp/gemma-1.1-2b-it-cpu-int4.bin"
    //private val MODEL_PATH = "/data/local/tmp/gemma2-2b-it-cpu-int8.bin"
    fun initialize(): Boolean {
        if (llmInference != null) return true

        // 1. 检查内存是否足够
        if (!memoryManager.hasEnoughMemoryForModel()) {
            val available = memoryManager.getAvailableRamReadable()
            val required = memoryManager.getRequiredModelMemoryReadable()
            Log.e("LocalLLM", "内存不足！可用: $available, 需要: $required")
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "内存不足！可用: $available, 需要: $required", Toast.LENGTH_LONG).show()
            }
            return false
        }

        // 2. 检查模型文件是否存在
        val modelFile = File(MODEL_PATH)
        if (!modelFile.exists()) {
            Log.e("LocalLLM", "模型文件未找到: $MODEL_PATH")
            return false
        }

        // 3. 记录内存状态
        Log.e("LocalLLM", "内存检测通过: ${memoryManager.getAvailableRamReadable()} 可用")

        try {
            Log.e("LocalLLM", "开始初始化...")

            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(MODEL_PATH)
                .setMaxTokens(1024)
                .setMaxNumImages(1)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            Log.e("LocalLLM", "模型加载成功！")
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "本地模型加载成功，AI 已就绪", Toast.LENGTH_SHORT).show()
            }
            return true

        } catch (e: Exception) {
            Log.e("LocalLLM", "加载失败: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    fun generateResponse(prompt: String, imagePath: String? = null): Flow<String> = callbackFlow {
        // 1. 确保引擎已就绪
        if (llmInference == null) {
            val success = initialize()
            if (!success) {
                trySend("【系统错误】模型文件未找到或加载失败。")
                close()
                return@callbackFlow
            }
        }

        // 2. 创建本次对话的 Session
        var session: LlmInferenceSession? = null

        try {
            // 3. 判断是否需要开启视觉模态
//            val hasImage = !imagePath.isNullOrEmpty() && File(imagePath).exists()

            val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                .setTemperature(0.7f)
                .setTopK(40)
                .setTopP(0.95f)
//                .setGraphOptions(
//                    GraphOptions.builder()
//                        .setEnableVisionModality(true)
//                        .build()
//                )
                .build()

            // 4. 创建 Session
            session = LlmInferenceSession.createFromOptions(llmInference!!, sessionOptions)
//            Log.d("LocalLLM", "Session 创建成功, 视觉模态: $hasImage")

            // 5. 添加图片
//            if (hasImage) {
//                try {
//                    val bitmap: Bitmap = BitmapFactory.decodeFile(imagePath)
//                    if (bitmap != null) {
//                        val mpImage: MPImage = BitmapImageBuilder(bitmap).build()
//                        session.addImage(mpImage)
//                        Log.d("LocalLLM", "图片已添加: $imagePath")
//                    } else {
//                        Log.e("LocalLLM", "图片解码失败: $imagePath")
//                    }
//                } catch (e: Exception) {
//                    Log.e("LocalLLM", "图片处理异常: ${e.message}")
//                }
//            }
            // 6. 文本提示
            session.addQueryChunk(prompt)

            // 7. 流式生成响应
            var isFinished = false
            session.generateResponseAsync { partialResult, done ->
                if (isFinished) return@generateResponseAsync
                // A. 发送内容
                if (!partialResult.isNullOrEmpty()) {
                    trySend(partialResult)
                }

                // B. 结束判断
                if (done) {
                    Log.d("LocalLLM", "生成完毕")
                    close()
                }
            }

        } catch (e: Exception) {
            Log.e("LocalLLM", "推理过程异常: ${e.message}")
            trySend("（推理中断: ${e.message}）")
            close()
        }

        // 5. 清理工作
        awaitClose {
            try {
                session?.close()
            } catch (e: Exception) {
                Log.e("LocalLLM", "Session关闭异常: ${e.message}")
            }
            Log.d("LocalLLM", "Session 已关闭")
        }
    }

    fun release() {
        try {
            llmInference?.close()
        } catch (e: Exception) {
            Log.e("LocalLLM", "释放资源异常: ${e.message}")
        }
        llmInference = null
    }
}