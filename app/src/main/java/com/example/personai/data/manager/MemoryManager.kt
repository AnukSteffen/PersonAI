package com.example.personai.data.manager

import android.app.ActivityManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // 本地模型大小约为 2GB（Gemma 2B INT4 量化）
    private val REQUIRED_MEMORY_FOR_MODEL = 2L * 1024 * 1024 * 1024 // 2GB

    /**
     * 获取可用 RAM 内存大小（字节）
     */
    fun getAvailableRamMemory(): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.availMem
    }

    /**
     * 获取总 RAM 内存大小（字节）
     */
    fun getTotalRamMemory(): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.totalMem
    }

    /**
     * 获取可用 RAM 内存百分比
     */
    fun getAvailableRamPercentage(): Double {
        val available = getAvailableRamMemory()
        val total = getTotalRamMemory()
        return if (total > 0) (available.toDouble() / total.toDouble()) * 100 else 0.0
    }

    /**
     * 检查是否有足够内存加载本地模型
     */
    fun hasEnoughMemoryForModel(): Boolean {
        return getAvailableRamMemory() >= REQUIRED_MEMORY_FOR_MODEL
    }

    /**
     * 获取可用内存的可读字符串
     */
    fun getAvailableRamReadable(): String {
        val bytes = getAvailableRamMemory()
        return formatSize(bytes)
    }

    /**
     * 获取总内存的可读字符串
     */
    fun getTotalRamReadable(): String {
        val bytes = getTotalRamMemory()
        return formatSize(bytes)
    }

    /**
     * 获取所需模型内存的可读字符串
     */
    fun getRequiredModelMemoryReadable(): String {
        return formatSize(REQUIRED_MEMORY_FOR_MODEL)
    }

    /**
     * 格式化字节数为可读字符串
     */
    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        if (bytes < 1024 * 1024) return "%.2f KB".format(bytes / 1024.0)
        if (bytes < 1024 * 1024 * 1024) return "%.2f MB".format(bytes / (1024.0 * 1024))
        return "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
    }

    /**
     * 获取内存状态摘要
     */
    fun getMemoryStatusSummary(): MemoryStatus {
        return MemoryStatus(
            availableRam = getAvailableRamMemory(),
            totalRam = getTotalRamMemory(),
            requiredForModel = REQUIRED_MEMORY_FOR_MODEL,
            hasEnoughMemory = hasEnoughMemoryForModel()
        )
    }

    data class MemoryStatus(
        val availableRam: Long,
        val totalRam: Long,
        val requiredForModel: Long,
        val hasEnoughMemory: Boolean
    )
}
