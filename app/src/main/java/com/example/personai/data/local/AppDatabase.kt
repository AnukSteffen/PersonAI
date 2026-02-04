package com.example.personai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.personai.domain.model.ChatMessage
import com.example.personai.domain.model.Comment
import com.example.personai.domain.model.Draft
import com.example.personai.domain.model.Follow
import com.example.personai.domain.model.Persona
import com.example.personai.domain.model.Post
import com.example.personai.domain.model.PostHidden
import com.example.personai.domain.model.PostHistory
import com.example.personai.domain.model.PostLike
import com.example.personai.domain.model.SavedAccount
import com.example.personai.domain.model.SyncLog
import com.example.personai.domain.model.User

@Database(
    entities = [Persona::class, ChatMessage::class,
        Post::class, Comment::class, User::class,
        PostHistory::class, PostLike::class, Follow::class, Draft::class,
        PostHidden::class, SavedAccount::class, SyncLog::class ],
    version = 13,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun personaDao(): PersonaDao
    abstract fun messageDao(): MessageDao
    abstract fun postDao(): PostDao
    abstract fun commentDao(): CommentDao

    abstract fun userDao(): UserDao

    abstract fun followDao(): FollowDao
    abstract fun draftDao(): DraftDao
    abstract fun syncDao(): SyncDao
}