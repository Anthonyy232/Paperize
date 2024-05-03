package com.anthonyla.paperize.feature.wallpaper.data.repository

import com.anthonyla.paperize.feature.wallpaper.data.data_source.AlbumDao
import com.anthonyla.paperize.feature.wallpaper.domain.model.Album
import com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaperAndFolder
import com.anthonyla.paperize.feature.wallpaper.domain.model.Folder
import com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper
import com.anthonyla.paperize.feature.wallpaper.domain.repository.AlbumRepository
import kotlinx.coroutines.flow.Flow

class AlbumRepositoryImpl(
    private val dao: AlbumDao
): AlbumRepository {
    override fun getAlbumsWithWallpaperAndFolder(): Flow<List<AlbumWithWallpaperAndFolder>> {
        return dao.getAlbumsWithWallpaperAndFolder()
    }

    override suspend fun upsertAlbumWithWallpaperAndFolder(albumWithWallpaperAndFolder: AlbumWithWallpaperAndFolder) {
        dao.upsertAlbum(albumWithWallpaperAndFolder.album)
        dao.upsertWallpaperList(albumWithWallpaperAndFolder.wallpapers)
        dao.upsertFolderList(albumWithWallpaperAndFolder.folders)
    }

    override suspend fun upsertAlbum(album: Album) {
        dao.upsertAlbum(album)
    }

    override suspend fun upsertWallpaper(wallpaper: Wallpaper) {
        dao.upsertWallpaper(wallpaper)
    }

    override suspend fun upsertFolder(folder: Folder) {
        dao.upsertFolder(folder)
    }

    override suspend fun upsertFolderList(folders: List<Folder>) {
        dao.upsertFolderList(folders)
    }

    override suspend fun upsertWallpaperList(wallpapers: List<Wallpaper>) {
        dao.upsertWallpaperList(wallpapers)
    }

    override suspend fun deleteAlbum(album: Album) {
        dao.deleteAlbum(album)
    }

    override suspend fun deleteWallpaper(wallpaper: Wallpaper) {
        dao.deleteWallpaper(wallpaper)
    }

    override suspend fun deleteFolder(folder: Folder) {
        dao.deleteFolder(folder)
    }

    override suspend fun updateAlbum(album: Album) {
        dao.updateAlbum(album)
    }

    override suspend fun updateFolder(folder: Folder) {
        dao.updateFolder(folder)
    }

    override suspend fun updateWallpaper(wallpaper: Wallpaper) {
        dao.updateWallpaper(wallpaper)
    }

    override suspend fun cascadeDeleteAlbum(album: Album) {
        dao.deleteAlbum(album)
        dao.cascadeDeleteWallpaper(album.initialAlbumName)
        dao.cascadeDeleteFolder(album.initialAlbumName)
    }

    override suspend fun cascadeDeleteFolder(initialAlbumName: String) {
        dao.cascadeDeleteFolder(initialAlbumName)
    }

    override suspend fun cascadeDeleteWallpaper(initialAlbumName: String) {
        dao.cascadeDeleteWallpaper(initialAlbumName)
    }

    override suspend fun deleteFolderList(folders: List<Folder>) {
        dao.deleteFolderList(folders)
    }

    override suspend fun deleteWallpaperList(wallpapers: List<Wallpaper>) {
        dao.deleteWallpaperList(wallpapers)
    }

    override suspend fun deleteAllData() {
        dao.deleteAllData()
    }
}