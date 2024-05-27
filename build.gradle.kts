buildscript {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    id("com.android.application") version "8.4.1" apply false
    id("com.android.library") version "8.4.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    id("com.google.dagger.hilt.android") version "2.50" apply false
    id("com.google.devtools.ksp") version "1.9.0-1.0.13" apply false
    id ("com.mikepenz.aboutlibraries.plugin") version "11.1.3" apply false
    id ("org.jetbrains.kotlin.plugin.serialization") version "1.9.22" apply false
}



