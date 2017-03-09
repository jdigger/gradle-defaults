/*
 * Copyright 2014-2017 the original author or authors.
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
package com.mooregreatsoftware.gradle.defaults

import com.mooregreatsoftware.gradle.bintray.ExtBintrayPlugin.BINTRAY_LABELS_KEY
import com.mooregreatsoftware.gradle.bintray.ExtBintrayPlugin.BINTRAY_PKG_KEY
import com.mooregreatsoftware.gradle.bintray.ExtBintrayPlugin.BINTRAY_REPO_KEY
import com.mooregreatsoftware.gradle.bintray.ExtBintrayPlugin.ISSUES_URL_KEY
import com.mooregreatsoftware.gradle.bintray.ExtBintrayPlugin.LICENSE_KEY
import com.mooregreatsoftware.gradle.bintray.ExtBintrayPlugin.ORG_ID_KEY
import com.mooregreatsoftware.gradle.bintray.ExtBintrayPlugin.SITE_URL_KEY
import com.mooregreatsoftware.gradle.bintray.ExtBintrayPlugin.VCS_READ_URL_KEY
import com.mooregreatsoftware.gradle.checkerframework.CheckerFrameworkExtension
import com.mooregreatsoftware.gradle.checkerframework.CheckerFrameworkPlugin.checkerFrameworkExtension
import com.mooregreatsoftware.gradle.license.ExtLicenseExtension
import com.mooregreatsoftware.gradle.license.ExtLicensePlugin.licenseExtension
import com.mooregreatsoftware.gradle.lombok.LombokExtension
import com.mooregreatsoftware.gradle.lombok.LombokPlugin.lombokExtension
import com.mooregreatsoftware.gradle.util.getCustomProperty
import com.mooregreatsoftware.gradle.util.hasCustomProperty
import com.mooregreatsoftware.gradle.util.isRootProject
import com.mooregreatsoftware.gradle.util.setCustomProperty
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Configuration for [DefaultsPlugin].
 *
 * @see [defaultsExtension]
 */
@Suppress("unused", "ConvertLambdaToReference")
open class DefaultsExtension(override val project: Project, override val license: ExtLicenseExtension, override val lombok: LombokExtension, override val checkerFramework: CheckerFrameworkExtension) : ReadableDefaultsExtension {

    override var orgId: String
        get() = customProp(ORG_ID_KEY, {
            LOG.debug("\"${DefaultsExtension.NAME}\" on ${project.name} - ${this.inspect()}")
            throw IllegalStateException("\"orgId\" is not set for \"${DefaultsExtension.NAME}\" on ${project.name}")
        })!!
        set(value) {
            project.setCustomProperty(ORG_ID_KEY, value)
        }

    fun setId(value: String) {
        LOG.warn("\"id\" property of \"${DefaultsExtension.NAME}\" is deprecated")
        orgId = value
    }

    override var openSource: Boolean
        get() = customProp("com.mooregreatsoftware.property.isOpenSource", {
            LOG.warn("\"${DefaultsExtension.NAME}.openSource\" is not set on ${project.rootProject.name}: assuming \"true\"")
            true
        })!!
        set(value) {
            project.setCustomProperty("com.mooregreatsoftware.property.bintray.isOpenSource", value)
        }

    override var orgName: String?
        get() = customProp("com.mooregreatsoftware.property.orgName", NULL_SUPPLIER)
        set(value) {
            project.setCustomProperty("com.mooregreatsoftware.property.orgName", value)
        }

    override var orgUrl: String?
        get() = customProp("com.mooregreatsoftware.property.orgUrl", NULL_SUPPLIER)
        set(value) {
            project.setCustomProperty("com.mooregreatsoftware.property.orgUrl", value)
        }

    override var bintrayRepo: String?
        get() = customProp(BINTRAY_REPO_KEY, NULL_SUPPLIER)
        set(value) {
            project.setCustomProperty(BINTRAY_REPO_KEY, value)
        }

    override var bintrayPkg: String?
        get() = customProp(BINTRAY_PKG_KEY, NULL_SUPPLIER)
        set(value) {
            project.setCustomProperty(BINTRAY_PKG_KEY, value)
        }

    override var bintrayLabels: Set<String>?
        get() = customProp(BINTRAY_LABELS_KEY, NULL_SUPPLIER)
        set(value) {
            project.setCustomProperty(BINTRAY_LABELS_KEY, value)
        }

    override var isBintrayToCentral: Boolean
        get() = customProp("com.mooregreatsoftware.property.bintray.isBintrayToCentral", { false })!!
        set(value) {
            project.setCustomProperty("com.mooregreatsoftware.property.bintray.isBintrayToCentral", value)
        }

    override val developers: Set<Developer>?
        get() = customProp("com.mooregreatsoftware.property.developers", NULL_SUPPLIER)

    fun setDevelopers(devs: Set<Map<String, Any>>) {
        fun devFromMap(map: Map<String, Any>): Developer {
            val email = map["email"] as String? ?: throw IllegalArgumentException("The email address for a developer must be set")
            val id = map.getOrElse("id", { email }) as String
            val name = map.getOrElse("name", { email }) as String
            return Developer(id = id, name = name, email = email)
        }

        project.setCustomProperty("com.mooregreatsoftware.property.developers", devs.map { devFromMap(it) }.toSet())
    }

    override var contributors: Set<Map<*, *>>?
        get() = customProp("com.mooregreatsoftware.property.contributors", NULL_SUPPLIER)
        set(value) {
            project.setCustomProperty("com.mooregreatsoftware.property.contributors", value)
        }

    override var siteUrl: String
        get() = customProp(SITE_URL_KEY, { "https://github.com/$orgId/${project.name}" })!!
        set(value) {
            project.setCustomProperty(SITE_URL_KEY, value)
        }

    override var issuesUrl: String
        get() = customProp(ISSUES_URL_KEY, { "$siteUrl/issues" })!!
        set(value) {
            project.setCustomProperty(ISSUES_URL_KEY, value)
        }

    override var vcsReadUrl: String
        get() = customProp(VCS_READ_URL_KEY, { "$siteUrl.git" })!!
        set(value) {
            project.setCustomProperty(VCS_READ_URL_KEY, value)
        }

    override var vcsWriteUrl: String
        get() = customProp(VCS_WRITE_URL_KEY, { vcsReadUrl })!!
        set(value) {
            project.setCustomProperty(VCS_WRITE_URL_KEY, value)
        }

    override var licenseKey: String
        get() = customProp(LICENSE_KEY, { "Apache-2.0" })!!
        set(value) {
            project.setCustomProperty(LICENSE_KEY, value)
        }

    override var licenseName: String
        get() = customProp("com.mooregreatsoftware.property.license.name", { "The Apache Software License, Version 2.0" })!!
        set(value) {
            project.setCustomProperty("com.mooregreatsoftware.property.license.name", value)
        }

    override var licenseUrl: String
        get() = customProp("com.mooregreatsoftware.property.license.url", { "http://www.apache.org/licenses/LICENSE-2.0" })!!
        set(value) {
            project.setCustomProperty("com.mooregreatsoftware.property.license.url", value)
        }

    override var copyrightYears: String
        get() = license.copyrightYears
        set(value) {
            license.copyrightYears = value
        }


    override val compatibilityVersion: JavaVersion
        get() = customProp("com.mooregreatsoftware.property.java.version", {
            val convention = project.convention.findPlugin(JavaPluginConvention::class.java) ?:
                throw GradleException("Trying to get the Java compatibility version on a project without the \"java\" plugin")
            convention.sourceCompatibility
        })!!

    fun setCompatibilityVersion(compatibilityVersion: Any) {
        project.setCustomProperty("com.mooregreatsoftware.property.java.version", JavaVersion.toVersion(compatibilityVersion))
    }


    private fun <T> customProp(propName: String, defaultSupplier: (Project) -> T): T? {
        return when {
            project.hasCustomProperty(propName) -> project.getCustomProperty(propName)
            else -> defaultSupplier(project)
        }
    }

    fun inspect(): String {
        return "DefaultsExtension(project=$project, lombok=$lombok, checkerFramework=$checkerFramework, license=$license)"
    }

    override fun toString(): String {
        return "DefaultsExtension(project=$project, lombok=$lombok, checkerFramework=$checkerFramework, orgId=$orgId, orgName=$orgName, orgUrl=$orgUrl, bintrayRepo=$bintrayRepo, bintrayPkg=$bintrayPkg, bintrayLabels=$bintrayLabels, isBintrayToCentral=$isBintrayToCentral, developers=$developers, contributors=$contributors, siteUrl=$siteUrl, issuesUrl=$issuesUrl, vcsReadUrl=$vcsReadUrl, vcsWriteUrl=$vcsWriteUrl, licenseKey='$licenseKey', licenseName='$licenseName', licenseUrl='$licenseUrl', copyrightYears=$copyrightYears, compatibilityVersion=$compatibilityVersion)"
    }


    companion object {
        val LOG: Logger = LoggerFactory.getLogger(DefaultsExtension::class.java)

        /**
         * The name to register this under as a Gradle extension.
         */
        const val NAME = "defaults"

        //        const val BINTRAY_PKG_KEY = "com.mooregreatsoftware.property.bintray.pkg"
//        const val BINTRAY_REPO_KEY = "com.mooregreatsoftware.property.bintray.repo"
//        const val BINTRAY_LABELS_KEY = "com.mooregreatsoftware.property.bintray.labels"
//
//        const val ORG_ID_KEY = "com.mooregreatsoftware.property.orgId"
//        const val SITE_URL_KEY = "com.mooregreatsoftware.property.siteUrl"
//        const val ISSUES_URL_KEY = "com.mooregreatsoftware.property.issuesUrl"
//
//        const val LICENSE_KEY = "com.mooregreatsoftware.property.license.key"
//
//        const val VCS_READ_URL_KEY = "com.mooregreatsoftware.property.vcs.readUrl"
        const val VCS_WRITE_URL_KEY = "com.mooregreatsoftware.property.vcs.writeUrl"


        private val NULL_SUPPLIER: (Project) -> Nothing? = { null }

        private tailrec fun <T> climb(ext: DefaultsExtension, supplier: (DefaultsExtension) -> T?, defaultSupplier: (Project) -> T): T {
            return supplier(ext) ?:
                if (ext.project.isRootProject()) defaultSupplier(ext.project)
                else climb(ext.project.parent.defaultsExtension(), supplier, defaultSupplier)
        }
    }

}


fun Project.defaultsExtension(): DefaultsExtension = extensions.findByType(DefaultsExtension::class.java) as DefaultsExtension? ?:
    extensions.create(DefaultsExtension.NAME, DefaultsExtension::class.java, this, licenseExtension(project), lombokExtension(project), checkerFrameworkExtension(project))
