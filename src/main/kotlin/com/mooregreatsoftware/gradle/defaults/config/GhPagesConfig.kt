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

import org.ajoberstar.gradle.git.ghpages.GithubPagesPluginExtension
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.CopySpec
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.GroovyPlugin.GROOVYDOC_TASK_NAME
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPlugin.JAVADOC_TASK_NAME
import org.gradle.api.plugins.scala.ScalaPlugin
import org.gradle.api.plugins.scala.ScalaPlugin.SCALA_DOC_TASK_NAME
import java.util.function.Supplier

@SuppressWarnings("WeakerAccess")
class GhPagesConfig(project: Project) : AbstractConfig(project) {


    fun config(vcsWriteUrlSupplier: Supplier<String>) {
        info("Applying plugin 'org.ajoberstar.github-pages'")
        plugins().apply("org.ajoberstar.github-pages")

        project.allprojects { this.associateDocTasks(it) }

        githubPages().pages.from("src/gh-pages")

        project.afterEvaluate { prj ->
            debug("Continuing configuring githubPages extension")
            githubPages().setRepoUri(vcsWriteUrlSupplier.get())
        }
    }


    private fun associateDocTasks(prj: Project) {
        val prjPlugins = prj.plugins
        val prjTasks = prj.tasks

        // associate the "*doc" tasks of the different languages with the gh-pages output
        prjPlugins.withType(JavaPlugin::class.java) { p -> addOutput(prjTasks.getByName(JAVADOC_TASK_NAME)) }
        prjPlugins.withType(GroovyPlugin::class.java) { p -> addOutput(prjTasks.getByName(GROOVYDOC_TASK_NAME)) }
        prjPlugins.withType(ScalaPlugin::class.java) { p -> addOutput(prjTasks.getByName(SCALA_DOC_TASK_NAME)) }
    }


    private fun addOutput(task: Task): CopySpec {
        val pages = githubPages().pages
        val from = pages.from(task.outputs.files)
        val replace = "docs" + task.path.replace(":", "/")

        return from.into(replace)
    }


    fun githubPages(): GithubPagesPluginExtension {
        return project.extensions.getByType(GithubPagesPluginExtension::class.java)
    }

}
