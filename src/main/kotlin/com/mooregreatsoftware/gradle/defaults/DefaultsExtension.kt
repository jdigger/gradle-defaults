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
package com.mooregreatsoftware.gradle.defaults

import com.mooregreatsoftware.gradle.defaults.config.CheckerFrameworkConfiguration
import com.mooregreatsoftware.gradle.defaults.config.LombokConfiguration
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention

@Suppress("unused")
open class DefaultsExtension(val project: Project) {

    private var _id: String? = null
    var id: String
        get() {
            if (_id == null) {
                throw IllegalStateException("\"id\" is not set for " + DefaultsExtension::class.java.name + " on " + this.project.name)
            }
            return _id as String
        }
        set(value) {
            _id = value
        }
    var orgName: String? = null
    var orgUrl: String? = null
    var bintrayRepo: String? = null
    var bintrayPkg: String? = null
    var bintrayLabels: Set<String>? = null
    var isBintrayToCentral = false

    private var _developers: Set<Developer>? = null

    var contributors: Set<Map<*, *>>? = null

    private var _siteUrl: String? = null
    var siteUrl: String
        get() = _siteUrl ?: "https://github.com/" + id + "/" + project.name
        set(value) {
            _siteUrl = value
        }

    private var _issuesUrl: String? = null
    var issuesUrl: String
        get() = _issuesUrl ?: siteUrl + "/issues"
        set(value) {
            _issuesUrl = value
        }

    private var _vcsReadUrl: String? = null
    var vcsReadUrl: String
        get() = _vcsReadUrl ?: siteUrl + ".git"
        set(value) {
            _vcsReadUrl = value
        }

    private var _vcsWriteUrl: String? = null
    var vcsWriteUrl: String
        get() = _vcsWriteUrl ?: "git@github.com:" + id + "/" + project.name + ".git"
        set(value) {
            _vcsWriteUrl = value
        }

    var licenseKey: String = "Apache-2.0"
    var licenseName: String = "The Apache Software License, Version 2.0"
    var licenseUrl: String = "http://www.apache.org/licenses/LICENSE-2.0"
    var copyrightYears: String? = null
    var lombokVersion: String = LombokConfiguration.DEFAULT_LOMBOK_VERSION
    var checkerFrameworkVersion: String = CheckerFrameworkConfiguration.DEFAULT_CHECKER_VERSION

    private var _useLombok = -1

    private var _useCheckerFramework = -1


    fun setCompatibilityVersion(compatibilityVersion: Any) {
        val convention = project.convention.findPlugin(JavaPluginConvention::class.java) ?: throw GradleException("Trying to set the Java compatibility version on a project without the \"java\" plugin")
        convention.setSourceCompatibility(compatibilityVersion)
    }


    val compatibilityVersion: String
        get() {
            val convention = project.convention.findPlugin(JavaPluginConvention::class.java) ?: throw GradleException("Trying to get the Java compatibility version on a project without the \"java\" plugin")
            return convention.sourceCompatibility.toString()
        }


    var useLombok: Boolean
        get() {
            return if (_useLombok > -1)
                _useLombok == 1
            else
                hasJavaSource(project)
        }
        set(useLombok) {
            _useLombok = if (useLombok) 1 else 0
        }


    var useCheckerFramework: Boolean
        get() {
            return if (_useCheckerFramework > -1)
                _useCheckerFramework == 1
            else
                hasJavaSource(project)
        }
        set(useCheckerFramework) {
            _useCheckerFramework = if (useCheckerFramework) 1 else 0
        }


    fun setDevelopers(devs: Set<Map<String, Any>>) {
        _developers = devs.map { devFromMap(it) }.toSet()
    }


    fun getDevelopers(): Set<Developer>? = _developers


    private fun devFromMap(map: Map<String, Any>): Developer {
        // TODO Check to see how much of this is necessary, or if can apply directly to the Maven configuration
        val email = map["email"] as String? ?: throw IllegalArgumentException("The email address for a developer must be set")
        val id = map.getOrElse("id", { email }) as String
        val name = map.getOrElse("name", { email }) as String
        return Developer(id = id, name = name, email = email)
    }


    class Developer(val id: String, val name: String, val email: String)

}
