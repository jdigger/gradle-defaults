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

import com.mooregreatsoftware.gradle.defaults.allJavaProjects
import com.mooregreatsoftware.gradle.defaults.defaultsExtension
import com.mooregreatsoftware.gradle.defaults.hasItems
import com.mooregreatsoftware.gradle.defaults.isRootProject
import com.mooregreatsoftware.gradle.defaults.maxIntStr
import com.mooregreatsoftware.gradle.defaults.xml.NodeBuilder
import com.mooregreatsoftware.gradle.defaults.xml.appendChild
import com.mooregreatsoftware.gradle.defaults.xml.appendChildren
import com.mooregreatsoftware.gradle.defaults.xml.findByAttribute
import com.mooregreatsoftware.gradle.defaults.xml.getOrCreate
import com.mooregreatsoftware.gradle.defaults.xml.n
import groovy.util.Node
import groovy.util.NodeList
import org.gradle.api.Project
import org.gradle.api.XmlProvider
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.gradle.plugins.ide.idea.model.IdeaProject
import java.io.File
import java.util.ArrayList
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class IntellijConfig
private constructor(private val project: Project,
                    private val javaConfigFuture: Future<JavaConfig?>) {

    init {
        project.plugins.apply("idea")

        // everything below this is only at the (top-level) "Project" level, not the "Module" level
        if (project.isRootProject()) {
            project.logger.info("Configuring the 'idea' plugin")

            project.codeStyleExtension()
            val ideaProject = ideaProject()
            ideaProject.vcs = "Git"

            ideaProject.ipr.withXml { customizeProjectXml(it) }

            project.plugins.withType(JavaBasePlugin::class.java) { configLanguageVersion() }
        }
    }


    private fun configLanguageVersion() {
        val ideaProject = ideaProject()
        val compatibilityVersion = project.defaultsExtension().compatibilityVersion
        ideaProject.jdkName = compatibilityVersion.toString()
        ideaProject.setLanguageLevel(compatibilityVersion)
    }


    fun ideaProject(): IdeaProject = project.ideaModel().project


    private fun customizeProjectXml(provider: XmlProvider) {
        val rootNode = provider.asNode()

        addGradleHome(rootNode)

        rootNode.setupCodeStyle(project.codeStyleExtension().indentSize.toString())
        rootNode.setupCompiler()
    }


    fun Node.setupCompiler() {
        val componentNodes = this.componentNodes()

        val compilerConfiguration = componentNodes.findByAttribute("name", "CompilerConfiguration") {
            this.appendNode("component", mapOf("name" to "CompilerConfiguration"))
        }

        val annotationProcessing = compilerConfiguration.getOrCreate("annotationProcessing") {
            compilerConfiguration.appendNode("annotationProcessing")
        }

        val javaConfig = javaConfigFuture.get(1, TimeUnit.SECONDS)
        if (javaConfig != null) {
            val profile = annotationProcessing.createProfileNode(javaConfig)

            if (javaConfig.bootClasspath.hasItems()) {
                val javacSettings = componentNodes.findByAttribute("name", "JavacSettings") {
                    this.appendNode("component", mapOf("name" to "JavacSettings"))
                }
                javacSettings.appendNode("option",
                    mapOf("name" to "ADDITIONAL_OPTIONS_STRING",
                        "value" to "-Xbootclasspath/p:" +
                            javaConfig.bootClasspath.map { it.absolutePath }.joinToString(PATH_SEPARATOR)))
            }

            project.allJavaProjects().forEach { prj -> profile.appendNode("module", mapOf("name" to prj.name)) }
        }
    }


    private fun Node.createProfileNode(javaConfig: JavaConfig): Node {
        val profileAttrs = mapOf("default" to "true", "name" to "AnnotationProcessors", "enabled" to "true")

        fun fileEntry(file: File): NodeBuilder = n("entry", mapOf("name" to file.absolutePath))

        val configurationFilesAsNodeBuilders = javaConfig.annotationProcessorLibFiles.sortedBy { it.name }.map { fileEntry(it) }.toList()

        val profileChildren = javaConfig.annotationProcessorClassNames.map { n("processor", mapOf("name" to it)) } +
            n("processorPath", mapOf("useClasspath" to "false"), configurationFilesAsNodeBuilders) +
            javaConfig.annotationProcessorOptions.map { n("option", mapOf("name" to it.name, "value" to it.value)) }

        return this.appendChildren("profile", profileAttrs,
            profileChildren)
    }

    private fun addGradleHome(rootNode: Node) {
        rootNode.appendChild("component", mapOf("name" to "GradleSettings"),
            n("option", nv("SDK_HOME", project.gradle.gradleHomeDir.toString())))
    }


    companion object {

        /**
         * Returns the [IntellijConfig] for in the given Project.
         *
         * @param project the project containing the IntellijConfig
         */
        @JvmStatic fun of(project: Project): Future<IntellijConfig?> {
            return ofInstance(project, { IntellijConfig(project, JavaConfig.of(project)) })
        }
    }
}


// **************************************************************************
//
// PRIVATE FUNCTIONS
//
// **************************************************************************


private fun nv(name: String, value: String) = mapOf("name" to name, "value" to value)

private fun Node.setupCodeStyle(indentSize: String) {
    val codeStyleNode = this.codeStyleNode()
    codeStyleNode.setValue(ArrayList<Any>()) // remove any previous children

    codeStyleNode.appendChild("option", nv("USE_PER_PROJECT_SETTINGS", "true"))
    codeStyleNode.appendChild("option", mapOf("name" to "PER_PROJECT_SETTINGS"),
        n("value",
            listOf(
                n("option", mapOf("name" to "OTHER_INDENT_OPTIONS"),
                    n("value", listOf(
                        n("option", nv("INDENT_SIZE", indentSize)),
                        n("option", nv("CONTINUATION_INDENT_SIZE", indentSize)),
                        n("option", nv("TAB_SIZE", indentSize)),
                        n("option", nv("USE_TAB_CHARACTER", "false")),
                        n("option", nv("SMART_TABS", "false")),
                        n("option", nv("LABEL_INDENT_SIZE", "0")),
                        n("option", nv("LABEL_INDENT_ABSOLUTE", "false")),
                        n("option", nv("USE_RELATIVE_INDENTS", "false"))))),
                n("option", nv("CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND", maxIntStr)),
                n("option", nv("NAMES_COUNT_TO_USE_IMPORT_ON_DEMAND", maxIntStr)),
                n("XML",
                    n("option", nv("XML_LEGACY_SETTINGS_IMPORTED", "true"))),
                n("GroovyCodeStyleSettings", listOf(
                    n("option", nv("CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND", maxIntStr)),
                    n("option", nv("NAMES_COUNT_TO_USE_IMPORT_ON_DEMAND", maxIntStr)))),
                n("JetCodeStyleSettings", listOf(
                    n("option", nv("NAME_COUNT_TO_USE_STAR_IMPORT", maxIntStr)),
                    n("option", nv("NAME_COUNT_TO_USE_STAR_IMPORT_FOR_MEMBERS", maxIntStr)),
                    n("option", mapOf("name" to "PACKAGES_TO_USE_STAR_IMPORTS"),
                        n("value",
                            n("package",
                                mapOf("name" to "kotlinx.android.synthetic",
                                    "withSubpackages" to "true",
                                    "static" to "false")
                            )
                        )
                    )
                ))
            ) +
                listOf("Groovy", "JAVA", "Scala", "kotlin").map { lang ->
                    n("codeStyleSettings", mapOf("language" to lang),
                        listOf(
                            n("option", nv("BLANK_LINES_AROUND_METHOD", "2")),
                            n("option", nv("ELSE_ON_NEW_LINE", "true")),
                            n("option", nv("CATCH_ON_NEW_LINE", "true")),
                            n("option", nv("FINALLY_ON_NEW_LINE", "false")),
                            n("option", nv("SPACE_AFTER_TYPE_CAST", "false")),
                            n("option", nv("INDENT_SIZE", "2")),
                            n("option", nv("TAB_SIZE", indentSize)),
                            n("option", nv("CONTINUATION_INDENT_SIZE", indentSize)),
                            n("indentOptions",
                                n("option", nv("CONTINUATION_INDENT_SIZE", indentSize)))
                        )
                    )
                }
        )
    )
}


private fun Node.codeStyleNode(): Node {
    val componentNodes = this.componentNodes()
    return componentNodes.findByAttribute("name", "ProjectCodeStyleSettingsManager") {
        this.appendNode("component", mapOf("name" to "ProjectCodeStyleSettingsManager"))
    }
}


private fun Node.componentNodes(): NodeList = this.get("component") as NodeList


private fun Project.ideaModel(): IdeaModel = this.extensions.findByType(IdeaModel::class.java)
