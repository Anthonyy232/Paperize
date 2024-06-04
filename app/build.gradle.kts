plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id ("com.mikepenz.aboutlibraries.plugin")
    id ("org.jetbrains.kotlin.plugin.serialization")
}
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    namespace = "com.anthonyla.paperize"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.anthonyla.paperize"
        minSdk = 26
        targetSdk = 34
        versionCode = 17
        versionName = "1.5.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        ndk {
            debugSymbolLevel = "FULL"
        }
    }


    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        kotlinCompilerExtensionVersion = "1.5.10"
    }

    androidResources {
        generateLocaleConfig = true
    }
}

androidComponents {
    onVariants(selector().withBuildType("release")) {
        it.packaging.resources.excludes.add("META-INF/**")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation(platform("androidx.compose:compose-bom:2024.05.00"))
    implementation("androidx.compose.ui:ui:1.7.0-beta02")
    implementation("androidx.compose.ui:ui-graphics:1.7.0-beta02")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.0-beta02")
    implementation("androidx.compose.material3:material3:1.3.0-beta02")
    implementation("androidx.navigation:navigation-compose:2.8.0-beta02")
    implementation("androidx.compose.material:material:1.7.0-beta02")
    implementation("androidx.datastore:datastore:1.1.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.compose.material:material-icons-extended:1.7.0-beta02")
    implementation("com.google.accompanist:accompanist-adaptive:0.34.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.compose.animation:animation:1.7.0-beta02")
    implementation("androidx.core:core-splashscreen:1.2.0-alpha01")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.1")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("androidx.documentfile:documentfile:1.1.0-alpha01")
    implementation("net.engawapg.lib:zoomable:1.7.0-beta02")
    implementation("com.github.skydoves:landscapist-glide:2.3.3")
    implementation("androidx.work:work-runtime-ktx:2.10.0-alpha02")
    implementation("androidx.hilt:hilt-work:1.2.0")
    implementation("com.airbnb.android:lottie-compose:6.4.1")
    implementation("com.google.accompanist:accompanist-permissions:0.35.0-alpha")
    implementation("com.mikepenz:aboutlibraries-core:11.2.1")
    implementation("com.mikepenz:aboutlibraries-compose-m3:11.2.1")
    implementation("androidx.compose.foundation:foundation:1.7.0-beta02")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.0-rc01")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.7.0-beta02")
    debugImplementation("androidx.compose.ui:ui-tooling:1.7.0-beta02")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.7.0-beta02")
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-android-compiler:2.51.1")
    implementation("androidx.room:room-runtime:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("com.lazygeniouz:dfc:1.0.8")
    implementation ("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.github.android:renderscript-intrinsics-replacement-toolkit:b6363490c3")
}

private fun strongSkippingConfiguration() = listOf(
    "-P",
    "plugin:androidx.compose.compiler.plugins.kotlin:experimentalStrongSkipping=true",
)