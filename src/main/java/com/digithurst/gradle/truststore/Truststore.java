package com.digithurst.gradle.truststore;

import org.gradle.api.Action;
import org.gradle.api.GradleScriptException;

import javax.annotation.MatchesPattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;

public class Truststore {
    private static Base defaultBase = null;

    private final File projectDir;
    private final String javaHomePath;

    private Base base;
    private final TrustedCertificates certificates;

    public Truststore(@Nonnull File projectDir, @Nonnull String javaHomePath) {
        this.projectDir = projectDir;
        this.javaHomePath = javaHomePath;

        base = getDefaultBase();
        certificates = new TrustedCertificates();
    }

    public Base getBase() {
        return base;
    }

    public void setBase(Base base) {
        this.base = base;
    }

    final Base getDefaultBase() {
        if (defaultBase == null) {
            defaultBase = java("changeit");
        }

        return defaultBase;
    }

    public TrustedCertificates getCertificates() {
        return certificates;
    }

    public void trustedCertificates(Action<? super TrustedCertificates> action) {
        action.execute(certificates);
    }

    public final static class Base {
        private File store;
        private String password;

        Base(@Nullable File store, @Nonnull @MatchesPattern(".{6,}") String password) {
            this.store = store;
            this.password = password;
        }

        @Nullable
        File getStore() {
            return store;
        }

        @Nonnull @MatchesPattern(".{6,}")
        String getPassword() {
            return password;
        }
    }

    @Nonnull
    public final Base empty(@Nonnull @MatchesPattern(".{6,}") String password) {
        return new Base(null, password);
    }

    @Nonnull
    public final Base java(@Nonnull @MatchesPattern(".{6,}") String password) {
        return new Base(new File(javaHomePath + "/lib/security", "cacerts"), password);
    }

    @Nonnull
    public final Base file(@Nonnull String storeFileName, @Nonnull @MatchesPattern(".{6,}") String password) {
        // TODO: handle absolute paths
        return new Base(new File(projectDir.getAbsolutePath() + "/" + storeFileName), password);
    }
}
