package com.example.personai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.personai.domain.model.Draft

@Dao
interface DraftDao {
    // 获取某用户的某种类型的最新草稿
    @Query("SELECT * FROM drafts WHERE userId = :userId AND type = :type ORDER BY lastModified DESC LIMIT 1")
    suspend fun getLatestDraft(userId: String, type: Int): Draft?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: Draft)

    @Query("DELETE FROM drafts WHERE userId = :userId AND type = :type")
    suspend fun clearDraft(userId: String, type: Int)
}