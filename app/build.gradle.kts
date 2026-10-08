import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    // Firebase: lê o app/google-services.json (baixado do console do Firebase)
    alias(libs.plugins.google.services)
}

// Configuração do Cloudinary fica no local.properties (não vai para o Git):
//   cloudinary.cloudName=SEU_CLOUD_NAME
//   cloudinary.uploadPreset=SEU_UPLOAD_PRESET
val localProps = Properties().apply {
    val arquivo = rootProject.file("local.properties")
    if (arquivo.exists()) arquivo.inputStream().use { load(it) }
}
fun localProp(nome: String): String = (localProps.getProperty(nome) ?: "").trim()

android {
    namespace = "com.example.renovai"
    compileSdk = 36

    val keystoreFile = System.getenv("ANDROID_KEYSTORE_FILE")
    val keystorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
    val keyAlias = System.getenv("ANDROID_KEY_ALIAS")
    val keyPassword = System.getenv("ANDROID_KEY_PASSWORD")

    if (!keystoreFile.isNullOrBlank() && !keystorePassword.isNullOrBlank() &&
        !keyAlias.isNullOrBlank() && !keyPassword.isNullOrBlank()
    ) {
        signingConfigs {
            create("release") {
                storeFile = file(keystoreFile)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    defaultConfig {
        applicationId = "com.example.renovai"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"${localProp("cloudinary.cloudName")}\"")
        buildConfigField("String", "CLOUDINARY_UPLOAD_PRESET", "\"${localProp("cloudinary.uploadPreset")}\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (signingConfigs.findByName("release") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Retrofit & OkHttp
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // OkHttp

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // RecyclerView (listas da Home: pedidos e cooperativas recentes)
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Fragment (aba Home dentro da EmpresaActivity)
    implementation("androidx.fragment:fragment:1.6.2")

    // Glide (carregar foto da empresa e das cooperativas a partir de uma URL)
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Cloudinary: upload das fotos (coletas)
    implementation("com.cloudinary:cloudinary-android:3.1.2")

    // Firebase: Firestore (cache offline), Storage (relatórios PDF) e Auth (login anônimo p/ regras)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.auth)
}