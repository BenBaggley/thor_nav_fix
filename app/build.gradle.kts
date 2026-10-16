import com.android.build.api.dsl.ApplicationExtension
import java.io.FileInputStream
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.koin.compiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_19
    }
}

ktlint {
    android.set(true)
    version.set("1.8.0")
    reporters {
        reporter(ReporterType.JSON)
    }
}

private fun getCredentials() = Properties().apply {
    load(FileInputStream(rootProject.file("credentials.properties")))
}

extensions.configure<ApplicationExtension> {
    namespace = "dev.benkidd.thornav"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "dev.benkidd.thornav"
        minSdk = 33
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_19
        targetCompatibility = JavaVersion.VERSION_19
    }

    buildFeatures {
        compose = true
        aidl = true
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            val credentials = getCredentials()
            val keystoreFile = credentials.getProperty("signing_store_file")
            storeFile = rootProject.file(keystoreFile)
            keyAlias = credentials.getProperty("signing_alias")
            keyPassword = credentials.getProperty("signing_key_pwd")
            storePassword = credentials.getProperty("signing_store_pwd")
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.material)
    implementation(libs.koin.android)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
}
