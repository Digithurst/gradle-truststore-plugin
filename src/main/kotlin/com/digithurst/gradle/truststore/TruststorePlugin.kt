package com.digithurst.gradle.truststore

import org.gradle.api.GradleScriptException
import org.gradle.api.InvalidUserCodeException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.security.*
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory


/**
 * Configure a custom trust store for Gradle to use during builds.
 * For example, this allows Gradle to pull dependencies from a Maven
 * repository that uses a self-signed certificate.
 *
 * @see Truststore
 *
 * @since 1.0.0
 */
@Suppress("unused") // accessed by Gradle triggered by build.gradle.kts
class TruststorePlugin : Plugin<Project> {
    private lateinit var project: Project

    // TODO: Make configurable?
    private val customKeystore: File by lazy {
        File(project.buildDir.toString() + "/truststores", "cacerts")
    }

    override fun apply(project: Project) {
        this.project = project

        val javaHome = System.getProperty("java.home")
        logger.debug("Found Java home directory: $javaHome")

        val extension = Truststore(project.projectDir, javaHome)
        project.extensions.add(Truststore::class.java, "truststore", extension)

        project.afterEvaluate { this.setupStore(it) }
        logger.debug("Deferred setup of trust store")
    }

    private fun setupStore(project: Project) {
        val truststore = project.extensions.getByType(Truststore::class.java)

        val storeBaseFile = truststore.base.store
        val storeBasePassword = truststore.base.password
        val certificates = truststore.certificates

        logger.debug(
            (listOf("Will try to assemble trust store from:", storeBaseFile) +
                    certificates.map { "${it.alias}:${it.path}" }
                    ).joinToString("\n - "))

        if (truststore.isDefault) {
            check(storeBaseFile != null)
            logger.debug("Using default trust store: ${storeBaseFile.absolutePath}")
        } else if (storeBaseFile != null && certificates.isEmpty()) {
            if (!storeBaseFile.isFile) {
                throw InvalidUserCodeException("Key store file does not exist: ${storeBaseFile.absolutePath}")
            }
            // TODO: Verify that store is valid?

            System.setProperty("javax.net.ssl.trustStore", storeBaseFile.absolutePath)
            System.setProperty("javax.net.ssl.trustStorePassword", storeBasePassword)
            logger.debug("Using custom trust store: ${storeBaseFile.absolutePath}")
        } else {
            // TODO: verify that action has to be taken: do nothing if result is present and inputs haven't changed.
            //       Investigate whether that's worth is: according to log timestamps, the actual import only takes
            //       a few hundreds of a second, any form of reasonable check may take about as long.

            val targetFile = customKeystore
            try {
                val ks: KeyStore

                // Load base store, if any.
                if (storeBaseFile != null) {
                    if (!storeBaseFile.isFile) {
                        throw InvalidUserCodeException("Trust store file does not exist: ${storeBaseFile.absolutePath}")
                    }

                    logger.debug("Importing key store " + storeBaseFile.absoluteFile)

                    // Apparently, there's no way to determine the type of the key store at hand,
                    // so we try one after the other.
                    ks = keyStoreTypes.asSequence() // --> map is lazy
                        .mapNotNull { ksType ->
                            try {
                                logger.debug("Trying to load trust store as $ksType")
                                FileInputStream(storeBaseFile.absoluteFile).use { storeIn ->
                                    val store = KeyStore.getInstance(ksType)
                                    store.load(storeIn, storeBasePassword.toCharArray())
                                    store
                                }
                            } catch (t: Throwable) {
                                when (t) {
                                    is KeyStoreException, is NoSuchAlgorithmException -> {
                                        logger.debug("Trust store is not of type $ksType")
                                        null
                                    }
                                    is CertificateException, is IOException -> {
                                        logger.error("Loading trust store failed", t)
                                        null
                                    }
                                    else -> throw t // unexpected error
                                }
                            }
                        }
                        .firstOrNull()
                        ?: throw KeyStoreException("No provider could load ${storeBaseFile.absoluteFile}")

                    logger.debug("Imported trust store of type ${ks.type}")
                } else {
                    ks = KeyStore.getInstance(KeyStore.getDefaultType())
                    ks.load(null, storeBasePassword.toCharArray())
                }

                // Add custom certificates
                check(certificateFactoryTypes.size == 1) { "Need to refactor to account for multiple possible certificate types" }
                val cf = CertificateFactory.getInstance(certificateFactoryTypes[0])
                for (cert in certificates) {
                    logger.debug("Importing certificate ${cert.path}")
                    FileInputStream(cert.path).use { certIn ->
                        ks.setCertificateEntry(
                            cert.alias,
                            cf.generateCertificate(certIn)
                        )
                    }
                }

                // Write result
                logger.debug("Writing custom trust store to ${targetFile.absolutePath}")
                Files.createDirectories(targetFile.toPath().parent)
                FileOutputStream(targetFile).use { storeOut ->
                    ks.store(storeOut, storeBasePassword.toCharArray())
                }
            } catch (t: Throwable) {
                when (t) {
                    is CertificateException, is KeyStoreException, is IOException, is NoSuchAlgorithmException -> {
                        logger.error("Could not assemble trust store", t)
                        throw GradleScriptException("Could not assemble trust store", t)
                    }
                    else -> throw t // unexpected error
                }
            }

            System.setProperty("javax.net.ssl.trustStore", targetFile.absolutePath)
            System.setProperty("javax.net.ssl.trustStorePassword", storeBasePassword)
            logger.debug("Using aggregated custom trust store: " + targetFile.absolutePath)
        }

        // TODO: Is this always effective? Setting those properties only does anything _before_ the first SSL connection...
        //      If not -- (how) can we inject a custom SSLContext into Gradle?
    }

    internal companion object {
        internal val logger = Logging.getLogger(TruststorePlugin::class.java)

        /**
         * Possible key store types, as per the
         * [Java 8 documentation](https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#KeyStore).
         */
        private val keyStoreTypes = listOf(/*"jceks",*/ "jks", "dks", "pkcs11", "pkcs12")
        /* Note: `jceks` results in
         *           `org.apache.http.ssl.SSLInitializationException: DerInputStream.getLength(): lengthTag=78, too big`
         *        so we leave it out. */

        /**
         * Possible certificate factory types, as per the
         * [Java 8 documentation](https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#CertificateFactory).
         */
        private val certificateFactoryTypes = listOf("X.509")
    }
}

/**
 * Alias for `configure<Truststore> { ... }`.
 */
@Suppress("unused") // Used in build.gradle.kts
inline fun Project.truststore(configuration: Truststore.() -> Unit) {
    extensions.findByType(Truststore::class.java)?.let(configuration)
}
