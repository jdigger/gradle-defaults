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
import com.mooregreatsoftware.gradle.defaults.config.KotlinConfig
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
import org.gradle.api.publish.plugins.PublishingPlugin
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Supplier

class DefaultsPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        if (project != project.rootProject) {
            project.logger.warn("${this.javaClass.name} can only be applied to the root project")
            return
        }

        val extension = project.defaultsExtension()

        project.allprojects { prj -> configProject(prj, extension) }
    }


    private fun configProject(prj: Project, extension: DefaultsExtension) {
        prj.defaultsExtension()

        prj.repositories.jcenter()

        prj.plugins.apply("org.ajoberstar.organize-imports")

        ReleaseConfig.of(prj)
        GhPagesConfig.of(prj)
        JavaConfig.of(prj)
        IntellijConfig.of(prj)
        GroovyConfig.of(prj)
        ScalaConfig.of(prj)
        KotlinConfig.of(prj)
        LicenseConfig(prj).config(Supplier<String> { extension.copyrightYears })
        MavenPublishingConfig(prj)
        BintrayConfig.of(prj)
        LombokConfiguration.of(prj)
        CheckerFrameworkConfiguration.of(prj)

        addOrderingRules(prj)
    }


    companion object {
        val DEFAULT_USER_EMAIL = "unknown@unknown"
        private val EMAIL_CACHE_KEY = "com.mooregreatsoftware.defaults.useremail"


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

@Suppress("UNCHECKED_CAST", "DEPRECATION")
fun Project.grgit(): Grgit? {
    val key = Grgit::class.java.name
    val ext = rootProject.extensions.extraProperties
    if (ext.has(key)) {
        return (ext.get(key) as AtomicReference<Grgit?>).get()
    }
    else {
        var grgitRef: AtomicReference<Grgit?>
        try {
            grgitRef = AtomicReference(Grgit.open(project.file(".")))
        }
        catch (e: Exception) {
            grgitRef = AtomicReference(null)
        }
        ext.set(key, grgitRef)
        return grgitRef.get()
    }
}
