package com.example.personai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.personai.domain.model.SavedAccount
import com.example.personai.domain.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    suspend fun getUserByPhone(phone: String): User?

    @Query("SELECT * FROM users WHERE id = :id")
    fun getUserByIdFlow(id: String): Flow<User?>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: String): User?

    @Insert
    suspend fun insertUser(user: User)

    @Query("UPDATE users SET nickname = :newNickname WHERE id = :userId")
    suspend fun updateNickname(userId: String, newNickname: String)

     @Query("UPDATE users SET avatarUrl = :url WHERE id = :id")
     suspend fun updateAvatar(id: String, url: String)

    @Query("SELECT * FROM saved_accounts ORDER BY lastLoginTime DESC")
    fun getSavedAccounts(): Flow<List<SavedAccount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedAccount(account: SavedAccount)

    @Query("DELETE FROM saved_accounts WHERE phone = :phone")
    suspend fun deleteSavedAccount(phone: String)

    @Query("UPDATE saved_accounts SET nickname = :nickname WHERE phone = :phone")
    suspend fun updateSavedNickname(phone: String, nickname: String)

    @Query("UPDATE saved_accounts SET avatarUrl = :url WHERE phone = :phone")
    suspend fun updateSavedAvatar(phone: String, url: String)

    @Query("SELECT phone FROM users WHERE id = :userId")
    suspend fun getPhoneByUserId(userId: String): String?

    @Query("SELECT COUNT(*) FROM follows WHERE personaId = :userId")
    suspend fun getUserFollowerCount(userId: String): Int

    @Query("SELECT * FROM users WHERE id IN (:ids)")
    suspend fun getUsersByIds(ids: List<String>): List<User>

}