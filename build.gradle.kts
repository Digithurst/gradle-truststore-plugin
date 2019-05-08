/* * * * * * * * * * * *
 *  Parameters
 * * * * * * * * * * * */

// name --> settings.gradle.kts
group = "com.digithurst"
version = System.getenv("BUILD_LABEL") ?: "local"
val isReleaseBuild = System.getenv("BUILD_IS_RELEASE")?.toBoolean() ?: false

/* * * * * * * * * * * *
 *  Configure Plugins
 * * * * * * * * * * * */

plugins {
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "0.10.1"
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

gradlePlugin {
    plugins {
        create("truststorePlugin") {
            id = "com.digithurst.gradle.truststore"
            implementationClass = "com.digithurst.gradle.truststore.TruststorePlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/Digithurst/gradle-truststore-plugin"
    vcsUrl = "https://github.com/Digithurst/gradle-truststore-plugin.git"

    (plugins) {
        "truststorePlugin" {
            displayName = "Custom Trust Stores for Builds"
            description = """
                Configure a custom trust store for Gradle to use during builds.
                For example, this allows Gradle to pull dependencies from a Maven
                repository that uses a self-signed certificate.
            """.trimIndent()
            tags = listOf("certificates", "ca", "truststore", "build-configuration")
        }
    }
}