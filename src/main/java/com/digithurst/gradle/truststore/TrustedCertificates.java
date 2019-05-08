package com.digithurst.gradle.truststore;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Specifies a set of certificates to add to the base trust store to create the
 * {@link Truststore} being configured.
 *
 * @since 1.0.0
 */
@SuppressWarnings("WeakerAccess") // Used in build.gradle.kts
public class TrustedCertificates {
    private List<TrustedCertificate> certificates = new ArrayList<>();

    /**
     * <p>
     * Add a trusted certificate to the {@link Truststore#setBase(Truststore.Base) base trust store}
     * that will be used for this build.
     * </p>
     * <p><strong>Note:</strong> This will cause the given certificate to be trusted implicitly
     * without further checks. Ensure that the certificate is authentic before using it!</p>
     *
     * @param fileName The certificate file path; absolute or relative to the project root.
     * @param alias A (unique) alias for the given certificate (e.g. the associated host name);
     *              cf. {@link java.security.KeyStore#setCertificateEntry KeyStore.setCertificateEntry}.
     *
     * @since 1.0.0
     */
    @SuppressWarnings({"unused", "WeakerAccess"}) // Used in build.gradle.kts
    public void file(@Nonnull String fileName, @Nonnull String alias) {
        certificates.add(new TrustedCertificate(fileName, alias));
    }

    List<TrustedCertificate> getCertificates() {
        return Collections.unmodifiableList(certificates);
    }

    boolean isEmpty() {
        return certificates.isEmpty();
    }

    static final class TrustedCertificate {
        private final String fileName;
        private final String alias;

        TrustedCertificate(@Nonnull String fileName, @Nonnull String alias) {
            this.fileName = fileName;
            this.alias = alias;
        }

        String getFileName() {
            return fileName;
        }

        String getAlias() {
            return alias;
        }
    }
}
