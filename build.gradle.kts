// Top-level build file.
// AGP 9 compiles Kotlin itself ("built-in Kotlin"), so the Kotlin Android plugin is NOT applied
// to the app module. It is declared here with `apply false` only to pin the Kotlin Gradle plugin
// version on the build classpath, so it matches the Compose compiler plugin version.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
