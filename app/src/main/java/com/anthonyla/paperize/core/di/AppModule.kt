package com.anthonyla.paperize.core.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.anthonyla.paperize.core.constants.Constants
import com.anthonyla.paperize.data.database.PaperizeDatabase
import com.anthonyla.paperize.data.database.dao.AlbumDao
import com.anthonyla.paperize.data.database.dao.FolderDao
import com.anthonyla.paperize.data.database.dao.WallpaperDao
import com.anthonyla.paperize.data.database.dao.WallpaperQueueDao
import com.anthonyla.paperize.data.datastore.PreferencesManager
import com.anthonyla.paperize.data.repository.AlbumRepositoryImpl
import com.anthonyla.paperize.data.repository.SettingsRepositoryImpl
import com.anthonyla.paperize.data.repository.WallpaperRepositoryImpl
import com.anthonyla.paperize.domain.repository.AlbumRepository
import com.anthonyla.paperize.domain.repository.SettingsRepository
import com.anthonyla.paperize.domain.repository.WallpaperRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt dependency injection module
 *
 * Provides all application-wide dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provide Room database
     */
    @Provides
    @Singleton
    fun providePaperizeDatabase(app: Application): PaperizeDatabase {
        return Room.databaseBuilder(
            app,
            PaperizeDatabase::class.java,
            Constants.DATABASE_NAME
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    /**
     * Provide DAOs
     */
    @Provides
    @Singleton
    fun provideAlbumDao(database: PaperizeDatabase): AlbumDao =
        database.albumDao()

    @Provides
    @Singleton
    fun provideWallpaperDao(database: PaperizeDatabase): WallpaperDao =
        database.wallpaperDao()

    @Provides
    @Singleton
    fun provideFolderDao(database: PaperizeDatabase): FolderDao =
        database.folderDao()

    @Provides
    @Singleton
    fun provideWallpaperQueueDao(database: PaperizeDatabase): WallpaperQueueDao =
        database.wallpaperQueueDao()

    /**
     * Provide PreferencesManager
     */
    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager =
        PreferencesManager(context)

    /**
     * Provide Repositories
     */
    @Provides
    @Singleton
    fun provideAlbumRepository(
        @ApplicationContext context: Context,
        database: PaperizeDatabase,
        albumDao: AlbumDao,
        wallpaperDao: WallpaperDao,
        folderDao: FolderDao
    ): AlbumRepository = AlbumRepositoryImpl(context, database, albumDao, wallpaperDao, folderDao)

    @Provides
    @Singleton
    fun provideWallpaperRepository(
        @ApplicationContext context: Context,
        wallpaperDao: WallpaperDao,
        wallpaperQueueDao: WallpaperQueueDao
    ): WallpaperRepository = WallpaperRepositoryImpl(context, wallpaperDao, wallpaperQueueDao)

    @Provides
    @Singleton
    fun provideSettingsRepository(
        preferencesManager: PreferencesManager
    ): SettingsRepository = SettingsRepositoryImpl(preferencesManager)
}
