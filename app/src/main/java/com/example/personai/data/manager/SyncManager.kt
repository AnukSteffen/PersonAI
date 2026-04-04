package com.example.personai.data.manager

import android.util.Log
import com.example.personai.data.manager.NetworkMonitor
import com.example.personai.data.local.SyncDao
import com.example.personai.di.ApplicationScope
import com.example.personai.domain.model.SyncLog
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val syncDao: SyncDao,
//    private val api: ApiService,
    private val networkMonitor: NetworkMonitor,
    @ApplicationScope private val scope: CoroutineScope
) {

    private val syncMutex = kotlinx.coroutines.sync.Mutex()

    init {
        // 启动时监听网络
        observeNetwork()
    }

    private fun observeNetwork() {
        scope.launch {
            networkMonitor.isOnline.collect { isOnline ->
                if (isOnline) {
                    Log.d("SyncManager", "网络已连接，开始检查待同步任务...")
                    performSync()
                } else {
                    Log.d("SyncManager", "网络断开，任务积压中...")
                }
            }
        }
    }

    // 执行同步
    private suspend fun performSync() { //可能被网络监听器和 scheduleSync() 同时触发,这里执行前加锁
        if (syncMutex.isLocked) return
        syncMutex.withLock {
            val logs = syncDao.getAllLogs()
            if (logs.isEmpty()) return
            Log.d("SyncManager", "发现 ${logs.size} 个待同步任务")
            logs.forEach { log ->
                try {
                    delay(500)

                    // 在真实项目中，这里会调用 Retrofit
                    when (log.action) {
                        "UPDATE_NICKNAME" -> {
                            val data = Gson().fromJson(log.payload, Map::class.java)
                            Log.d("SyncManager", "[模拟上传] 更新昵称: ${data["nickname"]}")
                        }
                        "UPDATE_AVATAR" -> {
                            Log.d("SyncManager", "[模拟上传] 更新头像: (文件流处理)")
                        }
                        "FOLLOW" -> {
                            val data = Gson().fromJson(log.payload, Map::class.java)
                            Log.d("SyncManager", "[模拟上传] 关注操作: ${data["personaId"]}")
                        }
                        "UNFOLLOW" -> {
                            val data = Gson().fromJson(log.payload, Map::class.java)
                            Log.d("SyncManager", "[模拟上传] 取消关注: ${data["personaId"]}")
                        }
                    }

                    // 同步成功，删除日志
                    syncDao.delete(log)
                    Log.d("SyncManager", "任务 ${log.id} 同步完成")

                } catch (e: Exception) {
                    Log.e("SyncManager", "任务 ${log.id} 失败: ${e.message}")
                    // 真实场景下可能需要重试机制
                }
            }
        }
    }

    // 对外暴露的方法
    fun scheduleSync(action: String, payload: Map<String, Any>) {
        scope.launch {
            val json = Gson().toJson(payload)
            val log = SyncLog(action = action, payload = json)
            syncDao.insert(log) //记录直接插入数据库，这里可以用缓存+文件进行优化
            Log.d("SyncManager", "操作已记录到本地队列: $action")

            // 尝试立即同步
            performSync()
        }
    }
}