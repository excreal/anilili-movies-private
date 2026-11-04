buildscript {
    dependencies { classpath(libs.kotlin.gradle) }
    configurations.classpath {
        resolutionStrategy.eachDependency {
            if (requested.group == "com.google.protobuf" && requested.name in setOf("protobuf-java", "protobuf-java-util")) {
                useVersion("4.36.0")
                because("Protobuf 36 removes terminally deprecated Unsafe use for standard Java messages")
            }
            if (requested.group == "com.google.crypto.tink" && requested.name == "tink") {
                useVersion("1.24.0")
                because("Replace AGP's legacy generated protobuf classes with supported, regenerated Tink messages")
            }
        }
    }
}
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
allprojects {
    configurations.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "com.google.protobuf" && requested.name in setOf("protobuf-java", "protobuf-java-util")) {
                useVersion("4.36.0")
                because("Use the supported Java runtime without Unsafe memory access")
            }
        }
    }
}
