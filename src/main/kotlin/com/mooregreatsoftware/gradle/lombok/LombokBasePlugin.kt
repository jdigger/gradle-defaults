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
package com.mooregreatsoftware.gradle.lombok

import com.mooregreatsoftware.gradle.annotationprocessor.AbstractAnnotationProcessorPlugin
import com.mooregreatsoftware.gradle.defaults.config.LombokConfiguration
import com.mooregreatsoftware.gradle.defaults.getConfiguration
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.Dependency
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import java.io.File

class LombokBasePlugin : AbstractAnnotationProcessorPlugin() {

    var versionSupplier = { LombokConfiguration.DEFAULT_LOMBOK_VERSION }


    override fun apply(project: Project) {
        super.apply(project)
    }


    override fun myProcessorClassNames(): Collection<String> {
        return listOf(LombokConfiguration.LOMBOK_LAUNCH_ANNOTATION_PROCESSOR)
    }


    private fun processorLibConf(configurations: ConfigurationContainer): Configuration {
        return getConfiguration("lombok.processor.lib.conf", { it.add(lombokDependency()) },
            configurations)
    }


    override fun myProcessorLibFiles(configurations: ConfigurationContainer): Collection<File> {
        return processorLibConf(configurations).files
    }


    fun processorLibraryFile(configurations: ConfigurationContainer): File {
        return processorLibConf(configurations).singleFile
    }


    override fun addCompileOnlyDependencies(configurations: ConfigurationContainer) {
        //        addCompileOnlyDependency(project, lombokDependency());
    }


    private fun lombokDependency(): Dependency {
        return DefaultExternalModuleDependency("org.projectlombok", "lombok", versionSupplier())
    }

}
