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
import org.gradle.api.tasks.compile.AbstractCompile

abstract class AbstractLanguageConfig<PT : Plugin<*>>
protected constructor(project: Project) : AbstractConfig(project), ArtifactPublisher {

    private var docJarTask: Jar? = null


    fun config(): AbstractLanguageConfig<PT> {
        plugins().withType(pluginClass()) { plugin -> configLanguage() }
        return this
    }


    protected open fun configLanguage() {
        info("Configuring {}", pluginClass().simpleName)

        registerArtifacts(MavenPublishingConfig.mainPublication(project))
    }


    override fun registerArtifacts(publication: MavenPublication) {
        publication.artifact(docJarTask())
        artifacts().add("archives", docJarTask())
    }


    fun docJarTask(): Jar {
        if (docJarTask == null) {
            docJarTask = createDocJarTask()
        }
        return docJarTask as Jar
    }


    protected fun createDocJarTask(): Jar {
        val docTaskName = docTaskName()
        val docJarTask = tasks().create(docTaskName + "Jar", Jar::class.java)
        docJarTask.classifier = docTaskName
        docJarTask.from(docTask().outputs.files)
        return docJarTask
    }


    fun docTask(): Task {
        return tasks().getByName(docTaskName())
    }


    protected abstract fun docTaskName(): String


    protected abstract fun pluginClass(): Class<PT>


    fun jarTask(): Jar {
        return tasks().findByName(JAR_TASK_NAME) as Jar
    }


    fun compileTask(): AbstractCompile = tasks().getByName(compileTaskName()) as AbstractCompile


    protected abstract fun compileTaskName(): String

}
