plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.musicalike"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.musicalike"
        minSdk = 29
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.androidthings)
    implementation(libs.play.services.auth)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Firebase BOM y servicios
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.android.gms:play-services-auth:20.2.0")
    implementation("com.google.firebase:firebase-firestore:24.0.0")
    implementation("com.google.firebase:firebase-auth:21.0.5")
    implementation("com.google.code.gson:gson:2.8.8")
    implementation("com.google.apis:google-api-services-youtube:v3-rev222-1.25.0")
    implementation("com.google.http-client:google-http-client-jackson2:1.42.0")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.6.4")

    // Spotify Authentication
    implementation("com.spotify.android:auth:1.2.3")
    implementation("com.google.android.gms:play-services-auth:20.6.0")
    implementation("com.google.api-client:google-api-client:1.32.1")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.32.1")
    implementation("com.google.apis:google-api-services-youtube:v3-rev222-1.25.0")
    implementation("com.fasterxml.jackson.core:jackson-core:2.12.5")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.12.5")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.12.5")

    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.0")

    // Otras dependencias
    implementation("androidx.activity:activity-ktx:1.7.2")
    implementation("com.google.android.material:material:1.9.0")
    implementation("com.google.code.gson:gson:2.6.1")
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
}