import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.flow.FlowAction
import org.gradle.api.flow.FlowParameters
import org.gradle.api.flow.FlowScope
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
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

    // Two dimensions: the edition (what is in the app) and the distribution (who publishes it).
    // The core edition is the app alone; the demo edition adds Demos & Previews and what is behind it.
    // core and foss are the defaults, so a fresh checkout runs the clean app.
    flavorDimensions += listOf("edition", "distribution")

    productFlavors {
        create("core") {
            dimension = "edition"
            isDefault = true
        }
        create("demo") {
            dimension = "edition"
        }
        create("foss") {
            dimension = "distribution"
            isDefault = true
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
    testOptions {
        // Android calls such as Log.d do nothing in local unit tests instead of throwing
        unitTests.isReturnDefaultValues = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // ViewModel for the Home screen state, and collectAsStateWithLifecycle to read its StateFlow in Compose
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    // Dependency injection: one container, started in the Application class (see di/AppModule.kt)
    implementation(libs.koin.android)
    // The NWS observation call: Retrofit over OkHttp, JSON read into typed classes with kotlinx.serialization
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    // runTest and a test dispatcher for the ViewModel tests
    testImplementation(libs.kotlinx.coroutines.test)
    // A local web server, so the NWS client's requests and its handling of each answer can be tested on the JVM
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.mockk)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// Prints the app's build label (BuildInfo.kt) as the last thing in the build output, once the whole
// build has finished, so it is visible without scrolling. Uses Gradle's build-finished hook (works
// with the configuration cache). The label is read as a file input so a label change is picked up.
val buildLabel = providers.fileContents(
    layout.projectDirectory.file("src/main/java/io/github/genesjc/microclimatefinder/BuildInfo.kt")
).asText.map { text ->
    Regex("LABEL = \"([^\"]+)\"").find(text)?.groupValues?.get(1) ?: "unknown"
}

// Also prints the label at the START of every build, so it shows even when the build stops part
// way (a compile error, say) and the end-of-build line never appears.
abstract class PrintBuildLabelAtStart : DefaultTask() {
    @get:Input
    abstract val label: Property<String>

    @TaskAction
    fun print() {
        println("")
        println("=== Build label: ${label.get()} ===")
    }
}

val printBuildLabelAtStart = tasks.register<PrintBuildLabelAtStart>("printBuildLabel") {
    label.set(buildLabel)
}

// Every build of the app starts with preBuild, so hang the print there
tasks.configureEach {
    if (name == "preBuild") dependsOn(printBuildLabelAtStart)
}

abstract class PrintBuildLabel : FlowAction<PrintBuildLabel.Params> {
    interface Params : FlowParameters {
        @get:Input
        val label: Property<String>
    }

    override fun execute(parameters: Params) {
        println("")
        println("=== Build label: ${parameters.label.get()} ===")
    }
}

interface InjectedFlow {
    @get:Inject
    val flowScope: FlowScope
}

objects.newInstance<InjectedFlow>().flowScope.always(PrintBuildLabel::class.java) {
    parameters.label.set(buildLabel)
}
