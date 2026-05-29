package com.example.personai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.personai.domain.model.Persona
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonaDao {
    @Query("SELECT * FROM personas ORDER BY createdAt DESC")
    fun getAllPersonas(): Flow<List<Persona>>

    @Query("SELECT * FROM personas WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchPersonas(query: String): Flow<List<Persona>>

    @Query("SELECT * FROM personas WHERE tags LIKE '%' || :tag || '%'")
    fun searchPersonasByTag(tag: String): Flow<List<Persona>>

    @Query("SELECT * FROM personas WHERE creatorId = :creatorId ORDER BY createdAt DESC")
    fun getPersonasByCreator(creatorId: String): Flow<List<Persona>>

    @Query("SELECT COUNT(*) FROM follows WHERE userId = :userId")
    suspend fun getFollowingCount(userId: String): Int

    @Query("SELECT * FROM personas WHERE id IN (:ids)")
    suspend fun getPersonasByIds(ids: List<String>): List<Persona>

    @Query("SELECT * FROM personas WHERE id = :id")
    suspend fun getPersonaById(id: String): Persona?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersona(persona: Persona)

    @Query("UPDATE personas SET followerCount = followerCount + 1 WHERE id = :id")
    suspend fun incrementFollowerCount(id: String)

    @Query("UPDATE personas SET followerCount = followerCount - 1 WHERE id = :id")
    suspend fun decrementFollowerCount(id: String)

    @Query("UPDATE personas SET interactionCount = interactionCount + 1 WHERE id = :id")
    suspend fun incrementInteractionCount(id: String)

    @Query("UPDATE personas SET followingCount = followingCount + 1 WHERE id = :id")
    suspend fun incrementFollowingCount(id: String)

    @Query("UPDATE personas SET followingCount = followingCount - 1 WHERE id = :id")
    suspend fun decrementFollowingCount(id: String)

    @Query("UPDATE personas SET currentStatus = :status WHERE id = :id")
    suspend fun updateCurrentStatus(id: String, status: String)

    @Query("UPDATE personas SET evolvedPersonality = :evolution WHERE id = :id AND creatorId = :creatorId")
    suspend fun updateEvolvedPersonality(id: String, creatorId: String, evolution: String)
}