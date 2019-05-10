# Custom Trust Stores for Gradle Builds

[
  ![Gradle Plugin Portal](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/com/digithurst/gradle/com.digithurst.gradle.truststore/maven-metadata.xml.svg?label=gradle%20plugin)
](https://plugins.gradle.org/plugin/com.digithurst.gradle.truststore)

Configure a custom trust store for Gradle to use during builds.
For example, this allows Gradle to pull dependencies from a Maven
repository that uses a self-signed certificate.

## Usage

Add this to your `build.gradle.kts` file:

```kotlin
plugins {
    id("com.digithurst.gradle.truststore") version "1.1.0"
}

truststore {
    base = empty("your-secure-password") // XOR
    base = file("your-truststore", password = "your-secure-password") // XOR
    base = java("your-secure-password")
    // default: 
    // base = java("changeit")
    
    trustedCertificates {
        file("your-certificate.crt", alias = "your.host")
    }
    // default: no addition certificates
}
```

Or, if you prefer, your `build.gradle` file:

```groovy
plugins {
    id 'com.digithurst.gradle.truststore' version '1.1.0'
}

truststore {
    base = java("changeit")
    
    trustedCertificates {
        it.file("your-certificate.crt", "your.host")
    }
}

```

_Note:_ 

 * If `base = java(...)` is used, the plugin will look for trust store 
   `$JAVA_HOME/lib/security/cacerts`. Provide the corresponding password.
 * In case of `file` and `java`, the original key stores are never changed.


## Additional Hints

 * If you have a PEM certificate instead of a CRT, convert it like so:
 
    ```bash
    openssl x509 -in your-certificate.pem  -inform PEM -out your-certificate.crt
    ```
    
## Limitations

<!-- TODO: investigate -->

 * Since the modified trust store is assembled _after_ processing of
   the build script, it won't be available for pulling _plugins_ via 
   HTTPS. In such a case, you will have to create your store manually
   using `keytool` (or pick the result of this plugin up in 
   `build/truststores`), and point Gradle towards it manually, 
   e.g. like so:
   
   ```groove
   buildscript {
       System.setProperty('javax.net.ssl.trustStore', 'your-truststore')
       System.setProperty('javax.net.ssl.trustStorePassword', 'your-secure-password')
   }
   ```
 * Changes in the trust store configuration are not picked up by running
   Gradle daemons. Stop all daemons with `grade --stop` after making
   changes, or use `--no-daemon` in the first place (until the configuration
   has converged).
 * The plugin may not work if any of the other plugins performs an SSL connection
   during build script evaluation.

## References

 * https://www.baeldung.com/java-truststore
 * https://plugins.gradle.org/plugin/de.chkpnt.truststorebuilder  
   Builds a trust store, but doesn't configure Gradle itself to use it.