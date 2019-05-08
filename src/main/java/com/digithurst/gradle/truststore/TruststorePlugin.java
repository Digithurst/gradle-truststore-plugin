package com.digithurst.gradle.truststore;

import com.digithurst.gradle.truststore.TrustedCertificates.TrustedCertificate;
import org.gradle.api.GradleScriptException;
import org.gradle.api.InvalidUserCodeException;
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
import java.util.Objects;
import java.util.stream.Stream;


/**
 * Configure a custom trust store for Gradle to use during builds.
 * For example, this allows Gradle to pull dependencies from a Maven
 * repository that uses a self-signed certificate.
 *
 * @see Truststore
 *
 * @since 1.0.0
 */
@SuppressWarnings("unused") // accessed by Gradle triggered by build.gradle.kts
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

    // TODO: Make configurable?
    private File getCustomKeystore(Project project) {
        return new File(project.getBuildDir() + "/truststores", "cacerts");
    }


    @Override
    public void apply(Project project) {
        String javaHome = System.getProperty("java.home");
        logger.debug("Found Java home directory: " + javaHome);

        Truststore extension = new Truststore(project.getProjectDir(), javaHome);
        project.getExtensions().add(Truststore.class, "truststore", extension);

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
            if (!storeBase.getStore().isFile()) {
                throw new InvalidUserCodeException("Key store file does not exist: "
                        + storeBase.getStore().getAbsolutePath());
            }
            // TODO: Verify that store is valid?

            logger.debug("Using custom trust store: " + storeBase.getStore().getAbsolutePath());
            System.setProperty("javax.net.ssl.trustStore", storeBase.getStore().getAbsolutePath());
            System.setProperty("javax.net.ssl.trustStorePassword", storeBase.getPassword());
        } else {
            // TODO: verify that action has to be taken: do nothing if result is present and inputs haven't changed.
            //       Investigate whether that's worth is: according to log timestamps, the actual import only takes
            //       a few hundreds of a second, any form of reasonable check may take about as long.

            final File targetFile = getCustomKeystore(project);
            try {
                @Nonnull
                final KeyStore ks;

                // Load base store, if any.
                if ( storeBase.getStore() != null ) {
                    logger.debug("Importing key store " + storeBase.getStore().getAbsoluteFile());

                    // Apparently, there's no way to determine the type of the key store at hand,
                    // so we try one after the other.
                    ks = Stream.of(keyStoreTypes)
                            .map(ksType -> {
                                try ( FileInputStream storeIn = new FileInputStream(storeBase.getStore().getAbsoluteFile()) ) {
                                    KeyStore store = KeyStore.getInstance(ksType);
                                    store.load(storeIn, storeBase.getPassword().toCharArray());
                                    return store;
                                } catch (KeyStoreException|NoSuchAlgorithmException e) {
                                    logger.debug("Key store is not of type " + ksType);
                                    return null;
                                } catch (IOException|CertificateException e) {
                                    logger.error("Loading key store failed", e);
                                    return null;
                                }
                            })
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElseThrow(() ->
                                    new KeyStoreException("No provider could load "
                                            + storeBase.getStore().getAbsoluteFile()));

                    logger.debug("Imported key store of type " + ks.getType());
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
