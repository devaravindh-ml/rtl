buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://repo.objectbox.io/objectbox")
    }

    dependencies {
        classpath("io.objectbox:objectbox-gradle-plugin:4.0.3")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    id("com.google.dagger.hilt.android") version "2.51" apply false
}