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

# Keep domain models for serialization
-keepclassmembers class com.anthonyla.paperize.domain.model.Album {
 !transient <fields>;
}
-keepclassmembers class com.anthonyla.paperize.domain.model.Wallpaper {
 !transient <fields>;
}
-keepclassmembers class com.anthonyla.paperize.domain.model.Folder {
 !transient <fields>;
}

# Keep database entities
-keep class com.anthonyla.paperize.data.database.entities.** { *; }

# Keep wallpaper service
-keep class com.anthonyla.paperize.service.wallpaper.WallpaperChangeService { *; }
-keep class com.anthonyla.paperize.service.wallpaper.WallpaperChangeService$* { *; }

# Keep enums
-keep class com.anthonyla.paperize.core.WallpaperSourceType { *; }
-keep class com.anthonyla.paperize.core.ScreenType { *; }
-keep class com.anthonyla.paperize.core.ScalingType { *; }

# Keep navigation routes for serialization
-keep class com.anthonyla.paperize.presentation.common.navigation.** { *; }

# Keep Hilt generated classes (more specific to avoid overly broad rules)
-keep,allowobfuscation,allowshrinking @dagger.hilt.android.lifecycle.HiltViewModel class *
-keep,allowobfuscation,allowshrinking class * extends androidx.lifecycle.ViewModel
-keep class dagger.hilt.android.** { *; }
-keep class dagger.hilt.internal.** { *; }
-keep class javax.inject.** { *; }

# Keep Room database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**