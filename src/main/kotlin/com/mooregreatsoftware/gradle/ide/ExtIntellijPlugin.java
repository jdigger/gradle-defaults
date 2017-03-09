/*
 * Copyright 2014-2017 the original author or authors.
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
package com.mooregreatsoftware.gradle.ide;

import com.mooregreatsoftware.gradle.util.JavacUtils;
import com.mooregreatsoftware.gradle.util.xml.NodeBuilder;
import com.mooregreatsoftware.gradle.java.ExtJavaPlugin;
import com.mooregreatsoftware.gradle.util.xml.XmlUtilsKt;
import groovy.util.Node;
import groovy.util.NodeList;
import javaslang.collection.List;
import lombok.val;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.XmlProvider;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.plugins.ide.idea.model.IdeaModel;
import org.gradle.plugins.ide.idea.model.IdeaProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.mooregreatsoftware.gradle.util.JavacUtils.PATH_SEPARATOR;
import static com.mooregreatsoftware.gradle.util.JavacUtils.getMutableAnnotationProcessorClassNames;
import static com.mooregreatsoftware.gradle.util.JavacUtils.getMutableAnnotationProcessorLibFiles;
import static com.mooregreatsoftware.gradle.defaults.DefaultsExtensionKt.defaultsExtension;
import static com.mooregreatsoftware.gradle.util.xml.XmlUtilsKt.appendChild;
import static com.mooregreatsoftware.gradle.util.xml.XmlUtilsKt.findByAttribute;
import static com.mooregreatsoftware.gradle.util.xml.XmlUtilsKt.getOrCreate;
import static com.mooregreatsoftware.gradle.util.xml.XmlUtilsKt.n;
import static com.mooregreatsoftware.gradle.ide.CodeStyleExtension.codeStyleExtension;
import static com.mooregreatsoftware.gradle.util.ProjectUtilsKt.allJavaProjects;

@SuppressWarnings({"Convert2MethodRef", "SameParameterValue"})
public class ExtIntellijPlugin implements Plugin<Project> {
    private static final Logger LOG = LoggerFactory.getLogger(ExtIntellijPlugin.class);
    public static final String PLUGIN_ID = "com.mooregreatsoftware.idea";


    @Override
    public void apply(Project project) {
        project.getPlugins().apply("idea");

        // everything below this is only at the (top-level) "Project" level, not the "Module" level
        if (project == project.getRootProject()) {
            LOG.info("Configuring the 'idea' plugin");

            codeStyleExtension(project);
            val ideaProject = ideaProject(project);
            ideaProject.setVcs("Git");

            ideaProject.getIpr().withXml(xmlProvider -> customizeProjectXml(project, xmlProvider));

            project.getPlugins().withType(JavaBasePlugin.class, plugin -> configLanguageVersion(project));
        }
    }


    private void configLanguageVersion(Project project) {
        val ideaProject = ideaProject(project);
        val compatibilityVersion = defaultsExtension(project).getCompatibilityVersion();
        ideaProject.setJdkName(compatibilityVersion.toString());
        ideaProject.setLanguageLevel(compatibilityVersion);
    }


    private static IdeaProject ideaProject(Project project) {
        return ideaModel(project).getProject();
    }


    private static void customizeProjectXml(Project project, XmlProvider provider) {
        val rootNode = provider.asNode();

        addGradleHome(project, rootNode);

        setupCodeStyle(rootNode, String.valueOf(codeStyleExtension(project).getIndentSize()));
        setupCompiler(project, rootNode);
    }


    public static void setupCompiler(Project project, Node node) {
        val componentNodes = componentNodes(node);

        val compilerConfiguration = findByAttribute(componentNodes, "name", "CompilerConfiguration",
            () -> node.appendNode("component", mapOf("name", "CompilerConfiguration")));

        val annotationProcessing = getOrCreate(compilerConfiguration, "annotationProcessing",
            () -> compilerConfiguration.appendNode("annotationProcessing"));

        project.getPlugins().withId(ExtJavaPlugin.PLUGIN_ID, it -> {
            val profile = createProfileNode(project, annotationProcessing);

            final List<File> bootClasspath = List.ofAll(JavacUtils.getMutableBootClasspath(project));
            if (!bootClasspath.isEmpty()) {
                val javacSettings = findByAttribute(componentNodes, "name", "JavacSettings",
                    () -> node.appendNode("component", mapOf("name", "JavacSettings"))
                );
                javacSettings.appendNode("option",
                    mapOf("name", "ADDITIONAL_OPTIONS_STRING",
                        "value", "-Xbootclasspath/p:" +
                            bootClasspath.map(file -> file.getAbsolutePath()).mkString(PATH_SEPARATOR)));
            }

            allJavaProjects(project).forEach(prj -> profile.appendNode("module", mapOf("name", prj.getName())));
        });
    }


    private static NodeBuilder fileEntry(File file) {
        return n("entry", mapOf("name", file.getAbsolutePath()));
    }


    private static Node createProfileNode(Project project, Node node) {
        val profileAttrs = mapOf("default", "true", "name", "AnnotationProcessors", "enabled", "true");

        final List<NodeBuilder> configurationFilesAsNodeBuilders = List.ofAll(getMutableAnnotationProcessorLibFiles(project)).
            sortBy(file -> file.getName()).
            map(file -> fileEntry(file));

        val annotationProcessorOptions = JavacUtils.getMutableAnnotationProcessorOptions(project);

        val nodeBuilder = n("processorPath", mapOf("useClasspath", "false"), configurationFilesAsNodeBuilders);
        val profileChildren = List.ofAll(getMutableAnnotationProcessorClassNames(project)).map(it -> n("processor", mapOf("name", it))).append(
            nodeBuilder).appendAll(List.ofAll(annotationProcessorOptions).map(option -> n("option", mapOf("name", option.name, "value", option.value))));

        return XmlUtilsKt.appendChildren(node, "profile", profileAttrs, profileChildren);
    }


    private static Node addGradleHome(Project project, Node rootNode) {
        return appendChild(rootNode, "component", mapOf("name", "GradleSettings"),
            n("option", nv("SDK_HOME", project.getGradle().getGradleHomeDir().toString())));
    }


    private static Map<String, String> nv(String name, String value) {
        return mapOf("name", name, "value", value);
    }


    private static Map<String, String> mapOf(String name, String value) {
        val map = new HashMap<String, String>();
        map.put(name, value);
        return map;
    }


    private static Map<String, String> mapOf(String name1, String value1, String name2, String value2) {
        val map = new HashMap<String, String>();
        map.put(name1, value1);
        map.put(name2, value2);
        return map;
    }


    private static Map<String, String> mapOf(String name1, String value1, String name2, String value2, String name3, String value3) {
        val map = new HashMap<String, String>();
        map.put(name1, value1);
        map.put(name2, value2);
        map.put(name3, value3);
        return map;
    }


    private static final String maxIntStr = String.valueOf(Integer.MAX_VALUE);


    private static void setupCodeStyle(Node node, String indentSize) {
        val codeStyleNode = codeStyleNode(node);
        codeStyleNode.setValue(new ArrayList()); // remove any previous children

        appendChild(codeStyleNode, "option", nv("USE_PER_PROJECT_SETTINGS", "true"));
        appendChild(codeStyleNode, "option", mapOf("name", "PER_PROJECT_SETTINGS"),
            n("value",
                List.of(
                    n("option", mapOf("name", "OTHER_INDENT_OPTIONS"),
                        n("value", List.of(
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
                    n("GroovyCodeStyleSettings", List.of(
                        n("option", nv("CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND", maxIntStr)),
                        n("option", nv("NAMES_COUNT_TO_USE_IMPORT_ON_DEMAND", maxIntStr)))),
                    n("JetCodeStyleSettings", List.of(
                        n("option", nv("NAME_COUNT_TO_USE_STAR_IMPORT", maxIntStr)),
                        n("option", nv("NAME_COUNT_TO_USE_STAR_IMPORT_FOR_MEMBERS", maxIntStr)),
                        n("option", mapOf("name", "PACKAGES_TO_USE_STAR_IMPORTS"),
                            n("value",
                                n("package",
                                    mapOf("name", "kotlinx.android.synthetic",
                                        "withSubpackages", "true",
                                        "static", "false")
                                )
                            )
                        )
                        )
                    )
                ).
                    appendAll(
                        List.of("Groovy", "JAVA", "Scala", "kotlin").map(lang ->
                            n("codeStyleSettings", mapOf("language", lang),
                                List.of(
                                    n("option", nv("BLANK_LINES_AROUND_METHOD", "2")),
                                    n("option", nv("ELSE_ON_NEW_LINE", "true")),
                                    n("option", nv("FINALLY_ON_NEW_LINE", "true")),
                                    n("option", nv("CATCH_ON_NEW_LINE", "true")),
                                    n("option", nv("FINALLY_ON_NEW_LINE", "false")),
                                    n("option", nv("SPACE_AFTER_TYPE_CAST", "false")),
                                    n("option", nv("INDENT_SIZE", "2")),
                                    n("option", nv("TAB_SIZE", indentSize)),
                                    n("option", nv("CONTINUATION_INDENT_SIZE", indentSize)),
                                    n("indentOptions",
                                        n("option", nv("CONTINUATION_INDENT_SIZE", indentSize)))
                                )
                            ))
                    )
            )
        );
    }


    private static Node codeStyleNode(Node node) {
        val componentNodes = componentNodes(node);
        return findByAttribute(componentNodes, "name", "ProjectCodeStyleSettingsManager",
            () -> node.appendNode("component", mapOf("name", "ProjectCodeStyleSettingsManager"))
        );
    }


    private static NodeList componentNodes(Node node) {
        return (NodeList)node.get("component");
    }


    private static IdeaModel ideaModel(Project project) {
        return project.getExtensions().findByType(IdeaModel.class);
    }

}
