package com.anthonyla.paperize.core

class EmptyAlbumException : Exception("Album has no wallpapers")

class NoValidWallpaperException : Exception("No readable wallpaper found")
