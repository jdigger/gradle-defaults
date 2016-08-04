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

import com.mooregreatsoftware.gradle.defaults.getConfiguration
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import java.io.File
import java.util.function.Supplier

class LombokConfiguration protected constructor(project: Project, lombokVersionSupplier: Supplier<String>) : AnnotationProcessorConfiguration(project, lombokVersionSupplier) {


    override fun myProcessorClassNames(): Collection<String> {
        return listOf(LOMBOK_LAUNCH_ANNOTATION_PROCESSOR)
    }


    private fun processLibConf(): Configuration {
        return getConfiguration("lombok.processor.lib.conf", { deps -> deps.add(lombokDependency()) }, project.configurations)
    }


    override fun myProcessorLibFiles(): Collection<File> {
        return processLibConf().files
    }


    fun processorLibraryFile(): File {
        return processLibConf().singleFile
    }


    override fun addCompileOnlyDependencies() {
        AnnotationProcessorConfiguration.addCompileOnlyDependency(project, lombokDependency())
    }


    private fun lombokDependency(): Dependency {
        return DefaultExternalModuleDependency("org.projectlombok", "lombok", versionSupplier.get())
    }

    companion object {
        @JvmStatic val DEFAULT_LOMBOK_VERSION = "1.16.8"

        @JvmStatic val LOMBOK_LAUNCH_ANNOTATION_PROCESSOR = "lombok.launch.AnnotationProcessorHider\$AnnotationProcessor"


        @JvmStatic fun create(project: Project, lombokVersionSupplier: Supplier<String>,
                              javaConfig: JavaConfig): LombokConfiguration {
            val lombokConfig = LombokConfiguration(project, lombokVersionSupplier)

            lombokConfig.configure(javaConfig)

            return lombokConfig
        }
    }

}
