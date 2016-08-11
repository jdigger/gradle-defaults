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

import com.mooregreatsoftware.gradle.defaults.DefaultsPlugin
import com.mooregreatsoftware.gradle.defaults.defaultsExtension
import com.mooregreatsoftware.gradle.defaults.hasItems
import com.mooregreatsoftware.gradle.defaults.postEvalCreate
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPlugin.COMPILE_JAVA_TASK_NAME
import org.gradle.api.plugins.JavaPlugin.JAVADOC_TASK_NAME
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.JavaCompile
import java.io.File
import java.time.Instant
import java.util.ArrayList
import java.util.HashMap
import java.util.TreeSet
import java.util.concurrent.Future
import java.util.concurrent.TimeoutException

val PATH_SEPARATOR: String = System.getProperty("path.separator")

open class JavaConfig protected constructor(project: Project) : AbstractLanguageConfig(project) {

    private var sourcesJarTask: Jar? = null

    private val _processorLibFiles = TreeSet<File>()
    private val _bootClasspath = TreeSet<File>()
    private val _annotationProcessorOptions = TreeSet<Option>()
    private val _annotationProcessorClassNames = TreeSet<String>()
    private val _javacOptions = TreeSet<String>()


    override fun configLanguage(pluginClassname: String) {
        super.configLanguage(pluginClassname)

        project.postEvalCreate {
            setManifestAttributes() // TODO doFirst for task
        }

        project.tasks.withType(JavaCompile::class.java) { configureJavac(it) }

        project.tasks.withType(AbstractCompile::class.java) { task ->
            task.doFirst {
                val defaults = project.defaultsExtension()
                task.sourceCompatibility = defaults.compatibilityVersion.toString()
                task.targetCompatibility = defaults.compatibilityVersion.toString()
            }
        }
    }


    private fun configureJavac(jcTask: JavaCompile): Task {
        return jcTask.doFirst(ConfigCompilerAction())
    }


    fun sourcesJarTask(): Jar {
        if (sourcesJarTask == null) {
            sourcesJarTask = createSourcesJarTask()
        }
        return sourcesJarTask as Jar
    }


    /**
     * This will pick up all sources, regardless of language or resource location,
     * as long as they use the "main" [SourceSetContainer].
     */
    private fun createSourcesJarTask(): Jar {
        val sourceJarTask = project.tasks.create(SOURCES_JAR_TASK_NAME, Jar::class.java)
        sourceJarTask.classifier = "sources"
        val ss = project.sourceSets!!
        sourceJarTask.from(ss.findByName("main").allSource as FileCollection)
        return sourceJarTask
    }


    override fun registerArtifacts(publication: MavenPublication) {
        super.registerArtifacts(publication)

        val mainJarTask = project.tasks.getByName("jar") as Jar
        publication.artifact(mainJarTask)
        project.artifacts.add("archives", mainJarTask)

        publication.artifact(sourcesJarTask())
        project.artifacts.add("archives", sourcesJarTask())
    }


    private fun setManifestAttributes() {
        project.logger.debug("Setting MANIFEST.MF attributes")
        configureManifestAttributes(jarTask())
    }


    protected fun configureManifestAttributes(jarTask: Jar) {
        val attrs = manifestAttributes()

        jarTask.manifest.attributes(attrs)
    }


    protected fun manifestAttributes(): Map<String, String> {
        val attrs = HashMap<String, String>()
        attrs.put("Implementation-Title", project.description ?: project.name)
        attrs.put("Implementation-Version", project.version.toString())
        attrs.put("Built-By", builtBy())
        attrs.put("Built-Date", Instant.now().toString())
        attrs.put("Built-JDK", System.getProperty("java.version") as String)
        attrs.put("Built-Gradle", project.gradle.gradleVersion)
        return attrs
    }


    protected open fun builtBy(): String {
        return DefaultsPlugin.userEmail(project)
    }


    override fun docTaskName(): String {
        return JAVADOC_TASK_NAME
    }


    override fun compileTaskName(): String {
        return COMPILE_JAVA_TASK_NAME
    }


    val annotationProcessorLibFiles: Iterable<File>
        get() = this._processorLibFiles


    fun registerAnnotationProcessorLibFiles(files: Collection<File>) {
        this._processorLibFiles.addAll(files)
    }


    val annotationProcessorClassNames: Iterable<String>
        get() = this._annotationProcessorClassNames


    fun registerAnnotationProcessorClassnames(classnames: Collection<String>) {
        this._annotationProcessorClassNames.addAll(classnames)
    }


    val bootClasspath: Iterable<File>
        get() = this._bootClasspath


    fun registerBootClasspath(files: Collection<File>) {
        this._bootClasspath.addAll(files)
    }


    val annotationProcessorOptions: Iterable<Option>
        get() = this._annotationProcessorOptions


    /**
     * Register annotation processor arguments. Do not include the "-A". (e.g., instead of "-Awarn" use "warn")
     */
    fun registerAnnotationProcessorOptions(options: Collection<Option>) {
        fun stripLeadingDashA(o: JavaConfig.Option): JavaConfig.Option = when {
            o.name.startsWith("-A") -> JavaConfig.Option(o.name.substring(2), o.value)
            else -> o
        }

        options.map { stripLeadingDashA(it) }.forEach { e -> this._annotationProcessorOptions.add(e) }
    }


    val javacOptions: Iterable<String>
        get() = this._javacOptions


    /**
     * Register "raw" javac arguments.
     */
    fun registerJavacOptions(options: Collection<String>) {
        this._javacOptions.addAll(options)
    }

    protected fun createJavacArgs(): List<String> {
        val compilerArgs = ArrayList<String>()

        addProcessor(compilerArgs)
        addProcessorPath(compilerArgs)
        addAnnotationProcessorOptions(compilerArgs)
        registerJavacOptions(listOf("-Xlint:unchecked"))
        addOtherCompilerArgs(compilerArgs)
        addBootClasspath(compilerArgs)

        return compilerArgs
    }


    private fun addBootClasspath(compilerArgs: MutableList<String>) {
        if (bootClasspath.hasItems()) {
            compilerArgs.add("-Xbootclasspath/p:" + bootClasspath.map { it.absolutePath }.joinToString(PATH_SEPARATOR))
        }
    }


    private fun addProcessor(compilerArgs: MutableList<String>) {
        compilerArgs.add("-processor")
        compilerArgs.add(annotationProcessorClassNames.joinToString(","))
    }


    private fun addProcessorPath(compilerArgs: MutableList<String>) {
        compilerArgs.add("-processorpath")
        compilerArgs.add(
            annotationProcessorLibFiles.map { it.absolutePath }.sorted().joinToString(PATH_SEPARATOR))
    }


    private fun addAnnotationProcessorOptions(compilerArgs: MutableList<String>) {
        annotationProcessorOptions.map { "-A" + it.name + "=" + it.value }.forEach { compilerArgs.add(it) }
    }


    private fun addOtherCompilerArgs(compilerArgs: MutableList<String>) {
        javacOptions.forEach { compilerArgs.add(it) }
    }

    // **********************************************************************
    //
    // HELPER CLASSES
    //
    // **********************************************************************

    protected inner class ConfigCompilerAction : Action<Task> {

        @Throws(TimeoutException::class)
        override fun execute(javaCompileTask: Task) {
            val compilerArgs = createJavacArgs()
            val javaCompile = javaCompileTask as JavaCompile
            val options = javaCompile.options

            options.compilerArgs = compilerArgs
        }

    }


    data class Option(val name: String, val value: String) : Comparable<Option> {
        override fun compareTo(other: Option): Int {
            val nameComp = name.compareTo(other.name)
            return if (nameComp != 0) nameComp else value.compareTo(other.value)
        }
    }

    companion object {
        val SOURCES_JAR_TASK_NAME = "sourcesJar"


        /**
         * Returns the JavaConfig for in the given Project.
         *
         * @param project the project containing the JavaConfig
         */
        @JvmStatic fun of(project: Project): Future<JavaConfig?> {
            return ofInstance(project, { JavaConfig(project).config(JavaPlugin::class.java) as JavaConfig })
        }

    }

}

val Project.sourceSets: SourceSetContainer?
    get() = convention.findPlugin(JavaPluginConvention::class.java)?.sourceSets
