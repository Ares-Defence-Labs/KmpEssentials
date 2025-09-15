import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)

    kotlin("plugin.serialization") version "2.2.20"

    id("org.gradle.maven-publish")
    id("signing")
    id("maven-publish")
    id("com.vanniktech.maven.publish") version "0.34.0"
    id("kotlin-parcelize")
}

tasks.register("assembleXCFramework") {
    group = "build"
    dependsOn(
//        "linkDebugFrameworkIosArm64",
//        "linkDebugFrameworkIosX64",
//        "linkDebugFrameworkIosSimulatorArm64",
        "linkReleaseFrameworkIosArm64",
        "linkReleaseFrameworkIosSimulatorArm64"
    )

    val xcFrameworkDir = layout.buildDirectory.dir("XCFrameworks/KmpEssentials.xcframework")

    doLast {
        xcFrameworkDir.get().asFile.deleteRecursively()

        exec {
            commandLine(
                "xcodebuild",
                "-create-xcframework",

                "-framework",
                "${buildDir}/bin/iosArm64/releaseFramework/KmpEssentials.framework",
                "-framework",
                "${buildDir}/bin/iosSimulatorArm64/releaseFramework/KmpEssentials.framework",

//                "-framework", "${buildDir}/bin/iosArm64/debugFramework/shared.framework",
//                "-framework", "${buildDir}/bin/iosX64/debugFramework/shared.framework",
//                "-framework", "${buildDir}/bin/iosSimulatorArm64/debugFramework/shared.framework",

                "-output",
                xcFrameworkDir.get().asFile.absolutePath
            )
        }

        println("XCFramework created at ${xcFrameworkDir.get().asFile.absolutePath}")
    }
}

kotlin {
    kotlin.applyDefaultHierarchyTemplate()
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach {
//        it.compilations {
//            val main by getting {
//                cinterops {
//                    create("SwiftEssentialsEngine")
//                }
//            }
//        }
        it.binaries.framework {
            baseName = "KmpEssentials"
        }
    }

    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }

        publishLibraryVariants("release")
    }

    // watchOS (Apple)
    watchosArm64()
    watchosArm32()
    watchosX64()
    watchosSimulatorArm64()

    // java target -- Compose UI (Mac, Linux, Desktop)
    jvm()

    // tvos (Apple)
    tvosX64()
    tvosArm64()
    tvosSimulatorArm64()

    // browser
    js(IR) {
        //binaries.library()
        browser()
        nodejs()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("co.touchlab:kermit:2.0.8")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1-0.6.x-compat")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
            }
        }

        // watch os target
        val watchosMain by getting {

        }
        val watchosX64Main by getting {

        }
        val watchosArm32Main by getting {

        }
        val watchosArm64Main by getting {

        }
        val watchosSimulatorArm64Main by getting {

        }

        // jvm
        val jvmMain by getting {
            dependencies {
                implementation("org.json:json:20250517")
                implementation("org.bytedeco:javacv:1.5.12")
                implementation("net.java.dev.jna:jna-platform:5.17.0")
                implementation("net.java.dev.jna:jna:5.17.0")
                implementation("org.quartz-scheduler:quartz:2.5.0")
            }
        }

        val jsMain by getting {
            dependencies {
                implementation("com.diglol.crypto:crypto:0.2.0")
                implementation("org.kotlincrypto.hash:sha2:0.7.1")
            }
        }

        // tvos targets
        val tvosMain by getting {

        }
        val tvosX64Main by getting {

        }
        val tvosArm64Main by getting {

        }
        val tvosSimulatorArm64Main by getting {

        }

        // android & iOS targets
        val androidMain by getting {
            dependencies {
                implementation("com.google.assistant.suggestion:assistant-suggestions:2.2.1")
                implementation("androidx.core:core-google-shortcuts:1.1.0")
                implementation("com.google.android.play:review:2.0.2")
                implementation("com.google.android.play:review-ktx:2.0.2")

                implementation("dev.tmapps:konnection:1.4.5")
                implementation("com.liftric:kvault:1.12.0")

                implementation("io.coil-kt:coil:2.7.0")
                implementation("androidx.preference:preference:1.2.1")
                implementation("com.google.android.material:material:1.12.0")
                implementation("androidx.biometric:biometric:1.1.0")
                implementation("androidx.activity:activity-ktx:1.9.1")
                implementation("androidx.appcompat:appcompat:1.7.1")
                implementation("androidx.startup:startup-runtime:1.2.0")
                implementation("androidx.core:core-ktx:1.17.0")
                implementation("androidx.work:work-runtime-ktx:2.10.4")
                implementation("com.google.android.gms:play-services-location:21.3.0")
                implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.9.3")
                implementation(libs.androidx.lifecycle.process)
            }
        }

        // iOS Targets
        val iosArm64Main by getting
        val iosX64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by getting {
            resources.srcDirs("src/iosMain/resources")
            dependencies {
                implementation("dev.tmapps:konnection:1.4.5")
                implementation("com.liftric:kvault:1.12.0")
            }
        }
    }
}

//
//tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink>().configureEach {
//    doLast {
//        val frameworks = listOf(
//            "bin/iosArm64/releaseFramework/KmpEssentials.framework",
//            "bin/iosArm64/debugFramework/KmpEssentials.framework",
//            "bin/iosSimulatorArm64/releaseFramework/KmpEssentials.framework",
//            "bin/iosSimulatorArm64/debugFramework/KmpEssentials.framework",
//            "bin/iosX64/debugFramework/KmpEssentials.framework",
//            "bin/iosX64/releaseFramework/KmpEssentials.framework"
//        )
//
//        val mp3File = File("src/iosMain/resources/silencer_audio_backgrounding.mp3")
//
//        if (!mp3File.exists()) {
//            println("❌ MP3 file not found in resources directory!")
//            return@doLast
//        }
//
//        frameworks.forEach { frameworkPath ->
//            val frameworkDir = File(buildDir, frameworkPath)
//            val resourcesDir = File(frameworkDir, "Resources")
//            resourcesDir.mkdirs()
//
//            val targetFile = File(resourcesDir, mp3File.name)
//            mp3File.copyTo(targetFile, overwrite = true)
//            println("✅ MP3 file copied to $frameworkPath")
//        }
//    }
//}
//

afterEvaluate {
    mavenPublishing {
        // Define coordinates for the published artifact
        coordinates(
            groupId = "io.github.thearchitect123",
            artifactId = "kmpEssentials",
            version = "2.4.8"
        )

        // Configure POM metadata for the published artifact
        pom {
            name.set("KmpEssentials")
            description.set("An essentials library for Kotlin multiplatform that makes it easy to work with any native apis from your shared business logic. Supports iOS & Android")
            inceptionYear.set("2024")
            url.set("https://github.com/TheArchitect123/KmpEssentials")

            licenses {
                license {
                    name.set("MIT")
                    url.set("https://opensource.org/licenses/MIT")
                }
            }

            // Specify developers information
            developers {
                developer {
                    id.set("Dan Gerchcovich")
                    name.set("TheArchitect123")
                    email.set("dan.developer789@gmail.com")
                }
            }

            // Specify SCM information
            scm {
                url.set("https://github.com/TheArchitect123/KmpEssentials")
            }
        }

        // Configure publishing to Maven Central
        publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

        // Enable GPG signing for all publications
        signAllPublications()
    }
}

signing {
    val privateKeyFile = project.properties["signing.privateKeyFile"] as? String
        ?: error("No Private key file found")
    val passphrase = project.properties["signing.password"] as? String
        ?: error("No Passphrase found for signing")

    // Read the private key from the file
    val privateKey = File(privateKeyFile).readText(Charsets.UTF_8)

    useInMemoryPgpKeys(privateKey, passphrase)
    sign(publishing.publications)
}

android {
    namespace = "io.github.thearchitect123"
    compileSdk = libs.versions.droidCompileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.droidMinSdk.get().toInt()
    }
    buildFeatures {
        viewBinding = true // Enable ViewBinding
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}