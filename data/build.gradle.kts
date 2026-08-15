plugins { alias(libs.plugins.android.library) }

android { namespace = "org.alexrust.callwhitelist.data"; compileSdk = libs.versions.compileSdk.get().toInt(); defaultConfig { minSdk = libs.versions.minSdk.get().toInt() } }

dependencies {
    implementation(project(":domain"))
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation(project(":core:preferences"))
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.datastore.preferences)
}
