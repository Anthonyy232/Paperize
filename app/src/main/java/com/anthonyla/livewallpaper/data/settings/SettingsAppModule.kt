package com.anthonyla.livewallpaper.data.settings

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SettingsAppModule {
    @Singleton
    @Provides
    fun providesSettingsDataStore (
        @ApplicationContext context: Context
    ): SettingsDataStore = SettingsDataStoreImpl(context)

    @Provides
    fun provideContext(
        @ApplicationContext context: Context,
    ): Context {
        return context
    }
}