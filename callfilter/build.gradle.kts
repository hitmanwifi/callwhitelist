plugins { alias(libs.plugins.android.library) }

android { namespace = "org.alexrust.callwhitelist.callfilter"; compileSdk = libs.versions.compileSdk.get().toInt(); defaultConfig { minSdk = libs.versions.minSdk.get().toInt() } }

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":core:model"))
    implementation(project(":core:preferences"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.datetime)
}
