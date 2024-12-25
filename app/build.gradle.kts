plugins {
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.musicalike"  // Aquí se define el namespace
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
}

dependencies {
    // Dependencias de AndroidX y otras bibliotecas
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
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // Spotify Authentication
    implementation("com.spotify.android:auth:1.2.3")
    implementation ("com.google.android.gms:play-services-auth:20.3.0")
    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    //implementation ("com.spotify.android:spotify-app-remote:0.7.0")
    // Otras dependencias
    implementation("androidx.activity:activity-ktx:1.7.2")
    implementation("com.google.android.material:material:1.9.0")
    implementation("com.google.code.gson:gson:2.6.1")
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")

    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    // Firebase Auth
    implementation("com.google.firebase:firebase-auth")
}
