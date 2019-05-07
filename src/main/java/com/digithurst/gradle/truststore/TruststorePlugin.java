package com.digithurst.gradle.truststore;

import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.internal.impldep.com.google.common.io.Files;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.List;


public final class TruststorePlugin implements Plugin<Project> {
    private Logger logger = Logging.getLogger(TruststorePlugin.class);

    public File getCustomKeystore(Project project) {
        return new File(project.getGradle().getGradleUserHomeDir() + "/truststores", "cacerts");
    }


    @Override
    public void apply(Project project) {
        project.getExtensions().create("truststore", Truststore.class, project.getProjectDir());

        project.afterEvaluate(this::setupStore);
        logger.debug("Deferred setup of trust store");
    }

    private void setupStore(Project project) {
        final Truststore truststore = project.getExtensions().getByType(Truststore.class);

        final Truststore.Base storeBase = truststore.getBase();
        final List<TrustedCertificates.TrustedCertificate> certificates = truststore.getCertificates().getCertificates();

        if (storeBase == Truststore.DEFAULT_BASE && certificates.isEmpty()) {
            assert storeBase.getStore() != null;
            logger.debug("Using default trust store: " + storeBase.getStore().getAbsolutePath());
        } else if (truststore.getBase().getStore() != null && truststore.getCertificates().isEmpty()) {
            assert storeBase.getStore() != null;
            // TODO: Verify that store exists and is valid

            logger.debug("Using custom trust store: " + storeBase.getStore().getAbsolutePath());
            System.setProperty("javax.net.ssl.trustStore", storeBase.getStore().getAbsolutePath());
            System.setProperty("javax.net.ssl.trustStorePassword", storeBase.getPassword());

        } else {
            // TODO: verify that action as to be taken: do nothing if result is present and inputs haven't changed. (?)

            final File targetFile = getCustomKeystore(project);
//            try {
//                Files.createParentDirs(targetFile);
//            } catch ( IOException e ) {
//                logger.error("Could not create target directory for truststore", e);
//                return;
//            }

            // TODO: create/copy key store
            // TODO: add all certificates from truststore.getCertificates()
            throw new RuntimeException("Custom trust store with added certificates not yet implemented");
//            logger.error("Not yet implemented");
//
//            System.setProperty("javax.net.ssl.trustStore", targetFile.getAbsolutePath());
//            System.setProperty("javax.net.ssl.trustStorePassword", storeBase.getPassword());
//            logger.debug("Using aggregated custom trust store: " + targetFile.getAbsolutePath());
        }

        // TODO: Is this always effective? Setting those properties only does anything _before_ the first SSL connection...
        //      If not -- (how) can we inject a custom SSLContext into Gradle?
    }
}
