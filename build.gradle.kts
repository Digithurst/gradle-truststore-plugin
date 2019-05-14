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
    kotlin("jvm") version "1.3.31"

    id("com.gradle.plugin-publish") version "0.10.1"
    id("org.jetbrains.dokka") version "0.9.18"
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

/* * * * * * * * * * * *
 *  Configure Dependencies
 * * * * * * * * * * * */

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
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
            """.trimIndent().replace("\n", "")
            tags = listOf("certificates", "ca", "truststore", "build-configuration")
        }
    }
}

/* * * * * * * * * * * *
 *  Configure Tasks
 * * * * * * * * * * * */

tasks {
    compileKotlin.get().kotlinOptions {
        jvmTarget = "1.8"
    }

    compileTestKotlin.get().kotlinOptions {
        jvmTarget = "1.8"
    }

    dokka {
        outputDirectory = "$buildDir/javadoc"
        jdkVersion = 8
        includes = listOf("src/main/kotlin/com/digithurst/gradle/truststore/package-info.md")
    }

    val dokkaJar by creating(Jar::class) {
        from(dokka)
        group = JavaBasePlugin.DOCUMENTATION_GROUP
        classifier = "javadoc"
        description = "Assembles Kotlin docs with Dokka"
    }
    artifacts.add("archives", dokkaJar)
}