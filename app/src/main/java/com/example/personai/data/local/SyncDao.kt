package com.example.personai.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.personai.domain.model.SyncLog

@Dao
interface SyncDao {
    // 插入一条同步记录
    @Insert
    suspend fun insert(log: SyncLog)

    // 获取所有未同步的记录
    @Query("SELECT * FROM sync_logs ORDER BY timestamp ASC")
    suspend fun getAllLogs(): List<SyncLog>

    // 删除一条记录
    @Delete
    suspend fun delete(log: SyncLog)

    // 清空所有
    @Query("DELETE FROM sync_logs")
    suspend fun clearAll()
}