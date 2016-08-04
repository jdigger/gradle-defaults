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
import java.util.function.Consumer

/**
 * Does the given Project have any Java source code under it?
 *
 * If the project applies the Java plugin, that does not necessarily mean it has Java source, since it may
 * be another JVM language, source as Groovy or Scala. So its SourceSets are checked for .java files.
 *
 * If the project does not apply the Java plugin, the existence of .java files is still checked in the "standard"
 * locations: src/main/java, src/main/groovy, src/main/kotlin, src/main/scala.
 */
fun hasJavaSource(project: Project): Boolean {
    try {
        return when {
            hasJavaPlugin(project) -> hasJavaSourceWithJavaPlugin(project.convention)
            else -> {
                val foundJavaFile = hasJavaSourceWithoutJavaPlugin(project.projectDir)
                if (foundJavaFile) {
                    project.logger.warn("Found Java source files in a standard source directory, " +
                        "but the Java plugin has not been applied")
                }
                foundJavaFile
            }
        }
    }
    catch (exp: IOException) {
        project.logger.error("Could not verify if " + project.name + " has Java source", exp)
        return false
    }

}


/**
 * Returns all of the projects that have the Java plugin and have Java source files.
 *
 * @param project a handle into the Project structures, gets resolved to the root Project
 */
fun Project.allJavaProjects(): Iterable<Project> =
    this.rootProject.allprojects.filter({ hasJavaPlugin(it) }).filter({ hasJavaSource(it) })

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
