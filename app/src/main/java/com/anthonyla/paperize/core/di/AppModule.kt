package com.anthonyla.paperize.core.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.data.database.PaperizeDatabase
import com.anthonyla.paperize.data.datastore.PreferencesManager
import com.anthonyla.paperize.data.repository.AlbumRepositoryImpl
import com.anthonyla.paperize.data.repository.SettingsRepositoryImpl
import com.anthonyla.paperize.data.repository.WallpaperRepositoryImpl
import com.anthonyla.paperize.data.source.AndroidDocumentSource
import com.anthonyla.paperize.domain.repository.AlbumRepository
import com.anthonyla.paperize.domain.repository.SettingsRepository
import com.anthonyla.paperize.domain.repository.WallpaperRepository
import com.anthonyla.paperize.domain.source.DocumentSource
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
    fun provideWallpaperManager(@ApplicationContext context: Context): android.app.WallpaperManager =
        android.app.WallpaperManager.getInstance(context)

    @Provides
    fun provideDocumentSource(source: AndroidDocumentSource): DocumentSource = source

    @Provides
    @Singleton
    fun providePaperizeDatabase(app: Application): PaperizeDatabase {
        return Room.databaseBuilder(
            app,
            PaperizeDatabase::class.java,
            Constants.DATABASE_NAME
        )
            .addMigrations(*com.anthonyla.paperize.data.database.LIBRARY_MIGRATIONS)
            .build()
    }

    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager =
        PreferencesManager(context)

    @Provides
    fun provideAlbumRepository(repository: AlbumRepositoryImpl): AlbumRepository = repository

    @Provides
    fun provideWallpaperRepository(repository: WallpaperRepositoryImpl): WallpaperRepository = repository

    @Provides
    fun provideSettingsRepository(repository: SettingsRepositoryImpl): SettingsRepository = repository
}
