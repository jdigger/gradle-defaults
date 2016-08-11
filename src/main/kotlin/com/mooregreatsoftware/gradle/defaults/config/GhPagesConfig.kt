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

import com.mooregreatsoftware.gradle.defaults.ReadableDefaultsExtension
import com.mooregreatsoftware.gradle.defaults.Ternary
import com.mooregreatsoftware.gradle.defaults.config.KotlinConfig.Companion.DOKKA_PLUGIN_NAME
import com.mooregreatsoftware.gradle.defaults.grgit
import com.mooregreatsoftware.gradle.defaults.isRootProject
import com.mooregreatsoftware.gradle.defaults.readableDefaultsExtension
import org.ajoberstar.gradle.git.ghpages.GithubPagesPlugin
import org.ajoberstar.gradle.git.ghpages.GithubPagesPluginExtension
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.CopySpec
import org.gradle.api.internal.file.copy.CopySpecInternal
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.GroovyPlugin.GROOVYDOC_TASK_NAME
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPlugin.JAVADOC_TASK_NAME
import org.gradle.api.plugins.scala.ScalaPlugin
import org.gradle.api.plugins.scala.ScalaPlugin.SCALA_DOC_TASK_NAME
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class GhPagesConfig private constructor(project: Project, readableDefaultsFuture: Future<ReadableDefaultsExtension>) {

    init {
        with(project) {
            logger.info("Applying plugin '$GITHUB_PAGES_PLUGIN_ID'")
            plugins.apply(GITHUB_PAGES_PLUGIN_ID)

            associateDocTasks()

            val pages = githubPages().pages as CopySpecInternal
            pages.addChild().from("src/gh-pages")

            tasks.getByName(GithubPagesPlugin.getPREPARE_TASK_NAME()).doFirst {
                val readableDefaultsExtension = readableDefaultsFuture.get(1, TimeUnit.SECONDS)
                githubPages().setRepoUri(readableDefaultsExtension.vcsWriteUrl)
            }
        }
    }


    private fun Project.associateDocTasks() {
        // associate the "*doc" tasks of the different languages with the gh-pages output
        plugins.withType(JavaPlugin::class.java) { addOutput(tasks.getByName(JAVADOC_TASK_NAME)) }
        plugins.withType(GroovyPlugin::class.java) { addOutput(tasks.getByName(GROOVYDOC_TASK_NAME)) }
        plugins.withType(ScalaPlugin::class.java) { addOutput(tasks.getByName(SCALA_DOC_TASK_NAME)) }
        plugins.withId("kotlin") {
            plugins.withId(DOKKA_PLUGIN_NAME) {
                val dokkaTask = tasks.getByName("dokka")

                addOutput(dokkaTask)

                var dokkaJavadoc = tasks.findByName("dokkaJavadoc")
                if (dokkaJavadoc == null) {
                    @Suppress("UNCHECKED_CAST")
                    val clazz = Class.forName("org.jetbrains.dokka.gradle.DokkaTask") as Class<Task>
                    dokkaJavadoc = tasks.create("dokkaJavadoc", clazz, { task ->
                        val taskClass = task.javaClass
                        taskClass.getMethod("setOutputFormat", String::class.java)(task, "javadoc")
                        taskClass.getMethod("setOutputDirectory", String::class.java)(task, "${buildDir}/dokkaJavadoc")
                    })
                }
                addOutput(dokkaJavadoc)
            }
            afterEvaluate {
                if (!plugins.hasPlugin(DOKKA_PLUGIN_NAME)) logger.warn("Using the Kotlin plugin, but Dokka is not being used for API documentation")
            }
        }
    }


    private fun addOutput(task: Task): CopySpec {
        val project = task.project

        val logger = project.logger
        logger.info("Creating CopySpec for GhPages of $project from ${task.name}")

        val fromFiles = task.outputs.files
        val intoDir = task.path.replace(":", "/")
        logger.debug("will copy $fromFiles to $intoDir")

        val pages = project.githubPages().pages as CopySpecInternal
        return pages.addChild().into(intoDir).from(fromFiles)
    }


    companion object {

        private fun create(project: Project): Future<GhPagesConfig?> {
            return confFuture(project, "GH-Pages",
                {
                    if (project.isRootProject() && project.grgit() != null)
                        Ternary.TRUE
                    else
                        Ternary.FALSE
                },
                { true },
                { GhPagesConfig(project, project.readableDefaultsExtension()) },
                null
            )
        }

        /**
         * Returns the GhPagesConfig for the given Project.
         *
         * If this project is either not the root project, or if it is not in a git repository, this returns null.
         *
         * @param project the project containing the GhPagesConfig
         */
        @JvmStatic fun of(project: Project): Future<GhPagesConfig?> {
            return ofFuture(project, { create(project) })
        }

    }

}

const val GITHUB_PAGES_PLUGIN_ID = "org.ajoberstar.github-pages"


fun Project.githubPages(): GithubPagesPluginExtension {
    return extensions.getByType(GithubPagesPluginExtension::class.java)
}
