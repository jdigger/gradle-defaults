/*
 * Copyright 2014-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mooregreatsoftware.gradle.defaults.config

import com.mooregreatsoftware.gradle.defaults.DefaultsExtension
import com.mooregreatsoftware.gradle.defaults.isNotEmpty
import com.mooregreatsoftware.gradle.defaults.xml.appendChild
import com.mooregreatsoftware.gradle.defaults.xml.appendChildren
import com.mooregreatsoftware.gradle.defaults.xml.n
import groovy.util.Node
import org.gradle.api.Project
import org.gradle.api.XmlProvider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.internal.publication.MavenPomInternal
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin

class MavenPublishingConfig(project: Project, extension: DefaultsExtension) : AbstractConfigWithExtension(project, extension) {

    fun config() {
        plugins().apply(MavenPublishPlugin::class.java)

        // initialize the "main" publication if it hasn't already
        mainPublication(project)
    }

    companion object {
        val PUBLICATION_NAME = "main"


        fun mainPublication(project: Project): MavenPublication {
            val mavenPublishPlugin = project.plugins.findPlugin(MavenPublishPlugin::class.java)
            if (mavenPublishPlugin == null)
                project.plugins.apply(MavenPublishPlugin::class.java)

            val publishing = project.convention.getByType(PublishingExtension::class.java)

            val mainPub = publishing.publications.findByName(PUBLICATION_NAME) as MavenPublication? ?:
                createMainPublication(project)

            return mainPub
        }


        private fun createMainPublication(project: Project): MavenPublication {
            val publishing = project.convention.getByType(PublishingExtension::class.java)
            val pub = publishing.publications.create(PUBLICATION_NAME, MavenPublication::class.java)

            publishing.repositories.mavenLocal()

            project.afterEvaluate { prj -> groupid(project, pub.pom as MavenPomInternal) }

            configPom(project, pub)

            return pub
        }


        private fun configPom(project: Project, pub: MavenPublication) {
            pub.pom { pom -> pom.withXml { xmlProvider -> configPom(project, xmlProvider) } }
        }


        private fun configPom(project: Project, xmlProvider: XmlProvider) {
            val extension = defaultsExtension(project) ?: return

            with(xmlProvider.asNode()) {
                name(project)
                description(project)
                siteUrl(extension)
                organization(extension)
                licenses(extension)
                developers(extension)
                contributors(extension)
                scm(extension)
            }
        }


        private fun groupid(project: Project, pom: MavenPomInternal) {
            val projectIdentity = pom.projectIdentity
            if (!isNotEmpty(projectIdentity.groupId)) {
                if (!isNotEmpty(project.group.toString()))
                    throw IllegalStateException("There is no group set on the project")

                projectIdentity.groupId = project.group.toString()
            }
        }


        private fun Node.organization(extension: DefaultsExtension) {
            extension.orgName?.let {
                val children = mutableListOf(n("name", it))
                extension.orgUrl?.let { children.add(n("url", it)) }
                this.appendChildren("organization", children)
            }
        }


        private fun Node.siteUrl(extension: DefaultsExtension) {
            this.appendNode("url", extension.siteUrl)
        }


        private fun Node.description(project: Project) {
            if (isNotEmpty(project.description)) {
                this.appendNode("description", project.description)
            }
        }


        private fun Node.name(project: Project) {
            this.appendNode("name", project.name)
        }


        private fun Node.scm(extension: DefaultsExtension) {
            this.appendChildren("scm", listOf(
                n("connection", "scm:git:" + extension.vcsReadUrl),
                n("developerConnection", "scm:git:" + extension.vcsWriteUrl),
                n("url", extension.siteUrl)))
        }


        private fun Node.developers(extension: DefaultsExtension) {
            val developers = extension.getDevelopers()
            if (developers != null && !developers.isEmpty()) {
                this.appendChildren("developers", developers.map { developer ->
                    n("developer", listOf(
                        n("id", developer.id),
                        n("name", developer.name),
                        n("email", developer.email)))
                })
            }
        }


        private fun Node.contributors(extension: DefaultsExtension) {
            val contributors = extension.contributors
            if (contributors != null && !contributors.isEmpty()) {
                this.appendChildren("contributors", contributors.map { m ->
                    n("contributor", listOf(
                        // TODO ensure that the @NonNull is true...
                        n("name", m["name"] as String),
                        n("email", m["email"] as String)))
                })
            }
        }


        private fun Node.licenses(extension: DefaultsExtension) {
            this.appendChild("licenses",
                n("license", listOf(
                    n("name", extension.licenseName),
                    n("url", extension.licenseUrl))))
        }


        private fun defaultsExtension(project: Project?): DefaultsExtension? {
            if (project == null) return null
            return project.extensions.findByType(DefaultsExtension::class.java) ?: defaultsExtension(project.parent)
        }
    }

}
