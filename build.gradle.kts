buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // GeckoView 154 pulls Kotlin 2.4.10 metadata. AGP 9 built-in Kotlin defaults lower,
        // so explicitly raise KGP rather than suppressing metadata compatibility checks.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.10")
    }
}

plugins {
    id("com.android.application") version "9.3.1" apply false
}
