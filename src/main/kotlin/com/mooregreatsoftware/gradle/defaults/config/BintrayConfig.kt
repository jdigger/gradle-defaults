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
import com.mooregreatsoftware.gradle.defaults.DefaultsExtension
import org.gradle.api.GradleException
import org.gradle.api.Project
import java.io.File
import java.util.HashMap

@SuppressWarnings("WeakerAccess", "RedundantCast", "RedundantTypeArguments")
class BintrayConfig(project: Project, extension: DefaultsExtension) : AbstractConfigWithExtension(project, extension) {


    fun config() {
        plugins().withType(BintrayPlugin::class.java) { plugin ->
            if (project.hasProperty("bintrayUser") && project.hasProperty("bintrayKey")) {
                project.afterEvaluate { prj -> configBintray() }
            }
        }
    }


    private fun configBintray() {
        val bintray = bintrayExtension(project)

        bintray.user = project.property("bintrayUser") as String
        bintray.key = project.property("bintrayKey") as String

        bintray.setPublications("main")
        bintray.isPublish = true

        val pkgConfig = bintray.pkg

        if (extension.orgName != null) pkgConfig.userOrg = extension.id

        pkgConfig.setPackageRepo()
        pkgConfig.setPackageName()

        project.description?.let { pkgConfig.desc = it }
        pkgConfig.websiteUrl = extension.siteUrl
        pkgConfig.issueTrackerUrl = extension.issuesUrl
        pkgConfig.vcsUrl = extension.vcsReadUrl
        pkgConfig.setLicenses(extension.licenseKey)

        val bintrayLabels = toStringArray(extension.bintrayLabels)
        pkgConfig.setLabels(*bintrayLabels)
        pkgConfig.isPublicDownloadNumbers = true

        val bintrayPkgVersion = pkgConfig.version

        bintrayPkgVersion.vcsTag = "v" + project.version
        bintrayPkgVersion.attributes = bintrayAttributes()

        val gpg = bintrayPkgVersion.gpg
        gpg.sign = true
        if (project.hasProperty("gpgPassphrase")) {
            gpg.passphrase = project.property("gpgPassphrase") as String
        }
    }

    private fun BintrayExtension.PackageConfig.setPackageRepo() {
        if (this.repo.isNullOrBlank()) {
            if (extension.bintrayRepo.isNullOrBlank())
                throw GradleException("Need to set defaults { bintrayRepo = ... }")
            this.repo = extension.bintrayRepo
        }
        else {
            if (!extension.bintrayRepo.isNullOrBlank())
                throw GradleException("Both defaults { bintrayRepo = ... } and bintray { pkg { repo = ... } } have been set.")
            // repo has already been set
        }
    }

    private fun BintrayExtension.PackageConfig.setPackageName() {
        if (this.name.isNullOrBlank()) {
            this.name = when {
                extension.bintrayPkg.isNullOrBlank() -> project.name
                else -> extension.bintrayPkg
            }
        }
        else {
            if (!extension.bintrayPkg.isNullOrBlank())
                throw GradleException("Both defaults { bintrayPkg = ... } and bintray { pkg { name = ... } } have been set.")
            // name has already been set
        }
    }


    private fun bintrayAttributes(): Map<String, Any> {
        val bintrayAttributes = HashMap<String, Any>()
        if (extension.bintrayLabels?.contains("gradle") ?: false) {
            info("bintrayLabels does includes 'gradle' so generating 'gradle-plugins' attribute")
            val filesTree = gradlePluginPropertyFiles(project)
            val pluginIdBintrayAttributeValues = filesToPluginIds(filesTree).map { this.pluginIdToBintrayAttributeValue(it) }.toList()
            bintrayAttributes.put("gradle-plugins", pluginIdBintrayAttributeValues)
        }
        else {
            info("bintrayLabels does not include 'gradle' so not generating 'gradle-plugins' attribute")
        }

        info("bintrayAttributes: " + bintrayAttributes)
        return bintrayAttributes
    }


    private fun pluginIdToBintrayAttributeValue(pluginId: String) = pluginId + ":" + project.group + ":" + name()
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


fun bintrayExtension(project: Project): BintrayExtension = project.convention.getByType(BintrayExtension::class.java)
