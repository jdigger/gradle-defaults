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

import com.jfrog.bintray.gradle.BintrayPlugin
import com.mooregreatsoftware.gradle.defaults.Ternary
import com.mooregreatsoftware.gradle.defaults.grgit
import com.mooregreatsoftware.gradle.defaults.isRootProject
import org.ajoberstar.gradle.git.release.base.ReleasePluginExtension
import org.ajoberstar.grgit.Grgit
import org.gradle.api.Project
import java.util.concurrent.Future

class ReleaseConfig private constructor(project: Project, grgit: Grgit) {

    init {
        project.plugins.apply("org.ajoberstar.release-opinion")
        val release = project.convention.getByType(ReleasePluginExtension::class.java)
        release.grgit = grgit

        val releaseTask = project.tasks.getByName("release")

        project.plugins.withId(GITHUB_PAGES_PLUGIN_ID) {
            releaseTask.dependsOn("publishGhPages")
        }

        project.allprojects { prj ->
            val prjPlugins = prj.plugins
            val prjTasks = prj.tasks
            prjPlugins.withId("org.gradle.base") {
                releaseTask.dependsOn(prjTasks.getByName("clean"), prjTasks.getByName("build"))
            }
            prjPlugins.withType(BintrayPlugin::class.java) {
                releaseTask.dependsOn(prjTasks.getByName("bintrayUpload"))
            }
        }
    }


    companion object {

        private fun create(project: Project): Future<ReleaseConfig?> {
            return confFuture(project, "Release",
                {
                    if (project.isRootProject() && project.grgit() != null)
                        Ternary.TRUE
                    else
                        Ternary.FALSE
                },
                { true },
                { ReleaseConfig(project, project.grgit()!!) },
                null
            )
        }

        /**
         * Returns the ReleaseConfig for the given Project.
         *
         * If this project is either not the root project, or if it is not in a git repository, this returns null.
         *
         * @param project the project containing the ReleaseConfig
         */
        @JvmStatic fun of(project: Project): Future<ReleaseConfig?> {
            return ofFuture(project, { create(project) })
        }

    }

}
