package com.digithurst.gradle.truststore;

import org.gradle.api.Action;

import javax.annotation.MatchesPattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;

/**
 * <p>
 *     Gradle extension that configures the Trust Store to use for running the
 *     build script.
 * </p>
 * <p>
 *     By default, the Java trust store with no additional trusted certificates
 *     is used.
 * </p>
 *
 * @since 1.0.0
 */
public class Truststore {
    /**
     * The default Java trust store.
     *
     * Keeping a static instance so that the check "is the default set?"
     * is an easy instance-equality check.
     *
     * Filled lazily by {@link #getDefaultBase()} with {@code java("changeit")}.
     */
    private static Base defaultBase = null;

    @Nonnull
    private final File projectDir;
    @Nonnull
    private final String javaHomePath;

    @Nonnull
    private Base base;
    @Nonnull
    private final TrustedCertificates certificates;


    /**
     * Creates a new instance that will resolve paths relative to the given ones.
     */
    Truststore(@Nonnull File projectDir, @Nonnull String javaHomePath) {
        this.projectDir = projectDir;
        this.javaHomePath = javaHomePath;

        base = getDefaultBase();
        certificates = new TrustedCertificates();
    }

    @Nonnull
    @SuppressWarnings({"WeakerAccess", "JavaDoc"}) // Should be internal, but Kotlin DSL fails otherwise
    public Base getBase() {
        return base;
    }

    /**
     * Specify the trust store to be used as basis for the
     * trust store instance being configured.
     *
     * @param base The new store to start with.
     * @see #empty(String)
     * @see #java(String)
     * @see #file(String, String)
     */
    @SuppressWarnings({"unused", "WeakerAccess"}) // Used in build.gradle.kts
    public void setBase(@Nonnull Base base) {
        this.base = base;
    }

    @Nonnull
    final Base getDefaultBase() {
        if (defaultBase == null) {
            defaultBase = java("changeit");
        }

        return defaultBase;
    }

    @Nonnull
    TrustedCertificates getCertificates() {
        return certificates;
    }

    /**
     * Add certificates to the {@link #setBase(Base) base} trust store.
     *
     * @param action A closure with receiver of type {@link TrustedCertificates}.
     * @see TrustedCertificates#file(String, String) file(String, String)
     *
     * @since 1.0.0
     */
    @SuppressWarnings("unused") // Used in build.gradle.kts
    public void trustedCertificates(Action<? super TrustedCertificates> action) {
        action.execute(certificates);
    }

    /**
     * Specifies an existing (or empty) trust store to use as basis for the
     * {@link Truststore} being configured.
     *
     * @since 1.0.0
     */
    @SuppressWarnings("WeakerAccess") // Used in build.gradle.kts
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

    /**
     * Specifies that a new, empty trust store be created.
     *
     * @param password The password to be used for the new trust store.
     *                 Has to be at least six characters long.
     * @return A representation of a new, empty trust store.
     *
     * @since 1.0.0
     */
    @Nonnull
    @SuppressWarnings({"unused", "WeakerAccess"}) // Used in build.gradle.kts
    public final Base empty(@Nonnull @MatchesPattern(".{6,}") String password) {
        return new Base(null, password);
    }

    /**
     * Specify that the (system) Java trust store be used.
     *
     * @param password The password the Java trust store is protected with.
     *                 It will also be used for stores derived from this one.
     *                 Has to be at least six characters long.
     * @return A representation of the Java trust store.
     *
     * @since 1.0.0
     */
    @Nonnull
    @SuppressWarnings({"unused", "WeakerAccess"}) // Used in build.gradle.kts
    public final Base java(@Nonnull @MatchesPattern(".{6,}") String password) {
        return new Base(new File(javaHomePath + "/lib/security", "cacerts"), password);
    }

    /**
     * Specify that a given trust store file be used.
     *
     * @param storeFileName Path to a trust store file, relative to the project root.
     * @param password The password the given trust store is protected with.
     *                 It will also be used for stores derived from this one.
     *                 Has to be at least six characters long.
     * @return A representation of trust store file.
     *
     * @since 1.0.0
     */
    @Nonnull
    @SuppressWarnings({"unused", "WeakerAccess"}) // Used in build.gradle.kts
    public final Base file(@Nonnull String storeFileName, @Nonnull @MatchesPattern(".{6,}") String password) {
        // TODO: handle absolute paths
        return new Base(new File(projectDir.getAbsolutePath() + "/" + storeFileName), password);
    }
}
