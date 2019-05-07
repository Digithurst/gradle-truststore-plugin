package com.digithurst.gradle.truststore;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TrustedCertificates {
    private List<TrustedCertificate> certificates = new ArrayList<>();

    public void file(@Nonnull String fileName, @Nonnull String alias) {
        certificates.add(new TrustedCertificate(fileName, alias));
    }

    public List<TrustedCertificate> getCertificates() {
        return Collections.unmodifiableList(certificates);
    }

    boolean isEmpty() {
        return certificates.isEmpty();
    }

    protected static final class TrustedCertificate {
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
