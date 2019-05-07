package com.digithurst.gradle.truststore;

import com.digithurst.gradle.truststore.TrustedCertificates.TrustedCertificate;
import org.gradle.api.GradleScriptException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.List;


public final class TruststorePlugin implements Plugin<Project> {
    /**
     * Possible key store types, as per the
     * <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#KeyStore">
     *     Java 8 documentation.
     * </a>
     */
    private static final String[] keyStoreTypes = new String[] { /*"jceks",*/ "jks", "dks", "pkcs11", "pkcs12" };
    /* Note: `jceks` results in
     *           `org.apache.http.ssl.SSLInitializationException: DerInputStream.getLength(): lengthTag=78, too big`
     *        so we leave it out. */

    /**
     * Possible certificate factory types, as per the
     * <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#CertificateFactory">
     *      Java 8 documentation.
     * </a>
     */
    private static final String[] certificateFactoryTypes = new String[] { "X.509" };

    private Logger logger = Logging.getLogger(TruststorePlugin.class);

    public File getCustomKeystore(Project project) {
        return new File(project.getBuildDir() + "/truststores", "cacerts");
    }


    @Override
    public void apply(Project project) {
        @SuppressWarnings("ConstantConditions") // property is always specified: https://docs.gradle.org/current/userguide/build_environment.html
        String javaHome = project.property("org.gradle.java.home").toString();
        logger.debug("Found Java home directory: " + javaHome);

        project.getExtensions().create("truststore", Truststore.class, project.getProjectDir(), javaHome);

        project.property("org.gradle.java.home");
        project.afterEvaluate(this::setupStore);
        logger.debug("Deferred setup of trust store");
    }

    private void setupStore(Project project) {
        final Truststore truststore = project.getExtensions().getByType(Truststore.class);

        final Truststore.Base storeBase = truststore.getBase();
        final List<TrustedCertificate> certificates = truststore.getCertificates().getCertificates();

        if (storeBase == truststore.getDefaultBase() && certificates.isEmpty()) {
            assert storeBase.getStore() != null;
            logger.debug("Using default trust store: " + storeBase.getStore().getAbsolutePath());
        } else if (truststore.getBase().getStore() != null && truststore.getCertificates().isEmpty()) {
            assert storeBase.getStore() != null;
            // TODO: Verify that store exists and is valid

            logger.debug("Using custom trust store: " + storeBase.getStore().getAbsolutePath());
            System.setProperty("javax.net.ssl.trustStore", storeBase.getStore().getAbsolutePath());
            System.setProperty("javax.net.ssl.trustStorePassword", storeBase.getPassword());

        } else {
            // TODO: verify that action as to be taken: do nothing if result is present and inputs haven't changed.

            final File targetFile = getCustomKeystore(project);
            try {
                @Nonnull
                final KeyStore ks;

                // Load base store, if any.
                if ( storeBase.getStore() != null ) {
                    logger.debug("Importing key store " + storeBase.getStore().getAbsoluteFile());

                    // Apparently, there's no way to determine the type of the key store at hand,
                    // so we try one after the other.
                    // TODO: Refactor to stream-based approach
                    KeyStore ksFound = null;
                    for ( final String ksType : keyStoreTypes ) {
                        try ( FileInputStream storeIn = new FileInputStream(storeBase.getStore().getAbsoluteFile()) ) {
                            ksFound = KeyStore.getInstance(ksType);
                            ksFound.load(storeIn, storeBase.getPassword().toCharArray());
                            break;
                        } catch (KeyStoreException|NoSuchAlgorithmException e) {
                            logger.debug("Key store is not of type " + ksType);
                        }
                    }

                    if (ksFound == null) {
                        throw new KeyStoreException("No provider is compatible with " + storeBase.getStore().getAbsoluteFile());
                    }

                    logger.debug("Imported key store of type " + ksFound.getType());
                    ks = ksFound;
                } else {
                    ks = KeyStore.getInstance(KeyStore.getDefaultType());
                    ks.load(null, storeBase.getPassword().toCharArray());
                }

                // Add custom certificates
                //noinspection ConstantConditions // protects against regressions
                assert certificateFactoryTypes.length == 1 :
                        "Need to refactor to account for multiple possible certificate types";
                final CertificateFactory cf = CertificateFactory.getInstance(certificateFactoryTypes[0]);
                for ( final TrustedCertificate cert : certificates) {
                    logger.debug("Importing certificate " + cert.getFileName());
                    try (FileInputStream certIn = new FileInputStream(cert.getFileName())) {
                        ks.setCertificateEntry(cert.getAlias(), cf.generateCertificate(certIn));
                    }
                }

                // Write result
                logger.debug("Writing custom trust store to " + targetFile.getAbsolutePath());
                Files.createDirectories(targetFile.toPath().getParent());
                try ( FileOutputStream storeOut = new FileOutputStream(targetFile) ) {
                    ks.store(storeOut, storeBase.getPassword().toCharArray());
                }
            } catch ( CertificateException | KeyStoreException | IOException | NoSuchAlgorithmException e ) {
                logger.error("Could not assemble trust store", e);
                throw new GradleScriptException("Could not assemble trust store", e);
            }

            logger.debug("Using aggregated custom trust store: " + targetFile.getAbsolutePath());
            System.setProperty("javax.net.ssl.trustStore", targetFile.getAbsolutePath());
            System.setProperty("javax.net.ssl.trustStorePassword", storeBase.getPassword());
        }

        // TODO: Is this always effective? Setting those properties only does anything _before_ the first SSL connection...
        //      If not -- (how) can we inject a custom SSLContext into Gradle?
    }
}
