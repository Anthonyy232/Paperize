# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-keepattributes Signature
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep class kotlin.coroutines.Continuation
-keep class androidx.datastore.*.** {*;}

-keepclassmembers class com.anthonyla.paperize.feature.wallpaper.domain.model.Album {
 !transient <fields>;
}
-keepclassmembers class com.anthonyla.paperize.feature.wallpaper.domain.model.AlbumWithWallpaperAndFolder {
 !transient <fields>;
}
-keepclassmembers class com.anthonyla.paperize.feature.wallpaper.domain.model.Wallpaper {
 !transient <fields>;
}
-keepclassmembers class com.anthonyla.paperize.feature.wallpaper.domain.model.Folder {
 !transient <fields>;
}

# Keep wallpaper services and their action enums
-keep class com.anthonyla.paperize.feature.wallpaper.wallpaper_service.HomeWallpaperService { *; }
-keep class com.anthonyla.paperize.feature.wallpaper.wallpaper_service.HomeWallpaperService$Actions { *; }
-keep class com.anthonyla.paperize.feature.wallpaper.wallpaper_service.LockWallpaperService { *; }
-keep class com.anthonyla.paperize.feature.wallpaper.wallpaper_service.LockWallpaperService$Actions { *; }

# Keep WallpaperAction classes
-keep class com.anthonyla.paperize.feature.wallpaper.wallpaper_alarmmanager.WallpaperAction { *; }
-keep class com.anthonyla.paperize.feature.wallpaper.wallpaper_alarmmanager.WallpaperAction$* { *; }

# Keep Type enum
-keep class com.anthonyla.paperize.core.Type { *; }