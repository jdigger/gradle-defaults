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

import com.mooregreatsoftware.gradle.defaults.config.CheckerFrameworkExtension
import com.mooregreatsoftware.gradle.defaults.config.LombokExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import java.util.concurrent.Future

/**
 * A read-only set of values for [DefaultsPlugin].
 *
 * @see [readableDefaultsExtension]
 */
interface ReadableDefaultsExtension {
    /**
     * Is this an open-source project? If true, additional assumptions are made, such as the need to have a license
     * on all code.
     */
    val openSource: Boolean

    /**
     * The Project this is associated/registered with.
     */
    val project: Project

    /**
     * Configuration for [Lombok](https://projectlombok.org/features/index.html)
     */
    val lombok: LombokExtension

    /**
     * Configuration for [Checker Framework](types.cs.washington.edu/checker-framework/current/checker-framework-manual.html)
     */
    val checkerFramework: CheckerFrameworkExtension

    /**
     * The id of the organization (a user also counts as a one-person organization). Used as the default for
     * computing the default values of [siteUrl], [vcsReadUrl] and [vcsWriteUrl].
     */
    val orgId: String

    /**
     * Used for configuring BinTray and Maven metadata.
     *
     * For BinTray, if this isn't set then [orgId] is used.
     *
     * For Maven, if this isn't set then the "organization" section of the pom.xml is omitted.
     */
    val orgName: String?

    /**
     * If there's an [orgName] then this is used as part of the "organization" section of the Maven pom.xml
     */
    val orgUrl: String?

    /**
     * The name of the BinTray repository to upload artifacts to, if BinTray is used.
     */
    val bintrayRepo: String?

    /**
     * The name of the BinTray package name, if BinTray is used.
     */
    val bintrayPkg: String?

    /**
     * The labels to set of the BinTray package, if BinTray is used.
     */
    val bintrayLabels: Set<String>?

    /**
     * Should the artifacts upload to BinTray also be sent to Maven Central?
     *
     * Default is "false"
     */
    val isBintrayToCentral: Boolean

    /**
     * The information to add to the "contributors" section of the Maven pom.xml
     */
    val contributors: Set<Map<*, *>>?

    /**
     * The information to add to the "developers" section of the Maven pom.xml
     */
    val developers: Set<Developer>?

    /**
     * The URL for the project. Defaults to "https://github.com/$orgId/${project.name}"
     */
    val siteUrl: String

    /**
     * The URL for where to find issue reports for the project. Defaults to "$siteUrl/issues"
     */
    val issuesUrl: String

    /**
     * The URL for where the version control system gets the source code for the project. Defaults to "${siteUrl}.git"
     */
    val vcsReadUrl: String

    /**
     * The URL for where the version control system gets the source code for the project. Defaults to [vcsReadUrl]
     */
    val vcsWriteUrl: String

    /**
     * The "key" for the license. Used by BinTray. Defaults to "Apache-2.0"
     */
    // TODO coordinate this and the other license* properties
    val licenseKey: String

    /**
     * The name of the license. Used by Maven. Defaults to "The Apache Software License, Version 2.0"
     */
    // TODO coordinate this and the other license* properties
    val licenseName: String

    /**
     * The URL of the license. Used by Maven. Defaults to http://www.apache.org/licenses/LICENSE-2.0
     */
    // TODO coordinate this and the other license* properties
    val licenseUrl: String

    /**
     * The copyright years for the project. Used for injecting into the license headers under the key "year".
     * Defaults to the current year.
     */
    val copyrightYears: String

    /**
     * The version of the Java Virtual Machine to target. Used for all the compilers that target the JVM.
     * Defaults to the version of Java being used to run Gradle and compile the code.
     */
    val compatibilityVersion: JavaVersion
}

class Developer(val id: String, val name: String, val email: String)


/**
 * The [Future] that contains the [ReadableDefaultsExtension] for the [Project] after the Project has been evaluated.
 */
@Suppress("UNCHECKED_CAST")
fun Project.readableDefaultsExtension(): Future<ReadableDefaultsExtension> {
    val keyName = ReadableDefaultsExtension::class.java.name
    val ext = extensions.extraProperties
    return when {
        ext.has(keyName) -> ext.get(keyName) as Future<ReadableDefaultsExtension>
        else -> {
            val future = postEvalCreate { project.defaultsExtension() }
            ext.set(keyName, future)
            future as Future<ReadableDefaultsExtension>
        }
    }
}
