package com.digithurst.gradle.truststore;

import org.gradle.api.Action;
import org.gradle.api.GradleScriptException;

import javax.annotation.MatchesPattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;

public class Truststore {
    final static Base DEFAULT_BASE = makeJavaBase("changeit");

    private final File projectDir;

    private Base base = DEFAULT_BASE;
    private final TrustedCertificates certificates = new TrustedCertificates();

    public Truststore(File projectDir) {
        this.projectDir = projectDir;
    }

    public Base getBase() {
        return base;
    }

    public void setBase(Base base) {
        this.base = base;
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
        return makeJavaBase(password);
    }

    @Nonnull
    public final Base file(@Nonnull String storeFileName, @Nonnull @MatchesPattern(".{6,}") String password) {
        // TODO: handle absolute paths
        return new Base(new File(projectDir.getAbsolutePath() + "/" + storeFileName), password);
    }

    @Nonnull
    private static Base makeJavaBase(@Nonnull @MatchesPattern(".{6,}") String password) {
        if (System.getenv("JAVA_HOME") == null) {
            throw new GradleScriptException("Referring to default Java key store, but JAVA_HOME not set.", null);
        }

        return new Base(new File(System.getenv("JAVA_HOME") + "/lib/security", "cacerts"), password);
    }
}
