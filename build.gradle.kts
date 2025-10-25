plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.google.dagger.hilt.android") version "2.52" apply false  // ✅ Match version
    id("com.google.devtools.ksp") version "2.1.0-1.0.29" apply false  // ✅ Match Kotlin!
    alias(libs.plugins.google.gms.google.services) apply false
}