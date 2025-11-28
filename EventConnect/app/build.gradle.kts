plugins {
    // Unificar plugins con el catálogo
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
}

android {
    namespace = "com.example.eventconnect"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.eventconnect"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["MAPS_API_KEY"] = (project.findProperty("MAPS_API_KEY") as? String) ?: ""
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
        // 🔧 Fuerza a que Java y Kotlin usen el mismo target
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        // 🔧 Asegura compatibilidad con la JVM
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    // Jetpack Compose (alineado al BOM del catálogo)
    implementation(platform(libs.androidx.compose.bom))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.8.0")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation("io.coil-kt:coil-gif:2.4.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("androidx.compose.material:material-icons-extended")

    // Compose Foundation + tooling preview (gestionadas por el BOM)
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // WorkManager para sincronización en segundo plano
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Editor de imágenes externo eliminado: migrado a edición nativa (rotar/flip/crop) sin dependencias

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")

    // Retirado: Places SDK, lo activaremos cuando proveamos la API key y validar integración

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")

    testImplementation("io.mockk:mockk:1.13.11")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("app.cash.turbine:turbine:1.0.0")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.test:core-ktx:1.6.1")
}

// JaCoCo (reemplazado)
plugins.apply("jacoco")

// Tarea de reporte para unidad debug
tasks.register<JacocoReport>("jacocoDebugReport") {
    // En Android la tarea de pruebas unitarias del variant debug es 'testDebugUnitTest'
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val fileFilter = listOf(
        "**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*",
        "**/*Test*.*", "android/**/*.*"
    )

    val buildDirPath = layout.buildDirectory.get().asFile.absolutePath
    val debugClassesDir = "$buildDirPath/tmp/kotlin-classes/debug"
    val javaClassesDir = "$buildDirPath/intermediates/javac/debug/classes"

    val debugTree = fileTree(debugClassesDir) { exclude(fileFilter) }
    // Incluimos también clases Java si existieran
    val javaTree = fileTree(javaClassesDir) { exclude(fileFilter) }

    classDirectories.setFrom(files(debugTree, javaTree))
    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
    executionData.setFrom(fileTree(layout.buildDirectory) {
        include("**/jacoco/testDebugUnitTest.exec")
    })
}

// Tarea opcional combinada si agregas más variantes en el futuro
// tasks.register<JacocoReport>("jacocoAllVariantsReport") {
//     dependsOn("jacocoDebugReport")
//     reports { xml.required.set(true); html.required.set(true) }
//     val reportsDir = layout.buildDirectory.dir("reports/jacoco")
// }
