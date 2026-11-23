plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.applify.watertracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.applify.watertracker"
        minSdk = 33
        targetSdk = 35
        versionCode = 6
        versionName = "1.5.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        warningsAsErrors = true
        disable += listOf("AndroidGradlePluginVersion", "GradleDependency", "NewerVersionAvailable", "IconLauncherShape")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)
    testImplementation(libs.junit)
}
