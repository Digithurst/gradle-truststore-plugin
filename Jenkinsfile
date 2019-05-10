pipeline {
    agent any

    options {
        buildDiscarder(logRotator(
                artifactNumToKeepStr: '10',
                numToKeepStr: '100'
        ))
    }

    environment {
        BUILD_LABEL = buildLabel()
        BUILD_IS_RELEASE = "${isRelease()}"

        GPP_AUTH = credentials('gradle-plugin-portal-auth')
    }

    stages {
        stage('Prepare') {
            steps {
                sh script: './gradlew --no-daemon--version'
                sh script: "./gradlew --no-daemon clean"
            }
        }

        stage('Build') {
            steps {
                sh './gradlew --no-daemon assemble'
            }
        }

        // TODO: We probably should have _some_ tests and analyses. ^_^

        stage('Publish') {
            steps {
                // TODO: Is this necessary? We publish globally, after all.
                dir('build/libs') {
                    archiveArtifacts artifacts: "*.jar", fingerprint: true, onlyIfSuccessful: true
                }

                script {
                    if (isRelease()) {
                        sh script: ["./gradlew --no-daemon publishPlugins",
                                    "-Pgradle.publish.key=${env.GPP_AUTH_USR}",
                                    "-Pgradle.publish.secret=${env.GPP_AUTH_PSW}"].join(" ")
                    }

                    // TODO: Use GitHub API to create a release there
                }
            }
        }
    }

    post {
        success {
            script {
                if (isRelease()) {
                    persistAndDescribeBuild()
                }
            }
        }
    }
}

/**
 * Parses the (branch or) tag being built and extracts the version number.
 *
 * <em>Note:</em> If an actual branch is being built, this method will still
 * attempt to match. Use outside of a `when { tag ... }` guard at your own risk.
 *
 * @return
 *      The version number, if any; {@code null} if there is no (matching) tag.
 */
String versionFromTag() {
    String tag = env.BRANCH_NAME

    def matchRC = tag =~ /^v(\d+\.\d+\.\d+-rc\d*)$/
    if (matchRC.matches()) {
        return matchRC[0][1]
    }

    def matchRelease = tag =~ /^v(\d+\.\d+\.\d+)$/
    if (matchRelease.matches()) {
        return matchRelease[0][1]
    }

    return null
}

/**
 * @return a unique label for the current build (assuming branches and tags don't collide).
 */
String buildLabel() {
    return versionFromTag() ?: "${env.GIT_BRANCH}.${env.BUILD_ID}"
}

/**
 * @return {@code true} if (and only if) this build is for a release (candidate),
 *      as determined from the tag (or branch) name.
 */
Boolean isRelease() {
    return versionFromTag() != null
}

/**
 * Tells Jenkins to keep this build forever
 *
 * @param displayName
 *      A nicer name to be shown instead of the build number.
 *      If {@code null}, uses the version from the tag.
 *
 * @param description
 *      A longer description.
 *      If {@code null}, uses a reasonable default based on the tag.
 */
void persistAndDescribeBuild(String displayName = null, String description = null) {
    currentBuild.keepLog = true
    currentBuild.displayName = displayName ?: currentBuild.displayName

    if ( description != null ) {
        currentBuild.description = description
    } else {
        String version = versionFromTag()
        String candidate = version.contains("-rc") ? " Candidate" : ""
        version = version.replace("-rc", " RC ")
        currentBuild.description = "Release${candidate} ${version}"
    }
}