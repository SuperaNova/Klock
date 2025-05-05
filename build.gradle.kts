// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) version "1.9.22" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
    alias(libs.plugins.androidx.navigation.safeargs) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}