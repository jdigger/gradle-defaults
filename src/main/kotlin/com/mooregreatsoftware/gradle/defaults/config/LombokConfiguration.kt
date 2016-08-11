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

import com.mooregreatsoftware.gradle.defaults.Ternary
import com.mooregreatsoftware.gradle.defaults.asTernary
import com.mooregreatsoftware.gradle.defaults.getConfiguration
import com.mooregreatsoftware.gradle.defaults.hasJavaBasePlugin
import com.mooregreatsoftware.gradle.defaults.hasJavaSource
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import java.io.File
import java.util.concurrent.Future

/**
 * Configures the project for [Lombok](https://projectlombok.org/features/index.html)
 */
// TODO Offer to auto-create lombok.config file
class LombokConfiguration protected constructor(project: Project) : AnnotationProcessorConfiguration(project) {

    override fun myProcessorClassNames(): Collection<String> {
        return listOf(LOMBOK_LAUNCH_ANNOTATION_PROCESSOR)
    }


    private fun processLibConf(): Configuration {
        return getConfiguration("lombok.processor.lib.conf", { it.add(lombokDependency()) }, project.configurations)
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
        return DefaultExternalModuleDependency("org.projectlombok", "lombok", project.lombokExtension().version)
    }

    companion object {

        private fun create(project: Project): Future<LombokConfiguration?> {
            return confFuture(project, "Lombok",
                { project.lombokExtension().enabled },
                { project.hasJavaBasePlugin() && project.hasJavaSource() },
                {
                    val conf = LombokConfiguration(project)
                    conf.configure(JavaConfig.of(project))
                    conf
                },
                "${project.name} has Java source files, so enabling Lombok support. " +
                    "To enable/disable explicitly, set `${LombokExtension.NAME}.enabled = true`"
            )
        }

        /**
         * Returns the [LombokConfiguration] if the project has Java source code and
         * [LombokExtension.enabled] has not been set to false. Otherwise will contain null.
         */
        @JvmStatic fun of(project: Project): Future<LombokConfiguration?> {
            return ofFuture(project, { create(project) })
        }

        const val LOMBOK_LAUNCH_ANNOTATION_PROCESSOR = "lombok.launch.AnnotationProcessorHider\$AnnotationProcessor"
    }

}

fun Project.lombokExtension(): LombokExtension = project.extensions.findByType(LombokExtension::class.java) as LombokExtension? ?:
    project.extensions.create(LombokExtension.NAME, LombokExtension::class.java)


/**
 * Configuration options for [LombokConfiguration]
 */
open class LombokExtension {
    var version = DEFAULT_LOMBOK_VERSION

    private var _useLombok = Ternary.MAYBE

    /**
     * Is Lombok enabled? Defaults to MAYBE and will try to auto-detect support.
     * See [LombokConfiguration.of]
     */
    val enabled: Ternary
        get() = _useLombok

    @Suppress("unused")
    fun setEnabled(useLombok: Any) {
        _useLombok = useLombok.asTernary()
    }


    override fun toString(): String = "LombokExtension(version='$version', _useLombok=$_useLombok)"


    companion object {
        /**
         * The name to register this under as a Gradle extension.
         */
        const val NAME = "lombok"

        const val DEFAULT_LOMBOK_VERSION = "1.16.8"
    }
}
