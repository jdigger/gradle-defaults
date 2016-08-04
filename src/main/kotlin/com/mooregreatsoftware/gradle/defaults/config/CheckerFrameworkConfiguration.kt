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

import com.mooregreatsoftware.gradle.defaults.config.JavaConfig.Option
import com.mooregreatsoftware.gradle.defaults.getConfiguration
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import java.io.File
import java.util.Arrays.asList
import java.util.function.Supplier

@SuppressWarnings("WeakerAccess")
class CheckerFrameworkConfiguration protected constructor(project: Project, lombokVersionSupplier: Supplier<String>) : AnnotationProcessorConfiguration(project, lombokVersionSupplier) {

    override fun myProcessorClassNames(): Collection<String> {
        return setOf(CHECKERFRAMEWORK_NULLNESS_CHECKER)
    }


    override fun registerWithJavac(javaConfig: JavaConfig) {
        super.registerWithJavac(javaConfig)

        // "skipUses=javaslang"
        // remove warns to turn the checks into errors
        javaConfig.registerAnnotationProcessorOptions(
            asList(Option("warns", "true"), Option("lint", "-cast:unsafe")))

        javaConfig.registerBootClasspath(bootClasspathFiles())
    }


    private fun bootClasspathFiles(): Set<File> {
        return getConfiguration("checkerframework.bootclasspath.lib.conf",
            { deps ->
                deps.add(DefaultExternalModuleDependency("org.checkerframework", "jdk8", versionSupplier.get()))
            },
            project.configurations).files
    }


    private fun processLibConf(): Configuration {
        return getConfiguration("checkerframework.processor.lib.conf",
            { deps ->
                deps.add(DefaultExternalModuleDependency("org.checkerframework", "checker", versionSupplier.get()))
            },
            project.configurations)
    }


    fun processorLibraryFile(): File {
        return processLibConf().singleFile
    }


    fun compilerLibraryFile(): File {
        return getConfiguration("checkerframework.compiler.lib.conf",
            { deps ->
                deps.add(DefaultExternalModuleDependency("org.checkerframework", "compiler", versionSupplier.get()))
            },
            project.configurations).singleFile
    }


    override fun myProcessorLibFiles(): Collection<File> {
        return processLibConf().files
    }


    override fun addCompileOnlyDependencies() {
        val dep = DefaultExternalModuleDependency("org.checkerframework", "checker-qual", versionSupplier.get())
        AnnotationProcessorConfiguration.addCompileOnlyDependency(project, dep)
    }

    companion object {
        @JvmStatic val DEFAULT_CHECKER_VERSION = "2.0.1"

        @JvmStatic protected val CHECKERFRAMEWORK_NULLNESS_CHECKER = "org.checkerframework.checker.nullness.NullnessChecker"


        /**
         * Creates a new instance of [CheckerFrameworkConfiguration].
         *
         * @param project                the Project to create it against
         *
         * @param checkerVersionSupplier a [Supplier] of the version of Checker Framework to use
         *
         * @see DEFAULT_CHECKER_VERSION
         */
        @JvmStatic fun create(project: Project, checkerVersionSupplier: Supplier<String>,
                              javaConfig: JavaConfig): CheckerFrameworkConfiguration {
            val checkerConfiguration = CheckerFrameworkConfiguration(project, checkerVersionSupplier)

            checkerConfiguration.configure(javaConfig)

            return checkerConfiguration
        }
    }

}
