package com.anthonyla.paperize.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.anthonyla.paperize.data.settings.SettingsDataStore
import com.anthonyla.paperize.data.settings.SettingsDataStoreImpl
import com.anthonyla.paperize.feature.wallpaper.data.data_source.WallpaperDatabase
import com.anthonyla.paperize.feature.wallpaper.data.repository.WallpaperRepositoryImpl
import com.anthonyla.paperize.feature.wallpaper.domain.repository.WallpaperRepository
import com.anthonyla.paperize.feature.wallpaper.presentation.MainActivity
import com.anthonyla.paperize.feature.wallpaper.use_case.AddWallpaper
import com.anthonyla.paperize.feature.wallpaper.use_case.WallpaperUseCases
import com.anthonyla.paperize.feature.wallpaper.use_case.DeleteWallpaper
import com.anthonyla.paperize.feature.wallpaper.use_case.GetWallpapers
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
    fun provideWallpaperDatabase(app: Application): WallpaperDatabase {
        return Room.databaseBuilder(
            app,
            WallpaperDatabase::class.java,
            WallpaperDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    @Singleton
    fun provideWallpaperRepository(
        db: WallpaperDatabase
    ): WallpaperRepository {
        return WallpaperRepositoryImpl(db.wallpaperDao)
    }

    @Provides
    @Singleton
    fun provideWallpaperUseCases(
        repository: WallpaperRepository
    ): WallpaperUseCases {
        return WallpaperUseCases (
            getWallpapers = GetWallpapers(repository),
            deleteWallpaper = DeleteWallpaper(repository),
            addWallpaper = AddWallpaper(repository),
        )
    }

    @Provides
    @Singleton
    fun provideSettingsDataStore (
        @ApplicationContext context: Context
    ): SettingsDataStore = SettingsDataStoreImpl(context)

    @Provides
    fun provideContext(
        @ApplicationContext context: Context,
    ): Context {
        return context
    }
}