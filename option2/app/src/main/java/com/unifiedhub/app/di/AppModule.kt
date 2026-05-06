package com.unifiedhub.app.di

import android.content.ContentResolver
import android.content.Context
import androidx.room.Room
import com.unifiedhub.app.data.local.dao.TimelineDao
import com.unifiedhub.app.data.local.database.UnifiedHubDatabase
import com.unifiedhub.app.data.remote.gmail.GmailDataSource
import com.unifiedhub.app.data.remote.imap.ImapDataSource
import com.unifiedhub.app.data.repository.GmailDataSourceImpl
import com.unifiedhub.app.data.repository.ImapDataSourceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver =
        context.contentResolver

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): UnifiedHubDatabase =
        Room.databaseBuilder(
            context,
            UnifiedHubDatabase::class.java,
            "unified_hub.db"
        ).build()

    @Provides
    @Singleton
    fun provideTimelineDao(database: UnifiedHubDatabase): TimelineDao =
        database.timelineDao()

    @Provides
    @Singleton
    fun provideGmailDataSource(): GmailDataSource = GmailDataSourceImpl()

    @Provides
    @Singleton
    fun provideImapDataSource(): ImapDataSource = ImapDataSourceImpl()
}
