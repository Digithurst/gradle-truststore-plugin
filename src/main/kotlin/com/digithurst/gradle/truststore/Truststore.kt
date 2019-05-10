package com.digithurst.gradle.truststore

import com.digithurst.gradle.truststore.TrustedCertificates.TrustedCertificate
import org.gradle.api.Action

import javax.annotation.MatchesPattern
import java.io.File

/**
 * Gradle extension that configures the trust store to use for running the
 * build script.
 *
 * By default, the JVM trust store with no additional trusted certificates
 * is used.
 *
 * @since 1.0.0
 */
class Truststore
/**
 * Creates a new instance that will resolve paths relative to the given ones.
 */
internal constructor(private val projectDir: File, private val javaHomePath: String) {
    /**
     * The trust store to be used as basis for the trust store instance being configured.
     *
     * @see empty
     * @see java
     * @see file
     */
    var base: Base = java("changeit")
    private val certificatesSpec: TrustedCertificates = TrustedCertificates()

    internal val certificates: List<TrustedCertificate>
        get() = certificatesSpec.certificates

    internal val isDefault: Boolean
        get() = base == java("changeit") && certificatesSpec.isEmpty

    /**
     * Add certificates to the [base] trust store.
     *
     * @param action A closure with receiver of type [TrustedCertificates].
     * @see TrustedCertificates.file
     * @since 1.0.0
     */
    @Suppress("unused") // Used in build.gradle.kts
    fun trustedCertificates(action: Action<in TrustedCertificates>) {
        TruststorePlugin.logger.debug("Starting collection of trusted certificates.")
        action.execute(certificatesSpec)
    }

    /**
     * Specifies an existing (or empty) trust store to use as basis for the
     * [Truststore] being configured.
     *
     * @since 1.0.0
     */
    data class Base internal constructor(
        internal val store: File?,
        @param:MatchesPattern(".{6,}") @get:MatchesPattern(".{6,}")
        internal val password: String
    )

    /**
     * Specifies that a new, empty trust store be created.
     *
     * @param password The password to be used for the new trust store.
     *      Has to be at least six characters long.
     * @return A representation of a new, empty trust store.
     *
     * @since 1.0.0
     */
    @Suppress("unused") // Used in build.gradle.kts
    fun empty(@MatchesPattern(".{6,}") password: String): Base =
        Base(null, password)

    /**
     * Specify that the (system) Java trust store be used.
     *
     * @param password The password the Java trust store is protected with.
     *      It will also be used for stores derived from this one.
     *      Has to be at least six characters long.
     * @return A representation of the Java trust store.
     *
     * @since 1.0.0
     */
    @Suppress("unused") // Used in build.gradle.kts
    fun java(@MatchesPattern(".{6,}") password: String): Base =
        Base(File("$javaHomePath/lib/security", "cacerts"), password)

    /**
     * Specify that a given trust store file be used.
     *
     * @param storeFileName Path to a trust store file, relative to the project root.
     * @param password The password the given trust store is protected with.
     *      It will also be used for stores derived from this one.
     *      Has to be at least six characters long.
     * @return A representation of trust store file.
     *
     * @since 1.0.0
     */
    @Suppress("unused") // Used in build.gradle.kts
    fun file(storeFileName: String, @MatchesPattern(".{6,}") password: String): Base =
        Base(File("${projectDir.absolutePath}/$storeFileName"), password)
        // TODO: handle absolute paths
}
