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

import com.google.common.io.Files
import com.jfrog.bintray.gradle.BintrayExtension
import com.jfrog.bintray.gradle.BintrayPlugin
import com.mooregreatsoftware.gradle.defaults.ReadableDefaultsExtension
import com.mooregreatsoftware.gradle.defaults.Ternary
import com.mooregreatsoftware.gradle.defaults.postEvalCreate
import com.mooregreatsoftware.gradle.defaults.readableDefaultsExtension
import org.gradle.api.GradleException
import org.gradle.api.Project
import java.io.File
import java.util.HashMap
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class BintrayConfig private constructor(private val project: Project, readExtensionFuture: Future<ReadableDefaultsExtension>) {

    init {
        project.postEvalCreate {
            val readExtension = readExtensionFuture.get(20, TimeUnit.SECONDS)
            val bintray = project.bintrayExtension()!!

            project.gradle.taskGraph.addTaskExecutionGraphListener {
                if (it.hasTask("bintrayUpload")) {
                    if (project.hasProperty("bintrayUser") && project.hasProperty("bintrayKey")) {
                        bintray.user = project.property("bintrayUser") as String
                        bintray.key = project.property("bintrayKey") as String
                    }
                    else {
                        throw GradleException("You need to set the \"bintrayUser\" and \"bintrayKey\" properties on the project to upload to BinTray")
                    }
                }
            }

            bintray.setPublications("main")
            bintray.isPublish = true

            val pkgConfig = bintray.pkg

            if (readExtension.orgName != null) pkgConfig.userOrg = readExtension.orgId

            pkgConfig.setPackageRepo(readExtension)
            pkgConfig.setPackageName(readExtension)

            project.description?.let { pkgConfig.desc = it }
            pkgConfig.websiteUrl = readExtension.siteUrl
            pkgConfig.issueTrackerUrl = readExtension.issuesUrl
            pkgConfig.vcsUrl = readExtension.vcsReadUrl
            pkgConfig.setLicenses(readExtension.licenseKey)

            val bintrayLabels = toStringArray(readExtension.bintrayLabels)
            pkgConfig.setLabels(*bintrayLabels)
            pkgConfig.isPublicDownloadNumbers = true

            val bintrayPkgVersion = pkgConfig.version

            bintrayPkgVersion.vcsTag = "v" + project.version
            bintrayPkgVersion.attributes = bintrayAttributes(readExtension)

            val gpg = bintrayPkgVersion.gpg
            if (project.hasProperty("gpgPassphrase")) {
                gpg.sign = true
                gpg.passphrase = project.property("gpgPassphrase") as String
            }
            else {
                project.logger.info("\"gpgPassphrase\" not set on the project, so not signing the Bintray upload")
            }
        }
    }

    private fun BintrayExtension.PackageConfig.setPackageRepo(readExtension: ReadableDefaultsExtension) {
        if (this.repo.isNullOrBlank()) {
            if (readExtension.bintrayRepo.isNullOrBlank())
                throw GradleException("Need to set defaults { bintrayRepo = ... }")
            this.repo = readExtension.bintrayRepo
        }
        else {
            if (!readExtension.bintrayRepo.isNullOrBlank())
                throw GradleException("Both defaults { bintrayRepo = ... } and bintray { pkg { repo = ... } } have been set.")
            // repo has already been set
        }
    }

    private fun BintrayExtension.PackageConfig.setPackageName(readExtension: ReadableDefaultsExtension) {
        if (this.name.isNullOrBlank()) {
            this.name = when {
                readExtension.bintrayPkg.isNullOrBlank() -> project.name
                else -> readExtension.bintrayPkg
            }
        }
        else {
            if (!readExtension.bintrayPkg.isNullOrBlank())
                throw GradleException("Both defaults { bintrayPkg = ... } and bintray { pkg { name = ... } } have been set.")
            // name has already been set
        }
    }


    private fun bintrayAttributes(readExtension: ReadableDefaultsExtension): Map<String, Any> {
        val bintrayAttributes = HashMap<String, Any>()
        if (readExtension.bintrayLabels?.contains("gradle") ?: false) {
            project.logger.info("bintrayLabels does includes 'gradle' so generating 'gradle-plugins' attribute")
            val filesTree = gradlePluginPropertyFiles(project)
            val pluginIdBintrayAttributeValues = filesToPluginIds(filesTree).map { this.pluginIdToBintrayAttributeValue(it) }.toList()
            bintrayAttributes.put("gradle-plugins", pluginIdBintrayAttributeValues)
        }
        else {
            project.logger.info("bintrayLabels does not include 'gradle' so not generating 'gradle-plugins' attribute")
        }

        project.logger.info("bintrayAttributes: $bintrayAttributes")
        return bintrayAttributes
    }


    private fun pluginIdToBintrayAttributeValue(pluginId: String) = "$pluginId:${project.group}:${project.name}"

    companion object {

        private fun create(project: Project): Future<BintrayConfig?> {
            return confFuture(project, "BinTray",
                { if (project.plugins.hasPlugin(BintrayPlugin::class.java)) Ternary.TRUE else Ternary.MAYBE },
                { project.plugins.hasPlugin(BintrayPlugin::class.java) },
                { BintrayConfig(project, project.readableDefaultsExtension()) },
                null
            )
        }

        /**
         * Returns the BintrayConfig for in the given Project.
         *
         * @param project the project containing the BintrayConfig
         */
        @JvmStatic fun of(project: Project): Future<BintrayConfig?> {
            return ofFuture(project, { create(project) })
        }

    }
}

private fun toStringArray(labels: Set<String>?): Array<String> = when (labels) {
    null -> emptyArray()
    else -> labels.toTypedArray()
}


private fun filesToPluginIds(filesTree: Iterable<File>) = filesTree.map { filenameNoExtension(it) }


private fun filenameNoExtension(file: File) = Files.getNameWithoutExtension(file.name)


private fun gradlePluginPropertyFiles(project: Project): Iterable<File> = project.fileTree(
    mapOf("dir" to "src/main/resources/META-INF/gradle-plugins", "include" to "*.properties")
)


fun Project.bintrayExtension(): BintrayExtension? = this.convention.findByType(BintrayExtension::class.java)
