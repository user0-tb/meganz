plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
    id("de.mannodermaus.android-junit5")
}

apply(plugin = "jacoco")
apply(from = "${project.rootDir}/tools/jacoco.gradle")
apply(from = "${project.rootDir}/tools/util.gradle")
apply(from = "${project.rootDir}/tools/sdk.gradle")

android {
    val compileSdkVersion: Int by rootProject.extra
    compileSdk = compileSdkVersion
    val buildTools: String by rootProject.extra
    buildToolsVersion = buildTools

    defaultConfig {
        val minSdkVersion: Int by rootProject.extra
        minSdk = minSdkVersion

        val targetSdkVersion: Int by rootProject.extra
        targetSdk = targetSdkVersion

        val appVersion: String by rootProject.extra
        resValue("string", "app_version", "\"${appVersion}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        val javaVersion: JavaVersion by rootProject.extra
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    kotlin {
        val jdk: String by rootProject.extra
        jvmToolchain(jdk.toInt())
    }

    kotlinOptions {
        val jdk: String by rootProject.extra
        jvmTarget = jdk
        val shouldSuppressWarnings: Boolean by rootProject.extra
        suppressWarnings = shouldSuppressWarnings
        freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
    }

    lint {
        abortOnError = false
        xmlOutput = file("build/reports/lint-results.xml")
    }

    tasks.withType<Test> {
        configure<JacocoTaskExtension> {
            isIncludeNoLocationClasses = true
            excludes = listOf("jdk.internal.*")
        }
    }

    flavorDimensions += "service"
    productFlavors {
        create("gms") {
            dimension = "service"
        }
    }

    kapt {
        arguments {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }
    namespace = "mega.privacy.android.data"
}

dependencies {
    implementation(project(":domain"))

    implementation(lib.coroutines.core)
    implementation(google.gson)
    implementation(google.zxing)
    implementation(androidx.java.core)
    implementation(androidx.exifinterface)
    implementation(androidx.datastore.preferences)
    implementation(androidx.preferences)
    implementation(androidx.lifecycle.process)
    implementation(lib.fresco)
    implementation(androidx.work.ktx)
    implementation(androidx.room)
    implementation(androidx.hilt.work)
    implementation(google.hilt.android)
    kapt(google.hilt.android.compiler)
    kapt(androidx.hilt.compiler)
    kapt(androidx.room.compiler)

    "gmsImplementation"(lib.billing.client.ktx)

    coreLibraryDesugaring(lib.desugar)

    // Logging
    implementation(lib.bundles.logging)

    implementation(google.autovalue.annotations)
    kapt(google.autovalue)

    // Testing dependencies
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.truth.ext)
    testImplementation(testlib.test.core.ktx)
    testImplementation(lib.bundles.unit.test)
    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.junit5.api)
    testRuntimeOnly(testlib.junit.jupiter.engine)

    androidTestImplementation(testlib.bundles.unit.test)
    androidTestImplementation(lib.bundles.unit.test)
    androidTestImplementation(testlib.junit.test.ktx)
    androidTestImplementation(testlib.runner)
}

