package com.novoda.gradle.release

import com.jfrog.bintray.gradle.BintrayPlugin
import com.novoda.gradle.release.internal.AndroidAttachments
import com.novoda.gradle.release.internal.JavaAttachments
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublicationContainer
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPomDeveloper
import org.gradle.api.publish.maven.MavenPomDeveloperSpec
import org.gradle.api.publish.maven.MavenPomLicense
import org.gradle.api.publish.maven.MavenPomLicenseSpec
import org.gradle.api.publish.maven.MavenPomScm
import org.gradle.api.publish.maven.MavenPublication

class ReleasePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        PublishExtension extension = project.extensions.create('publish', PublishExtension)
        project.afterEvaluate {
            extension.validate()
            attachArtifacts(extension, project)
            new BintrayConfiguration(extension).configure(project)
        }
        project.apply([plugin: 'maven-publish'])
        new BintrayPlugin().apply(project)
    }

    private static void attachArtifacts(PublishExtension extension, Project project) {
        project.plugins.withId('com.android.library') {
            project.android.libraryVariants.all { variant ->
                String publicationName = variant.name
                MavenPublication publication = createPublication(publicationName, project, extension)
                new AndroidAttachments(publicationName, project, variant).attachTo(publication)
            }
        }
        project.plugins.withId('java') {
            def mavenPublication = project.publishing.publications.find { it.name == 'maven' }
            if (mavenPublication == null) {
                String publicationName = 'maven'
                MavenPublication publication = createPublication(publicationName, project, extension)
                new JavaAttachments(publicationName, project).attachTo(publication)
            }
        }
    }

    private static MavenPublication createPublication(String publicationName, Project project, PublishExtension extension) {
        PropertyFinder propertyFinder = new PropertyFinder(project, extension)
        String groupId = extension.groupId
        String artifactId = extension.artifactId
        String version = propertyFinder.publishVersion

        PublicationContainer publicationContainer = project.extensions.getByType(PublishingExtension).publications
        return publicationContainer.create(publicationName, MavenPublication) { MavenPublication publication ->
            publication.groupId = groupId
            publication.artifactId = artifactId
            publication.version = version

            publication.pom { MavenPom pom ->
                if (extension.licences != null && extension.licences.length >= 1 && extension.licenceUrls != null && extension.licenceUrls.length >= 1 ) {
                    pom.licenses { MavenPomLicenseSpec pomLicenseSpec ->
                        // take only the 1st for now
                        pomLicenseSpec.license { MavenPomLicense pomLicense ->
                            pomLicense.name.set(extension.licences[0])
                            pomLicense.url.set(extension.licenceUrls[0])
                        }
                    }
                }

                if (!extension.devId.isEmpty() && !extension.devName.isEmpty() && !extension.devEmail.isEmpty()) {
                    pom.developers { MavenPomDeveloperSpec pomDeveloperSpec ->
                        // take only 1 for now
                        pomDeveloperSpec.developer { MavenPomDeveloper pomDeveloper ->
                            pomDeveloper.id.set(extension.devId)
                            pomDeveloper.name.set(extension.devName)
                            pomDeveloper.email.set(extension.devEmail)
                        }
                    }
                }

                if (!extension.scmUrl.isEmpty() && !extension.scmConnection.isEmpty() && !extension.scmDevConnection.isEmpty()) {
                    pom.scm { MavenPomScm pomScm ->
                        pomScm.connection.set(extension.scmConnection)
                        pomScm.developerConnection.set(extension.scmDevConnection)
                        pomScm.url.set(extension.scmUrl)
                    }
                }
            }

        } as MavenPublication
    }
}
