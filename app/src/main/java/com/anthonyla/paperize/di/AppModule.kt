package com.anthonyla.paperize.di

import android.app.Application
import android.app.WallpaperManager
import android.content.Context
import androidx.room.Room
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.data.database.LIBRARY_MIGRATIONS
import com.anthonyla.paperize.data.database.PaperizeDatabase
import com.anthonyla.paperize.data.repository.AlbumRepositoryImpl
import com.anthonyla.paperize.data.repository.SettingsRepositoryImpl
import com.anthonyla.paperize.data.repository.WallpaperRepositoryImpl
import com.anthonyla.paperize.data.source.AndroidDocumentSource
import com.anthonyla.paperize.data.source.AndroidWallpaperRenderer
import com.anthonyla.paperize.domain.repository.AlbumRepository
import com.anthonyla.paperize.domain.repository.SettingsRepository
import com.anthonyla.paperize.domain.repository.WallpaperRepository
import com.anthonyla.paperize.domain.source.DocumentSource
import com.anthonyla.paperize.domain.source.WallpaperRenderer
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
    fun providePaperizeDatabase(app: Application): PaperizeDatabase =
        Room.databaseBuilder(app, PaperizeDatabase::class.java, Constants.DATABASE_NAME)
            .addMigrations(*LIBRARY_MIGRATIONS)
            .build()

    @Provides
    fun provideWallpaperManager(@ApplicationContext context: Context): WallpaperManager =
        WallpaperManager.getInstance(context)

    @Provides
    fun provideDocumentSource(source: AndroidDocumentSource): DocumentSource = source

    @Provides
    fun provideWallpaperRenderer(renderer: AndroidWallpaperRenderer): WallpaperRenderer = renderer

    @Provides
    fun provideAlbumRepository(repository: AlbumRepositoryImpl): AlbumRepository = repository

    @Provides
    fun provideWallpaperRepository(repository: WallpaperRepositoryImpl): WallpaperRepository = repository

    @Provides
    fun provideSettingsRepository(repository: SettingsRepositoryImpl): SettingsRepository = repository
}
