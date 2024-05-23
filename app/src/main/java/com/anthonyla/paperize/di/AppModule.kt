package com.anthonyla.paperize.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.anthonyla.paperize.data.settings.SettingsDataStore
import com.anthonyla.paperize.data.settings.SettingsDataStoreImpl
import com.anthonyla.paperize.feature.wallpaper.data.data_source.AlbumDatabase
import com.anthonyla.paperize.feature.wallpaper.data.data_source.SelectedAlbumDatabase
import com.anthonyla.paperize.feature.wallpaper.data.repository.AlbumRepositoryImpl
import com.anthonyla.paperize.feature.wallpaper.data.repository.SelectedAlbumRepositoryImpl
import com.anthonyla.paperize.feature.wallpaper.domain.repository.AlbumRepository
import com.anthonyla.paperize.feature.wallpaper.domain.repository.SelectedAlbumRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * AppModule provides dependencies for the application for Dagger Hilt to inject
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideAlbumDatabase(app: Application): AlbumDatabase {
        return Room.databaseBuilder(
            app,
            AlbumDatabase::class.java,
            AlbumDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().addMigrations().build()
    }

    @Provides
    @Singleton
    fun provideSelectedAlbumDatabase(app: Application): SelectedAlbumDatabase {
        return Room.databaseBuilder(
            app,
            SelectedAlbumDatabase::class.java,
            SelectedAlbumDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().addMigrations().build()
    }

    @Provides
    @Singleton
    fun provideAlbumDao(db: AlbumDatabase) = db.albumDao

    @Provides
    @Singleton
    fun provideAlbumRepository(
        db: AlbumDatabase
    ): AlbumRepository {
        return AlbumRepositoryImpl(db.albumDao)
    }

    @Provides
    @Singleton
    fun provideSelectedAlbumDao(db: SelectedAlbumDatabase) = db.selectedAlbumDao

    @Provides
    @Singleton
    fun provideSelectedAlbumRepository(
        db: SelectedAlbumDatabase
    ): SelectedAlbumRepository {
        return SelectedAlbumRepositoryImpl(db.selectedAlbumDao)
    }

    @Provides
    @Singleton
    fun provideSettingsDataStore (
        @ApplicationContext context: Context
    ): SettingsDataStore = SettingsDataStoreImpl(context)

    @Provides
    @Singleton
    fun provideContext(
        @ApplicationContext context: Context,
    ): Context { return context }
}