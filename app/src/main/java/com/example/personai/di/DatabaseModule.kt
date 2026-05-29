package com.example.personai.di

import android.content.Context
import androidx.room.Room
import com.example.personai.data.local.AppDatabase
import com.example.personai.data.local.UserDao
import com.example.personai.data.repository.RoomPersonaRepository
import com.example.personai.domain.repository.PersonaRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "persona_db"
        )
            .fallbackToDestructiveMigration(false)
            .build()
    }

    @Provides
    fun providePersonaDao(db: AppDatabase) = db.personaDao()

    @Provides
    fun provideMessageDao(db: AppDatabase) = db.messageDao()

    @Provides
    fun providePostDao(db: AppDatabase) = db.postDao()

    @Provides
    fun provideCommentDao(db: AppDatabase) = db.commentDao()

    @Provides
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()

    @Provides
    fun provideFollowDao(db: AppDatabase) = db.followDao()

    @Provides
    fun provideDraftDao(db: AppDatabase) = db.draftDao()

    @Provides
    fun provideSyncDao(db: AppDatabase)= db.syncDao()

}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    // 使用本地Room
    @Binds
    @Singleton
    abstract fun bindPersonaRepository(
        roomRepository: RoomPersonaRepository
    ): PersonaRepository

    // 使用云端 API
//     @Binds
//     @Singleton
//     abstract fun bindCloudRepository(
//         impl: FakeNetworkRepository
//     ): PersonaRepository
}