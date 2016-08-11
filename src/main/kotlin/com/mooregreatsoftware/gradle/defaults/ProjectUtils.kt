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
package com.mooregreatsoftware.gradle.defaults

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.plugins.Convention
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * Does the given Project have any Java source code under it?
 *
 * If the project applies the Java plugin, that does not necessarily mean it has Java source, since it may
 * be another JVM language, source as Groovy or Scala. So its SourceSets are checked for .java files.
 *
 * If the project does not apply the Java plugin, the existence of .java files is still checked in the "standard"
 * locations: src/main/java, src/main/groovy, src/main/kotlin, src/main/scala.
 */
fun Project.hasJavaSource(useCache: Boolean = true): Boolean {
    val key = "project.hasJavaSource"
    val ext = this.extensions.extraProperties

    if (useCache) {
        if (ext.has(key)) {
            return ext.get(key) as Boolean
        }
    }

    try {
        val hasJavaSrc = when {
            hasJavaPlugin(project) -> hasJavaSourceWithJavaPlugin(this.convention)
            else -> {
                val foundJavaFile = hasJavaSourceWithoutJavaPlugin(this.projectDir)
                if (foundJavaFile) {
                    this.logger.warn("Found Java source files in a standard source directory, " +
                        "but the Java plugin has not been applied")
                }
                foundJavaFile
            }
        }
        if (useCache) ext.set(key, hasJavaSrc)
        return hasJavaSrc
    }
    catch (exp: IOException) {
        project.logger.error("Could not verify if " + this.name + " has Java source", exp)
        if (useCache) ext.set(key, false)
        return false
    }
}


fun Project.hasJavaBasePlugin() = this.plugins.hasPlugin(JavaBasePlugin::class.java)


/**
 * Returns all of the projects that have the Java plugin and have Java source files.
 */
fun Project.allJavaProjects(): Iterable<Project> =
    this.rootProject.allprojects.filter({ hasJavaPlugin(it) }).filter({ it.hasJavaSource() })

fun Project.isRootProject() = this == this.rootProject

@Throws(IOException::class)
private fun hasJavaSourceWithoutJavaPlugin(projectDir: File): Boolean {
    val srcMain = projectDir.toPath().resolve("src").resolve("main")
    return when {
        Files.exists(srcMain) && Files.isDirectory(srcMain) -> hasJavaInStdSrcMain(srcMain)
        else -> false
    }
}

private fun hasJavaInStdSrcMain(srcMain: Path) =
    pathHasAFile(srcMain.resolve("java"), { isJavaFile(it) }) ||
        pathHasAFile(srcMain.resolve("groovy"), { isJavaFile(it) }) ||
        pathHasAFile(srcMain.resolve("kotlin"), { isJavaFile(it) }) ||
        pathHasAFile(srcMain.resolve("scala"), { isJavaFile(it) })


@Throws(IOException::class)
private fun pathHasAFile(path: Path, matcher: (Path) -> Boolean) = when {
    Files.exists(path) -> when {
        Files.isDirectory(path) -> dirHasFile(path, matcher)
        else -> matcher(path)
    }
    else -> false
}


@Throws(IOException::class)
private fun dirHasFile(dirPath: Path, matcher: (Path) -> Boolean) =
    Files.walk(dirPath).filter(matcher).findAny().isPresent


private fun isJavaFile(p: Path) = p.fileName.toString().endsWith(".java")


private fun hasJavaSourceWithJavaPlugin(projectConvention: Convention): Boolean {
    val sourceSets = sourceSets(projectConvention) ?: return false
    return sourceSets.filter({ hasJavaSource(it) }).firstOrNull() != null
}


private fun hasJavaSource(ss: SourceSet): Boolean = !ss.allJava.isEmpty


fun sourceSets(projectConvention: Convention): SourceSetContainer? {
    val javaPluginConvention = projectConvention.findPlugin(JavaPluginConvention::class.java)
    return when {
        javaPluginConvention != null -> javaPluginConvention.sourceSets
        else -> null
    }
}


private fun hasJavaPlugin(prj: Project): Boolean {
    return prj.plugins.hasPlugin(JavaBasePlugin::class.java)
}


/**
 * Gets the named [Configuration] from the [Project]. If it does not yet exist, it is created and the
 * dependencies are configured using the [Consumer].
 *
 * @param confName             the name of the [Configuration]
 *
 * @param dependenciesConsumer used to set up the given [DependencySet] for the newly created [Configuration]
 *
 * @param configurations       container of [Configuration]s
 *
 * @return the named [Configuration]
 *
 * @see .createConfiguration
 */
fun getConfiguration(confName: String, dependenciesConsumer: (DependencySet) -> Unit,
                     configurations: ConfigurationContainer): Configuration {
    return configurations.findByName(confName) ?: createConfiguration(confName, dependenciesConsumer, configurations)
}


/**
 * Creates a new [Configuration] with the given name, configuring the dependencies using the [Consumer].
 *
 * @param confName             the name of the new [Configuration]
 *
 * @param dependenciesConsumer used to set up the given [DependencySet] for the newly created [Configuration]
 *
 * @param configurations       container of [Configuration]s
 *
 * @return the newly created [Configuration]
 *
 * @throws InvalidUserDataException if the Configuration by already exists
 */
@Throws(InvalidUserDataException::class)
fun createConfiguration(confName: String,
                        dependenciesConsumer: (DependencySet) -> Unit,
                        configurations: ConfigurationContainer): Configuration {
    return configurations.create(confName) { conf -> dependenciesConsumer(conf.dependencies) }
}

/**
 * Returns a Future that returns a value after the Project has been fully evaluated.
 *
 * @return a Future that will be evaluated after the project has been evaluated
 */
fun <T> Project.postEvalCreate(creator: () -> T): Future<T> {
    val latch = CountDownLatch(1)

    // The ForkJoinPool that is used by default for CompletableFuture is shared between unit testing executions,
    // causing race conditions. By creating an isolated execution environment, that is avoided.
    var executor = Executors.newSingleThreadExecutor()
    val async = CompletableFuture.supplyAsync(Supplier {
        // wait for the signal that the Project has been evaluated
        latch.await()
        creator()
    }, executor)

    // Adds a listener to the Project that will fire when it finishes evaluating.
    this.afterEvaluate {
        // signal the detection to run
        latch.countDown()
        // wait for detection to finish before letting Project execution to continue
        async.get(20, TimeUnit.SECONDS)
        // cleanup
        executor.shutdown()
        executor = null
    }

    return async
}
