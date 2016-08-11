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
import nl.javadude.gradle.plugins.license.LicenseExtension
import nl.javadude.gradle.plugins.license.LicensePlugin
import org.gradle.api.Project
import org.gradle.api.plugins.Convention
import org.gradle.api.plugins.ExtraPropertiesExtension
import java.util.function.Supplier

class LicenseConfig(private val project: Project) {

    fun config(copyrightYears: Supplier<String>) {
        project.plugins.apply(LicensePlugin::class.java)
        val licenseExt = project.convention.getByType(LicenseExtension::class.java)
        licenseExt.header = project.rootProject.file("gradle/HEADER")
        licenseExt.strictCheck = true
        licenseExt.isUseDefaultMappings = true
        licenseExt.mapping("groovy", "SLASHSTAR_STYLE")
        licenseExt.mapping("java", "SLASHSTAR_STYLE")
        licenseExt.mapping("scala", "SLASHSTAR_STYLE")
        licenseExt.mapping("kt", "SLASHSTAR_STYLE")
        licenseExt.mapping("css", "SLASHSTAR_STYLE")

        project.afterEvaluate { prj -> configLicenseExtension(copyrightYears, licenseExt) }
    }


    private fun configLicenseExtension(copyrightYears: Supplier<String>, licenseExt: LicenseExtension) {
        licenseExt.exclude("**/*.properties")

        val years = copyrightYears.get()
        if (years != null) licenseExt.ext.set("year", years)
    }

}

val LicenseExtension.ext: ExtraPropertiesExtension
    get() {
        val metaClass = (this as GroovyObject).metaClass
        val conExtPropsProp = metaClass.getMetaProperty("extensions")
        val convExtensions = conExtPropsProp.getProperty(this) as Convention
        return convExtensions.extraProperties
    }
