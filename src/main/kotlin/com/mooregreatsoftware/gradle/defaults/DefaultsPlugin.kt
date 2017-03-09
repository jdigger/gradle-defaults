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

import com.jfrog.bintray.gradle.BintrayPlugin
import com.mooregreatsoftware.gradle.bintray.ExtBintrayPlugin
import com.mooregreatsoftware.gradle.checkerframework.CheckerFrameworkPlugin
import com.mooregreatsoftware.gradle.ghpages.ExtGhPagesPlugin
import com.mooregreatsoftware.gradle.groovy.ExtGroovyPlugin
import com.mooregreatsoftware.gradle.ide.ExtIntellijPlugin
import com.mooregreatsoftware.gradle.java.ExtJavaPlugin
import com.mooregreatsoftware.gradle.kotlin.ExtKotlinPlugin
import com.mooregreatsoftware.gradle.license.ExtLicensePlugin
import com.mooregreatsoftware.gradle.lombok.LombokPlugin
import com.mooregreatsoftware.gradle.release.ExtReleasePlugin
import com.mooregreatsoftware.gradle.scala.ExtScalaPlugin
import com.mooregreatsoftware.gradle.util.GrGitUtils
import com.mooregreatsoftware.gradle.util.hasJavaSource
import com.mooregreatsoftware.gradle.util.isRootProject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.compile.AbstractCompile

class DefaultsPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        if (project != project.rootProject) {
            project.logger.warn("${this.javaClass.name} can only be applied to the root project")
            return
        }

        project.allprojects { prj -> configProject(prj) }
    }


    private fun configProject(prj: Project) {
        val defaultsExtension = prj.defaultsExtension()

        prj.repositories.jcenter()

        if (prj.isRootProject() && GrGitUtils.grgit(prj) != null) {
            prj.plugins.apply(ExtReleasePlugin.PLUGIN_ID)
            prj.plugins.apply(ExtGhPagesPlugin.PLUGIN_ID)
        }

        prj.plugins.apply(ExtIntellijPlugin.PLUGIN_ID)

        prj.afterEvaluate {
            if (defaultsExtension.openSource)
                prj.plugins.apply(ExtLicensePlugin.PLUGIN_ID)
        }

        prj.plugins.withType(BintrayPlugin::class.java) {
            prj.plugins.apply(ExtBintrayPlugin::class.java)
        }

        prj.plugins.withId("java") {
            prj.plugins.apply(ExtJavaPlugin.PLUGIN_ID)
            if (prj.hasJavaSource()) {
                prj.plugins.apply(LombokPlugin.PLUGIN_ID)
                prj.plugins.apply(CheckerFrameworkPlugin.PLUGIN_ID)
            }
        }

        prj.plugins.withId("groovy") {
            prj.plugins.apply(ExtGroovyPlugin.PLUGIN_ID)
        }

        prj.plugins.withId("scala") {
            prj.plugins.apply(ExtScalaPlugin.PLUGIN_ID)
        }

        prj.plugins.withId("kotlin") {
            prj.plugins.apply(ExtKotlinPlugin.PLUGIN_ID)
        }

        prj.tasks.withType(AbstractCompile::class.java, { abstractCompile ->
            abstractCompile.doFirst({ task ->
                val defaults = task.project.defaultsExtension()
                abstractCompile.sourceCompatibility = defaults.compatibilityVersion.toString()
                abstractCompile.targetCompatibility = defaults.compatibilityVersion.toString()
            })
        })

        addOrderingRules(prj)
    }


    companion object {
        val PLUGIN_ID = "com.mooregreatsoftware.defaults"


        private fun addOrderingRules(project: Project) {
            project.plugins.withType(BasePlugin::class.java) {
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
    }

}
