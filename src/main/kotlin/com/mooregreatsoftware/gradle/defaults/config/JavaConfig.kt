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
import com.mooregreatsoftware.gradle.defaults.hasItems
import com.mooregreatsoftware.gradle.defaults.isBuggyJavac
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPlugin.COMPILE_JAVA_TASK_NAME
import org.gradle.api.plugins.JavaPlugin.JAVADOC_TASK_NAME
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import java.io.File
import java.time.Instant
import java.util.ArrayList
import java.util.HashMap
import java.util.TreeSet

val PATH_SEPARATOR: String = System.getProperty("path.separator")

open class JavaConfig protected constructor(project: Project) : AbstractLanguageConfig<JavaPlugin>(project) {

    private var sourcesJarTask: Jar? = null

    private val _processorLibFiles = TreeSet<File>()
    private val _bootClasspath = TreeSet<File>()
    private val _annotationProcessorOptions = TreeSet<Option>()
    private val _annotationProcessorClassNames = TreeSet<String>()
    private val _javacOptions = TreeSet<String>()


    override fun configLanguage() {
        super.configLanguage()

        project.afterEvaluate { prj -> setManifestAttributes() } // TODO doFirst for task

        val tasks = project.tasks // TODO FIX
        tasks.withType(JavaCompile::class.java) { this.configureJavac(it) }
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


    private fun createSourcesJarTask(): Jar {
        val sourceJarTask = tasks().create(SOURCES_JAR_TASK_NAME, Jar::class.java)
        sourceJarTask.classifier = "sources"
        sourceJarTask.from(sourceSets().findByName("main").allSource as FileCollection)
        return sourceJarTask
    }


    override fun registerArtifacts(publication: MavenPublication) {
        super.registerArtifacts(publication)

        publication.artifact(sourcesJarTask())
        artifacts().add("archives", sourcesJarTask())
    }


    private fun setManifestAttributes() {
        debug("Setting MANIFEST.MF attributes")
        configureManifestAttributes(jarTask())
    }


    protected fun configureManifestAttributes(jarTask: Jar) {
        val attrs = manifestAttributes()

        jarTask.manifest.attributes(attrs)
    }


    protected fun manifestAttributes(): Map<String, String> {
        val attrs = HashMap<String, String>()
        attrs.put("Implementation-Title", description())
        attrs.put("Implementation-Version", version())
        attrs.put("Built-By", builtBy())
        attrs.put("Built-Date", Instant.now().toString())
        attrs.put("Built-JDK", System.getProperty("java.version") as String)
        attrs.put("Built-Gradle", gradle().gradleVersion)
        return attrs
    }


    protected open fun builtBy(): String {
        return DefaultsPlugin.userEmail(project)
    }


    override fun docTaskName(): String {
        return JAVADOC_TASK_NAME
    }


    override fun pluginClass(): Class<JavaPlugin> {
        return JavaPlugin::class.java
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

        override fun execute(javaCompileTask: Task) {
            val compilerArgs = createJavacArgs()
            val options = (javaCompileTask as JavaCompile).options

            if (isBuggyJavac) {
                val extensions = project.extensions
                val checkerConf = extensions.findByName(CheckerFrameworkConfiguration::class.java.name) as CheckerFrameworkConfiguration?
                if (checkerConf != null) {
                    options.isFork = true
                    options.forkOptions.jvmArgs = listOf("-Xbootclasspath/p:" + checkerConf.compilerLibraryFile().absolutePath)
                }
            }

            options.compilerArgs = compilerArgs
        }

    }


    class Option(val name: String, val value: String) : Comparable<Option> {
        override fun compareTo(other: Option): Int {
            val nameComp = name.compareTo(other.name)
            return if (nameComp != 0) nameComp else value.compareTo(other.value)
        }
    }

    companion object {
        val SOURCES_JAR_TASK_NAME = "sourcesJar"


        /**
         * Returns the JavaConfig for in the given Project.

         * @param project the project containing the JavaConfig
         */
        @JvmStatic fun of(project: Project): JavaConfig {
            val javaConfig = project.extensions.findByName(JavaConfig::class.java.name) as JavaConfig?
            return javaConfig ?: JavaConfig(project).config() as JavaConfig
        }

    }

}
