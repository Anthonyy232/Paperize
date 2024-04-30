plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id ("com.mikepenz.aboutlibraries.plugin")
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
        versionCode = 1
        versionName = "0.2.0"

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
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
}

androidComponents {
    onVariants(selector().withBuildType("release")) {
        it.packaging.resources.excludes.add("META-INF/**")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation(platform("androidx.compose:compose-bom:2024.04.01"))
    implementation("androidx.compose.ui:ui:1.6.6")
    implementation("androidx.compose.ui:ui-graphics:1.6.6")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.6")
    implementation("androidx.compose.material3:material3:1.3.0-alpha05")
    implementation("androidx.navigation:navigation-compose:2.8.0-alpha07")
    implementation("androidx.compose.material:material:1.7.0-alpha07")
    implementation("androidx.datastore:datastore:1.1.0")
    implementation("androidx.datastore:datastore-preferences:1.1.0")
    implementation("androidx.compose.material:material-icons-extended:1.7.0-alpha07")
    implementation("com.google.accompanist:accompanist-adaptive:0.34.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.compose.animation:animation:1.7.0-alpha07")
    implementation("androidx.core:core-splashscreen:1.2.0-alpha01")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.documentfile:documentfile:1.1.0-alpha01")
    implementation("net.engawapg.lib:zoomable:1.7.0-beta02")
    implementation("com.github.skydoves:landscapist-glide:2.3.3")
    implementation("androidx.work:work-runtime-ktx:2.10.0-alpha02")
    implementation("androidx.hilt:hilt-work:1.2.0")
    implementation("com.airbnb.android:lottie-compose:6.4.0")
    implementation("com.google.accompanist:accompanist-permissions:0.35.0-alpha")
    implementation("com.mikepenz:aboutlibraries-core:11.1.3")
    implementation("com.mikepenz:aboutlibraries-compose-m3:11.1.3")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.0-alpha03")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.6.6")
    debugImplementation("androidx.compose.ui:ui-tooling:1.6.6")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.6.6")
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-android-compiler:2.51.1")
    implementation("androidx.room:room-runtime:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("com.lazygeniouz:dfc:1.0.8")
}