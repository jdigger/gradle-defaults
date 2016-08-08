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
package com.mooregreatsoftware.gradle.annotationprocessor

import com.mooregreatsoftware.gradle.defaults.config.JavaConfig
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.plugins.JavaBasePlugin
import java.io.File

abstract class AbstractAnnotationProcessorPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.plugins.apply(JavaBasePlugin::class.java)

        val configurations = project.configurations

        registerWithJavac(JavaConfig.of(project), configurations)
        addCompileOnlyDependencies(configurations)
    }


    /**
     * @see JavaConfig.registerAnnotationProcessorLibFiles
     * @see JavaConfig.registerAnnotationProcessorClassnames
     * @see JavaConfig.registerAnnotationProcessorOptions
     * @see JavaConfig.registerBootClasspath
     */
    protected fun registerWithJavac(javaConfig: JavaConfig, configurations: ConfigurationContainer) {
        javaConfig.registerAnnotationProcessorLibFiles(myProcessorLibFiles(configurations))
        javaConfig.registerAnnotationProcessorClassnames(myProcessorClassNames())
    }


    /**
     * Returns the fully-qualified class names of the annotation processors to use.
     */
    protected abstract fun myProcessorClassNames(): Collection<String>


    /**
     * Returns the files for the annotation processor libraries.
     */
    protected abstract fun myProcessorLibFiles(configurations: ConfigurationContainer): Collection<File>


    protected abstract fun addCompileOnlyDependencies(configurations: ConfigurationContainer)

}
