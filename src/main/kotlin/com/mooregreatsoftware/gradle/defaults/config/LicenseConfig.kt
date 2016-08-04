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

import groovy.lang.GroovyObject
import nl.javadude.gradle.plugins.license.License
import nl.javadude.gradle.plugins.license.LicenseExtension
import org.gradle.api.Project
import org.gradle.api.plugins.Convention
import java.util.function.Supplier

@SuppressWarnings("WeakerAccess")
class LicenseConfig(project: Project) : AbstractConfig(project) {


    fun config(copyrightYears: Supplier<String>) {
        plugins().apply("license")
        val licenseExt = project.convention.getByType(LicenseExtension::class.java)
        licenseExt.header = project.rootProject.file("gradle/HEADER")
        licenseExt.strictCheck = true
        licenseExt.isUseDefaultMappings = false
        licenseExt.mapping("groovy", "SLASHSTAR_STYLE")
        licenseExt.mapping("java", "SLASHSTAR_STYLE")

        project.afterEvaluate { prj -> configLicenseExtension(copyrightYears, licenseExt) }

        tasks().whenTaskAdded { task ->
            if (task is License) {
                task.exclude("**/*.properties")
            }
        }
    }


    private fun configLicenseExtension(copyrightYears: Supplier<String>, licenseExt: LicenseExtension) {
        @SuppressWarnings("RedundantCast") val metaClass = (licenseExt as GroovyObject).metaClass
        val conExtPropsProp = metaClass.getMetaProperty("extensions")
        val convExtensions = conExtPropsProp.getProperty(licenseExt) as Convention
        val extensions = convExtensions.extraProperties
        val years = copyrightYears.get()
        if (years != null) extensions.set("year", years)
    }

}
