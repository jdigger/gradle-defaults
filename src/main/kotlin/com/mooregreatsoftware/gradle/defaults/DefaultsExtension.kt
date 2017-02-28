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

import com.mooregreatsoftware.gradle.checkerframework.CheckerFrameworkExtension
import com.mooregreatsoftware.gradle.checkerframework.CheckerFrameworkPlugin.checkerFrameworkExtension
import com.mooregreatsoftware.gradle.license.ExtLicenseExtension
import com.mooregreatsoftware.gradle.license.ExtLicensePlugin
import com.mooregreatsoftware.gradle.license.ExtLicensePlugin.licenseExtension
import com.mooregreatsoftware.gradle.lombok.LombokExtension
import com.mooregreatsoftware.gradle.lombok.LombokPlugin.lombokExtension
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention

/**
 * Configuration for [DefaultsPlugin].
 *
 * @see [defaultsExtension]
 */
@Suppress("unused", "ConvertLambdaToReference")
open class DefaultsExtension(override val project: Project, override val license: ExtLicenseExtension, override val lombok: LombokExtension, override val checkerFramework: CheckerFrameworkExtension) : ReadableDefaultsExtension {

    private var _orgId: String? = null
    override var orgId: String
        get() {
            if (_orgId == null) {
                val parentId = project.parent.findProjectId()
                if (parentId != null) return parentId
                project.logger.debug("\"${DefaultsExtension.NAME}\" on ${project.name} - ${this.inspect()}")
                throw IllegalStateException("\"orgId\" is not set for \"${DefaultsExtension.NAME}\" on ${project.name}")
            }
            return _orgId as String
        }
        set(value) {
            _orgId = value
        }

    fun setId(value: String) {
        project.logger.warn("\"id\" property of \"${DefaultsExtension.NAME}\" is deprecated")
        orgId = value
    }

    override var openSource: Boolean
        get() {
            val openSourceProperty = project.openSourceProperty()
            when (openSourceProperty) {
                null -> {
                    val parentIsOpenSource = project.parent.findIsOpenSource()
                    when {
                        parentIsOpenSource != null -> return parentIsOpenSource
                        else -> {
                            project.logger.warn("\"$OPENSOURCE_PROPERTY_NAME\" is not set on ${project.rootProject.name}: assuming \"true\"")
                            return true
                        }
                    }
                }
                else -> return openSourceProperty
            }
        }
        set(value) {
            project.openSourceProperty(value)
        }

    private var _orgName: String? = null
    override var orgName: String?
        get() = climb({ it._orgName }, { null })
        set(value) {
            _orgName = value
        }

    private var _orgUrl: String? = null
    override var orgUrl: String?
        get() = climb({ it._orgUrl }, { null })
        set(value) {
            _orgUrl = value
        }

    private var _bintrayRepo: String? = null
    override var bintrayRepo: String?
        get() = climb({ it._bintrayRepo }, { null })
        set(value) {
            _bintrayRepo = value
        }

    private var _bintrayPkg: String? = null
    override var bintrayPkg: String?
        get() = climb({ it._bintrayPkg }, { null })
        set(value) {
            _bintrayPkg = value
        }

    private var _bintrayLabels: Set<String>? = null
    override var bintrayLabels: Set<String>?
        get() = climb({ it._bintrayLabels }, { null })
        set(value) {
            _bintrayLabels = value
        }

    private var _isBintrayToCentral: Boolean? = null
    override var isBintrayToCentral: Boolean
        get() = climb({ it._isBintrayToCentral }, { false })
        set(value) {
            _isBintrayToCentral = value
        }

    private var _developers: Set<Developer>? = null
    override val developers: Set<Developer>?
        get() = climb({ it._developers }, { null })

    fun setDevelopers(devs: Set<Map<String, Any>>) {
        _developers = devs.map { devFromMap(it) }.toSet()
    }

    private var _contributors: Set<Map<*, *>>? = null
    override var contributors: Set<Map<*, *>>?
        get() = climb({ it._contributors }, { null })
        set(value) {
            _contributors = value
        }

    private var _siteUrl: String? = null
    override var siteUrl: String
        get() = climb({ it._siteUrl }, { "https://github.com/$orgId/${project.name}" })
        set(value) {
            _siteUrl = value
        }

    private var _issuesUrl: String? = null
    override var issuesUrl: String
        get() = climb({ it._issuesUrl }, { "$siteUrl/issues" })
        set(value) {
            _issuesUrl = value
        }

    private var _vcsReadUrl: String? = null
    override var vcsReadUrl: String
        get() = climb({ it._vcsReadUrl }, { "$siteUrl.git" })
        set(value) {
            _vcsReadUrl = value
        }

    private var _vcsWriteUrl: String? = null
    override var vcsWriteUrl: String
        get() = climb({ it._vcsWriteUrl }, { vcsReadUrl })
        set(value) {
            _vcsWriteUrl = value
        }

    private var _licenseKey: String? = null
    override var licenseKey: String
        get() = climb({ it._licenseKey }, { "Apache-2.0" })
        set(value) {
            _licenseKey = value
        }

    private var _licenseName: String? = null
    override var licenseName: String
        get() = climb({ it._licenseName }, { "The Apache Software License, Version 2.0" })
        set(value) {
            _licenseName = value
        }

    private var _licenseUrl: String? = null
    override var licenseUrl: String
        get() = climb({ it._licenseUrl }, { "http://www.apache.org/licenses/LICENSE-2.0" })
        set(value) {
            _licenseUrl = value
        }

    override var copyrightYears: String
        get() = license.copyrightYears
        set(value) {
            license.copyrightYears = value
        }


    private var _compatibilityVersion: JavaVersion? = null
    override val compatibilityVersion: JavaVersion
        get() = when (_compatibilityVersion) {
            null -> {
                val convention = project.convention.findPlugin(JavaPluginConvention::class.java) ?:
                    throw GradleException("Trying to get the Java compatibility version on a project without the \"java\" plugin")
                convention.sourceCompatibility
            }
            else -> _compatibilityVersion!!
        }

    fun setCompatibilityVersion(compatibilityVersion: Any) {
        _compatibilityVersion = JavaVersion.toVersion(compatibilityVersion)
    }


    private fun <T> climb(supplier: (DefaultsExtension) -> T?, defaultSupplier: (Project) -> T): T {
        return climb(this, supplier, defaultSupplier)
    }

    fun inspect(): String {
        return "DefaultsExtension(project=$project, lombok=$lombok, checkerFramework=$checkerFramework, _orgId=$_orgId, _orgName=$_orgName, _orgUrl=$_orgUrl, _bintrayRepo=$_bintrayRepo, _bintrayPkg=$_bintrayPkg, _bintrayLabels=$_bintrayLabels, _isBintrayToCentral=$_isBintrayToCentral, _developers=$_developers, _contributors=$_contributors, _siteUrl=$_siteUrl, _issuesUrl=$_issuesUrl, _vcsReadUrl=$_vcsReadUrl, _vcsWriteUrl=$_vcsWriteUrl, _licenseKey=$_licenseKey, _licenseName=$_licenseName, _licenseUrl=$_licenseUrl, license=$license, _compatibilityVersion=$_compatibilityVersion)"
    }

    override fun toString(): String {
        return "DefaultsExtension(project=$project, lombok=$lombok, checkerFramework=$checkerFramework, orgId=$orgId, orgName=$orgName, orgUrl=$orgUrl, bintrayRepo=$bintrayRepo, bintrayPkg=$bintrayPkg, bintrayLabels=$bintrayLabels, isBintrayToCentral=$isBintrayToCentral, developers=$developers, contributors=$contributors, siteUrl=$siteUrl, issuesUrl=$issuesUrl, vcsReadUrl=$vcsReadUrl, vcsWriteUrl=$vcsWriteUrl, licenseKey='$licenseKey', licenseName='$licenseName', licenseUrl='$licenseUrl', copyrightYears=$copyrightYears, compatibilityVersion=$compatibilityVersion)"
    }


    companion object {
        /**
         * The name to register this under as a Gradle extension.
         */
        const val NAME = "defaults"
    }

}

// **************************************************************************
//
// PUBLIC VALUES AND FUNCTIONS
//
// **************************************************************************

val OPENSOURCE_PROPERTY_NAME = "openSource"


fun Project.openSourceProperty(): Boolean? {
    if (hasProperty(OPENSOURCE_PROPERTY_NAME)) {
        return java.lang.Boolean.valueOf(project.property(OPENSOURCE_PROPERTY_NAME).toString())
    }
    return null
}


fun Project.openSourceProperty(isOpenSource: Any) {
    extensions.extraProperties.set(OPENSOURCE_PROPERTY_NAME, java.lang.Boolean.valueOf(isOpenSource.toString()))
}


fun Project.defaultsExtension(): DefaultsExtension = extensions.findByType(DefaultsExtension::class.java) as DefaultsExtension? ?:
    extensions.create(DefaultsExtension.NAME, DefaultsExtension::class.java, this, licenseExtension(project), lombokExtension(project), checkerFrameworkExtension(project))


// **************************************************************************
//
// PRIVATE VALUES AND FUNCTIONS
//
// **************************************************************************

private fun devFromMap(map: Map<String, Any>): Developer {
    val email = map["email"] as String? ?: throw IllegalArgumentException("The email address for a developer must be set")
    val id = map.getOrElse("id", { email }) as String
    val name = map.getOrElse("name", { email }) as String
    return Developer(id = id, name = name, email = email)
}


private tailrec fun <T> climb(ext: DefaultsExtension, supplier: (DefaultsExtension) -> T?, defaultSupplier: (Project) -> T): T {
    return supplier(ext) ?:
        if (ext.project.isRootProject()) defaultSupplier(ext.project)
        else climb(ext.project.parent.defaultsExtension(), supplier, defaultSupplier)
}


private tailrec fun Project?.findProjectId(): String? {
    when (this) {
        null -> return null
        else -> {
            val dext = extensions.findByType(DefaultsExtension::class.java)
            return when {
                dext != null -> dext.orgId
                else -> parent.findProjectId()
            }
        }
    }
}


private tailrec fun Project?.findIsOpenSource(): Boolean? {
    when (this) {
        null -> return null
        else -> {
            val dext = extensions.findByType(DefaultsExtension::class.java)
            return when {
                dext != null -> dext.openSource
                else -> parent.findIsOpenSource()
            }
        }
    }
}
