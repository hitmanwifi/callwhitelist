plugins { alias(libs.plugins.android.library); alias(libs.plugins.legacy.kapt) }

android { namespace = "org.alexrust.callwhitelist.database"; compileSdk = libs.versions.compileSdk.get().toInt(); defaultConfig { minSdk = libs.versions.minSdk.get().toInt() } }

dependencies {
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)
}
