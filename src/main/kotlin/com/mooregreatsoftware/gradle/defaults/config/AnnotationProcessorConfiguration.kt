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
import org.gradle.api.artifacts.Dependency
import org.gradle.api.plugins.JavaPlugin
import java.io.File
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

abstract class AnnotationProcessorConfiguration
protected constructor(protected val project: Project) {

    protected fun configure(javaConfig: Future<JavaConfig?>) {
        configJavaPlugin(javaConfig)
    }


    protected fun configJavaPlugin(javaConfig: Future<JavaConfig?>) {
        project.plugins.withType(JavaPlugin::class.java) {
            registerWithJavac(javaConfig.get(1, TimeUnit.SECONDS)!!)
            addCompileOnlyDependencies()
        }
    }


    /**
     * @see JavaConfig.registerAnnotationProcessorLibFiles
     * @see JavaConfig.registerAnnotationProcessorClassnames
     * @see JavaConfig.registerAnnotationProcessorOptions
     * @see JavaConfig.registerBootClasspath
     */
    protected open fun registerWithJavac(javaConfig: JavaConfig) {
        javaConfig.registerAnnotationProcessorLibFiles(myProcessorLibFiles())
        javaConfig.registerAnnotationProcessorClassnames(myProcessorClassNames())
    }


    /**
     * Returns the fully-qualified class names of the annotation processors to use.
     */
    protected abstract fun myProcessorClassNames(): Collection<String>


    /**
     * Returns the files for the annotation processor libraries.
     */
    protected abstract fun myProcessorLibFiles(): Collection<File>


    protected abstract fun addCompileOnlyDependencies()

    companion object {

        @JvmStatic fun addCompileOnlyDependency(project: Project, checkerDep: Dependency) {
            project.configurations.getByName(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME).dependencies.add(checkerDep)
        }

    }

}
