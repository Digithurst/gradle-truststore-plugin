package com.digithurst.gradle.truststore

import java.util.ArrayList
import java.util.Collections

/**
 * Specifies a set of certificates to add to the base trust store to create the
 * [Truststore] being configured.
 *
 * @since 1.0.0
 */
class TrustedCertificates {
    private val certificatesSpec = ArrayList<TrustedCertificate>()

    internal val certificates: List<TrustedCertificate>
        get() = Collections.unmodifiableList(certificatesSpec)

    internal val isEmpty: Boolean
        get() = certificatesSpec.isEmpty()

    /**
     * Add a trusted certificate to the [base trust store][Truststore.base]
     * that will be used for this build.
     *
     * **Note:** This will cause the given certificate to be trusted implicitly
     * without further checks. Ensure that the certificate is authentic before using it!
     *
     * @param path The certificate file path; absolute or relative to the project root.
     * @param alias A (unique) alias for the given certificate (e.g. the associated host name);
     *      cf. [KeyStore.setCertificateEntry][java.security.KeyStore.setCertificateEntry].
     *
     * @since 1.0.0
     */
    fun file(path: String, alias: String) {
        certificatesSpec.add(TrustedCertificate(path, alias))
        TruststorePlugin.logger.debug("Added trusted certificate $path as $alias")
    }

    internal data class TrustedCertificate(val path: String, val alias: String)
}
