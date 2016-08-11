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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPlugin.JAR_TASK_NAME
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar

abstract class AbstractLanguageConfig
protected constructor(protected val project: Project) : ArtifactPublisher {

    private var docJarTask: Jar? = null


    fun <T : Plugin<*>> config(pluginClass: Class<T>): AbstractLanguageConfig {
        project.plugins.withType(pluginClass) { configLanguage(pluginClass.javaClass.name) }
        return this
    }


    fun config(pluginClassName: String): AbstractLanguageConfig {
        project.plugins.withId(pluginClassName) { configLanguage(pluginClassName) }
        return this
    }


    open protected fun configLanguage(pluginClassname: String) {
        project.logger.info("Configuring {}", pluginClassname)

        registerArtifacts(MavenPublishingConfig.mainPublication(project))
    }


    override fun registerArtifacts(publication: MavenPublication) {
        publication.artifact(docJarTask())
        project.artifacts.add("archives", docJarTask())
    }


    fun docJarTask(): Jar {
        if (docJarTask == null) {
            docJarTask = createDocJarTask()
        }
        return docJarTask as Jar
    }


    protected fun createDocJarTask(): Jar {
        val docTaskName = docTaskName()
        val docJarTask = project.tasks.create(docTaskName + "Jar", Jar::class.java)
        docJarTask.classifier = docTaskName
        docJarTask.from(docTask().outputs.files)
        return docJarTask
    }


    fun docTask(): Task {
        return project.tasks.getByName(docTaskName())
    }


    protected abstract fun docTaskName(): String


    fun jarTask(): Jar {
        return project.tasks.findByName(JAR_TASK_NAME) as Jar
    }


    protected abstract fun compileTaskName(): String

}
