plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.genesjc.microclimatefinder"
    compileSdk {
        version = release(version = 37)
    }

    defaultConfig {
        applicationId = "io.github.genesjc.microclimatefinder"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "distribution"

    productFlavors {
        create("foss") {
            dimension = "distribution"
            applicationId = "io.github.genesjc.microclimatefinder"
            versionNameSuffix = "-foss"
            buildConfigField("String", "FLAVOR_TYPE", "\"FOSS\"")
            buildConfigField("Boolean", "IS_COMMERCIAL", "false")
        }
        create("commercial") {
            dimension = "distribution"
            applicationId = "com.genetech.microclimatefinder"
            versionNameSuffix = "-commercial"
            buildConfigField("String", "FLAVOR_TYPE", "\"COMMERCIAL\"")
            buildConfigField("Boolean", "IS_COMMERCIAL", "true")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// Prints the app's build label (BuildInfo.LABEL) in the build output, so it is clear which build ran.
// Read as a file input so Gradle's configuration cache picks up label changes.
val buildLabel = providers.fileContents(
    layout.projectDirectory.file("src/main/java/io/github/genesjc/microclimatefinder/BuildInfo.kt")
).asText.map { text ->
    Regex("LABEL = \"([^\"]+)\"").find(text)?.groupValues?.get(1) ?: "unknown"
}

tasks.register("printBuildLabel") {
    val label = buildLabel
    doLast { println("Build label: ${label.get()}") }
}

tasks.matching { it.name == "preBuild" }.configureEach { dependsOn("printBuildLabel") }
