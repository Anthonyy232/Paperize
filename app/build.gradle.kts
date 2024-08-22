plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("com.mikepenz.aboutlibraries.plugin")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
}
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    namespace = "com.anthonyla.paperize"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.anthonyla.paperize"
        minSdk = 26
        targetSdk = 35
        versionCode = 30
        versionName = "2.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs += strongSkippingConfiguration()
    }

    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }

    androidResources {
        generateLocaleConfig = true
    }

    ndkVersion = "26.3.11579264"
    buildToolsVersion = "34.0.0"
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    aboutLibraries {
        excludeFields = arrayOf("generated")
    }
}

androidComponents {
    onVariants(selector().withBuildType("release")) {
        it.packaging.resources.excludes.add("META-INF/**")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui:1.7.0-beta07")
    implementation("androidx.compose.ui:ui-graphics:1.7.0-rc01")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.0-beta07")
    implementation("androidx.compose.material3:material3:1.3.0-beta05")
    implementation("androidx.navigation:navigation-compose:2.8.0-beta07")
    implementation("androidx.compose.material:material:1.7.0-beta07")
    implementation("androidx.datastore:datastore:1.1.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.compose.material:material-icons-extended:1.7.0-beta07")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.compose.animation:animation:1.7.0-beta07")
    implementation("androidx.core:core-splashscreen:1.2.0-alpha01")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("androidx.documentfile:documentfile:1.1.0-alpha01")
    implementation("net.engawapg.lib:zoomable:2.0.0-beta01")
    implementation("com.github.skydoves:landscapist-glide:2.3.6")
    implementation("androidx.work:work-runtime-ktx:2.10.0-alpha02")
    implementation("androidx.hilt:hilt-work:1.2.0")
    implementation("com.airbnb.android:lottie-compose:6.5.0")
    implementation("com.google.accompanist:accompanist-permissions:0.35.0-alpha")
    implementation("com.mikepenz:aboutlibraries-core:11.2.2")
    implementation("com.mikepenz:aboutlibraries-compose-m3:11.2.2")
    implementation("androidx.compose.foundation:foundation:1.7.0-beta07")
    implementation("com.github.nanihadesuka:LazyColumnScrollbar:2.2.0")
    implementation("com.joaomgcd:taskerpluginlibrary:0.4.10")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.7.0-beta07")
    debugImplementation("androidx.compose.ui:ui-tooling:1.7.0-beta07")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.7.0-beta07")
    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-android-compiler:2.52")
    implementation("androidx.room:room-runtime:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("com.lazygeniouz:dfc:1.0.8")
    implementation ("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    implementation("com.github.android:renderscript-intrinsics-replacement-toolkit:b6363490c3")
}

private fun strongSkippingConfiguration() = listOf(
    "-P",
    "plugin:androidx.compose.compiler.plugins.kotlin:experimentalStrongSkipping=true",
)