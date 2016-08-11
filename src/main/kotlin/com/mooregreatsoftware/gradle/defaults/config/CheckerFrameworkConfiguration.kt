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
import com.mooregreatsoftware.gradle.defaults.config.JavaConfig.Option
import com.mooregreatsoftware.gradle.defaults.getConfiguration
import com.mooregreatsoftware.gradle.defaults.hasJavaBasePlugin
import com.mooregreatsoftware.gradle.defaults.hasJavaSource
import com.mooregreatsoftware.gradle.defaults.isBuggyJavac
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import org.gradle.api.tasks.compile.JavaCompile
import java.io.File
import java.util.Arrays.asList
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Configures the project for [Checker Framework](types.cs.washington.edu/checker-framework/current/checker-framework-manual.html)
 */
class CheckerFrameworkConfiguration private constructor(project: Project, val version: String) : AnnotationProcessorConfiguration(project) {

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

        project.tasks.withType(JavaCompile::class.java) { this.configureJavac(it) }
    }


    private fun configureJavac(jcTask: JavaCompile): Task {
        return jcTask.doFirst(ConfigCompilerAction())
    }


    private fun bootClasspathFiles(): Set<File> {
        return getConfiguration("checkerframework.bootclasspath.lib.conf",
            { deps ->
                deps.add(DefaultExternalModuleDependency("org.checkerframework", "jdk8", version))
            },
            project.configurations).files
    }


    private fun processLibConf(): Configuration {
        return getConfiguration("checkerframework.processor.lib.conf",
            { deps ->
                deps.add(DefaultExternalModuleDependency("org.checkerframework", "checker", version))
            },
            project.configurations)
    }


    fun processorLibraryFile(): File {
        return processLibConf().singleFile
    }


    fun compilerLibraryFile(): File {
        return getConfiguration("checkerframework.compiler.lib.conf",
            { deps ->
                deps.add(DefaultExternalModuleDependency("org.checkerframework", "compiler", version))
            },
            project.configurations).singleFile
    }


    override fun myProcessorLibFiles(): Collection<File> {
        return processLibConf().files
    }


    override fun addCompileOnlyDependencies() {
        val dep = DefaultExternalModuleDependency("org.checkerframework", "checker-qual", version)
        AnnotationProcessorConfiguration.addCompileOnlyDependency(project, dep)
    }

    // **********************************************************************
    //
    // HELPER CLASSES
    //
    // **********************************************************************

    private inner class ConfigCompilerAction : Action<Task> {

        @Throws(TimeoutException::class)
        override fun execute(javaCompileTask: Task) {
            val options = (javaCompileTask as JavaCompile).options

            if (isBuggyJavac) {
                val checkerConfFuture = CheckerFrameworkConfiguration.of(project)
                val checkerConf = checkerConfFuture.get(1, TimeUnit.SECONDS)
                if (checkerConf != null) {
                    options.isFork = true
                    options.forkOptions.jvmArgs = listOf("-Xbootclasspath/p:" + checkerConf.compilerLibraryFile().absolutePath)
                }
            }
        }

    }

    companion object {
        const val CHECKERFRAMEWORK_NULLNESS_CHECKER = "org.checkerframework.checker.nullness.NullnessChecker"

        /**
         * Creates a new instance of [CheckerFrameworkConfiguration].
         *
         * @param project                the Project to create it against
         *
         * @see CheckerFrameworkExtension.DEFAULT_CHECKER_VERSION
         */
        @JvmStatic fun create(project: Project): Future<CheckerFrameworkConfiguration?> {
            val future = confFuture(project, "Checker Framework",
                { project.checkerFrameworkExtension().enabled },
                { project.hasJavaBasePlugin() && project.hasJavaSource() },
                {
                    val conf = CheckerFrameworkConfiguration(project, project.checkerFrameworkExtension().version)
                    conf.configure(JavaConfig.of(project))
                    conf
                },
                "${project.name} has Java source files, so enabling Checker Framework support. " +
                    "To enable/disable explicitly, set `${CheckerFrameworkExtension.NAME}.enabled = true`"
            )
            project.extensions.add(CheckerFrameworkConfiguration::class.java.name, future)
            return future
        }

        /**
         * Returns the [CheckerFrameworkConfiguration] if the project has Java source code and
         * [CheckerFrameworkExtension.enabled] has not been set to false. Otherwise will contain null.
         */
        @JvmStatic fun of(project: Project): Future<CheckerFrameworkConfiguration?> {
            @Suppress("UNCHECKED_CAST")
            val checkerConfigFuture =
                project.extensions.findByName(CheckerFrameworkConfiguration::class.java.name) as Future<CheckerFrameworkConfiguration?>?
            return when (checkerConfigFuture) {
                null -> create(project)
                else -> checkerConfigFuture
            }
        }
    }

}


/**
 * The [CheckerFrameworkExtension] for the project.
 */
fun Project.checkerFrameworkExtension(): CheckerFrameworkExtension =
    project.extensions.findByType(CheckerFrameworkExtension::class.java) as CheckerFrameworkExtension? ?:
        project.extensions.create(CheckerFrameworkExtension.NAME, CheckerFrameworkExtension::class.java)


/**
 * Configuration options for [CheckerFrameworkConfiguration]
 */
open class CheckerFrameworkExtension {
    /**
     * The version of the Checker Framework to use.
     */
    var version = DEFAULT_CHECKER_VERSION

    private var _useChecker = Ternary.MAYBE
    /**
     * Is Checker Framework enabled? Defaults to MAYBE and will try to auto-detect support.
     * See [CheckerFrameworkConfiguration.of]
     */
    val enabled: Ternary
        get() = _useChecker

    @Suppress("unused")
    fun setEnabled(useChecker: Any) {
        _useChecker = useChecker.asTernary()
    }


    override fun toString(): String = "CheckerFrameworkExtension(version='$version', _useChecker=$_useChecker)"


    companion object {
        /**
         * The name to register this under as a Gradle extension.
         */
        const val NAME = "checkerFramework"

        const val DEFAULT_CHECKER_VERSION = "2.0.1"
    }
}
