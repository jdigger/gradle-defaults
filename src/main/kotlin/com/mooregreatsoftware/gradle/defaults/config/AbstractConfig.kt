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

import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.ArtifactHandler
import org.gradle.api.invocation.Gradle
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.plugins.PluginContainer
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskContainer

abstract class AbstractConfig
constructor(protected val project: Project) {
    init {
        project.extensions.add(this.javaClass.name, this)
    }


    protected fun debug(msg: String) {
        project.logger.debug(msg)
    }


    protected fun debug(format: String, vararg msgArgs: String) {
        project.logger.debug(format, *msgArgs)
    }


    protected fun info(msg: String) {
        project.logger.info(msg)
    }


    protected fun info(format: String, vararg msgArgs: String) = project.logger.info(format, *msgArgs)


    protected fun plugins(): PluginContainer = project.plugins


    protected fun gradle(): Gradle = project.gradle


    protected fun artifacts(): ArtifactHandler = project.artifacts


    protected fun tasks(): TaskContainer = project.tasks


    protected fun description(): String = project.description ?: name()


    protected fun version(): String = project.version.toString()


    protected fun name(): String = project.name


    protected fun ext(): ExtraPropertiesExtension = project.extensions.extraProperties


    protected fun sourceSets(): SourceSetContainer = project.convention.findPlugin(JavaPluginConvention::class.java).sourceSets

}
