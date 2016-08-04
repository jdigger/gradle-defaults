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

import com.mooregreatsoftware.gradle.defaults.config.BintrayConfig
import com.mooregreatsoftware.gradle.defaults.config.CheckerFrameworkConfiguration
import com.mooregreatsoftware.gradle.defaults.config.GhPagesConfig
import com.mooregreatsoftware.gradle.defaults.config.GroovyConfig
import com.mooregreatsoftware.gradle.defaults.config.IntellijConfig
import com.mooregreatsoftware.gradle.defaults.config.JavaConfig
import com.mooregreatsoftware.gradle.defaults.config.LicenseConfig
import com.mooregreatsoftware.gradle.defaults.config.LombokConfiguration
import com.mooregreatsoftware.gradle.defaults.config.MavenPublishingConfig
import com.mooregreatsoftware.gradle.defaults.config.ReleaseConfig
import com.mooregreatsoftware.gradle.defaults.config.ScalaConfig
import org.ajoberstar.grgit.Grgit
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.StoredConfig
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.publish.plugins.PublishingPlugin
import java.io.IOException
import java.util.function.Supplier

class DefaultsPlugin : Plugin<Project> {


    override fun apply(project: Project) {
        if (project != project.rootProject) {
            project.logger.warn(this.javaClass.name + " can only be applied to the root project")
            return
        }

        val grgit = createGrgit(project)

        val extension = project.extensions.create("defaults", DefaultsExtension::class.java, project)

        project.plugins.apply("org.ajoberstar.organize-imports")

        if (grgit != null) {
            GhPagesConfig(project).config(Supplier<String> { extension.vcsWriteUrl })
            ReleaseConfig(project, grgit).config()
        }

        project.allprojects { prj -> configProject(prj, extension) }
    }


    private fun configProject(prj: Project, extension: DefaultsExtension) {
        prj.repositories.jcenter()
        val javaConfig = JavaConfig.of(prj)
        IntellijConfig.create(prj, { extension.compatibilityVersion }, javaConfig)
        GroovyConfig.create(prj)
        ScalaConfig(prj).config(Supplier<String> { extension.compatibilityVersion })
        LicenseConfig(prj).config(Supplier<String> { extension.copyrightYears })
        MavenPublishingConfig(prj, extension).config()
        BintrayConfig(prj, extension).config()
        configLombok(prj, extension, javaConfig)
        CheckerFrameworkConfiguration.create(prj, Supplier<String> { extension.checkerFrameworkVersion }, javaConfig)
        addOrderingRules(prj)
    }


    private fun configLombok(prj: Project, extension: DefaultsExtension, javaConfig: JavaConfig) {
        if (prj.plugins.findPlugin(JavaBasePlugin::class.java) != null) {
            potentialConfigureLombok(prj, extension, javaConfig)
        }
        else {
            prj.afterEvaluate { p -> potentialConfigureLombok(p, extension, javaConfig) }
        }
    }

    companion object {

        val DEFAULT_USER_EMAIL = "unknown@unknown"
        private val EMAIL_CACHE_KEY = "com.mooregreatsoftware.defaults.useremail"


        private fun potentialConfigureLombok(prj: Project, extension: DefaultsExtension, javaConfig: JavaConfig) {
            // TODO Move to LombokConfiguration.create
            if (extension.useLombok)
                LombokConfiguration.create(prj, Supplier<String> { extension.lombokVersion }, javaConfig)
            else
                prj.logger.info("Not configuring Lombok for {}", prj.name)
        }


        @Suppress("DEPRECATION")
        private fun createGrgit(project: Project): Grgit? {
            try {
                return Grgit.open(project.file("."))
            }
            catch (e: Exception) {
                return null
            }

        }


        private fun addOrderingRules(project: Project) {
            project.plugins.withType(BasePlugin::class.java) { plugin ->
                allTasksShouldRunAfterClean(project)
                publishingTasksShouldRunAfterBuild(project)
            }
        }


        private fun allTasksShouldRunAfterClean(project: Project) {
            val clean = project.tasks.getByName(BasePlugin.CLEAN_TASK_NAME)
            project.tasks.forEach { task ->
                if (task != clean) {
                    task.shouldRunAfter(clean)
                }
            }
        }


        private fun publishingTasksShouldRunAfterBuild(project: Project) {
            val build = project.tasks.getByName(BasePlugin.BUILD_GROUP)
            project.tasks.forEach { task ->
                if (task.group == PublishingPlugin.PUBLISH_TASK_GROUP) {
                    task.shouldRunAfter(build)
                }
            }
        }


        fun userEmail(project: Project): String {
            val rootExt = project.rootProject.extensions.extraProperties
            return when {
                rootExt.has(EMAIL_CACHE_KEY) -> rootExt.get(EMAIL_CACHE_KEY) as String
                else -> {
                    val userEmail = detectUserEmail(project)
                    rootExt.set(EMAIL_CACHE_KEY, userEmail)
                    userEmail
                }
            }
        }


        private fun detectUserEmail(project: Project): String {
            try {
                val rootDir = project.rootDir
                val userEmail = Git.open(rootDir).repository.config.email
                return when {
                    userEmail.isNullOrBlank() -> {
                        project.logger.warn("The git repository's \"user.email\" configuration is null, " +
                            "so using \"${DEFAULT_USER_EMAIL}\" instead")
                        DEFAULT_USER_EMAIL
                    }
                    else -> userEmail!!
                }
            }
            catch (e: IOException) {
                project.logger.warn("Could not detect the user's email address from the " +
                    "git repository's \"user.email\" configuration, so using \"${DEFAULT_USER_EMAIL}\" instead")
                return DEFAULT_USER_EMAIL
            }
        }


        val StoredConfig.email: String? get() = this.getString("user", null, "email")
    }

}
