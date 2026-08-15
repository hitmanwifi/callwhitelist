plugins { alias(libs.plugins.android.library) }

android { namespace = "org.alexrust.callwhitelist.preferences"; compileSdk = libs.versions.compileSdk.get().toInt(); defaultConfig { minSdk = libs.versions.minSdk.get().toInt() } }

dependencies {
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
}
