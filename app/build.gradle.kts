plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    kotlin("kapt")
}

android {
    namespace = "com.portfolio.iptvplayer"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.portfolio.iptvplayer"
        minSdk = 21 // Android TV devices as old as Lollipop are still common in the wild
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.fragment:fragment-ktx:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")

    // Leanback: purpose-built TV framework — D-pad focus, grid scrolling,
    // and card presentation come for free instead of being hand-rolled.
    implementation("androidx.leanback:leanback:1.0.0")

    // Media3 ExoPlayer: hardware-accelerated decode, adaptive bitrate,
    // and HLS/DASH support out of the box — the standard choice for
    // smooth playback on TV hardware of very different capability tiers.
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
    implementation("androidx.media3:media3-datasource-okhttp:1.4.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Glide: bitmap pooling makes it the fastest choice for a D-pad-scrolled
    // grid of channel logos — RecyclerView/GridView reuse views constantly
    // as focus moves, and Glide's pool avoids re-allocating bitmaps per scroll.
    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-android-compiler:2.51.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
