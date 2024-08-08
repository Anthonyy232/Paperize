buildscript {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    id("com.android.application") version "8.5.1" apply false
    id("com.android.library") version "8.5.1" apply false
    id("org.jetbrains.kotlin.android") version "2.0.10" apply false
    id ("org.jetbrains.kotlin.plugin.compose") version "2.0.10" apply false
    id("com.google.dagger.hilt.android") version "2.52" apply false
    id("com.google.devtools.ksp") version "2.0.10-1.0.24" apply false
    id ("com.mikepenz.aboutlibraries.plugin") version "11.2.2" apply false
    id ("org.jetbrains.kotlin.plugin.serialization") version "2.0.10" apply false
}



