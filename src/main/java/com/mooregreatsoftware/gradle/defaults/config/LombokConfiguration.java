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
package com.mooregreatsoftware.gradle.defaults.config;

import com.mooregreatsoftware.gradle.defaults.xml.NodeBuilder;
import com.mooregreatsoftware.gradle.defaults.xml.XmlUtils;
import lombok.val;
import org.gradle.api.Project;
import org.gradle.api.XmlProvider;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.plugins.ide.idea.IdeaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.mooregreatsoftware.gradle.defaults.config.IntellijConfig.ideaProject;
import static com.mooregreatsoftware.gradle.defaults.xml.XmlUtils.findByAttribute;
import static com.mooregreatsoftware.gradle.defaults.xml.XmlUtils.m;
import static com.mooregreatsoftware.gradle.defaults.xml.XmlUtils.n;
import static java.util.Arrays.asList;

public class LombokConfiguration {
    private static final String LOMBOK_CONFIGURATION_NAME = "lombokArtifacts";
    private static final String LOMBOK_LAUNCH_ANNOTATION_PROCESSOR = "lombok.launch.AnnotationProcessorHider$AnnotationProcessor";


    private LombokConfiguration() {
    }


    public static LombokConfiguration create(Project project, Supplier<String> lombokVersionSupplier) {
        val lombokConfig = new LombokConfiguration();

        configJavaPlugin(project, lombokVersionSupplier);
        configIdeaPlugin(project);

        return lombokConfig;
    }


    private static void configJavaPlugin(Project project, Supplier<String> lombokVersionSupplier) {
        project.getPlugins().withType(JavaPlugin.class, plugin -> {
            addProcessorToJavacTask(project);
            project.afterEvaluate(prj -> addDependencies(prj, lombokVersionSupplier));
        });
    }


    private static void configIdeaPlugin(Project project) {
        project.getPlugins().withType(IdeaPlugin.class, plugin -> configIdeaProject(project));
    }


    private static void configIdeaProject(Project project) {
        val ideaProject = ideaProject(project);
        if (ideaProject != null) {
            val ipr = ideaProject.getIpr();
            ipr.withXml(provider -> customizeProjectXml(provider, project));
        }
    }


    private static void customizeProjectXml(XmlProvider provider, Project project) {
        val rootNode = provider.asNode();

        val componentNodes = IntellijConfig.componentNodes(rootNode);

        val compilerConfiguration = findByAttribute(componentNodes, "name", "CompilerConfiguration",
            () -> rootNode.appendNode("component", m("name", "CompilerConfiguration"))
        );

        val annotationProcessing = XmlUtils.getOrCreate(compilerConfiguration, "annotationProcessing",
            () -> compilerConfiguration.appendNode("annotationProcessing")
        );
        val profileAttrs = new HashMap<String, String>();
        profileAttrs.put("default", "true");
        profileAttrs.put("name", "Lombok");
        profileAttrs.put("enabled", "true");
        val profile = XmlUtils.createNode(annotationProcessing, "profile", profileAttrs, asList(
            n("processor", m("name", LOMBOK_LAUNCH_ANNOTATION_PROCESSOR)),
            n("processorPath", m("useClasspath", "false"),
                lombokConfiguration(project).getFiles().stream().
                    map(LombokConfiguration::fileEntry).
                    collect(Collectors.toList()))
        ));

        project.getAllprojects().stream().
            filter(prj -> prj.getPlugins().hasPlugin(JavaPlugin.class)).
            forEach(prj -> profile.appendNode("module", m("name", prj.getName())));
    }


    private static NodeBuilder fileEntry(File file) {
        return n("entry", m("name", file.getAbsolutePath()));
    }


    private static void addProcessorToJavacTask(Project project) {
        project.getTasks().withType(JavaCompile.class, jcTask ->
            jcTask.getOptions().
                setCompilerArgs(asList("-processor", LOMBOK_LAUNCH_ANNOTATION_PROCESSOR))
        );
    }


    private static void addDependencies(Project project, Supplier<String> lombokVersionSupplier) {
        val lombokVersion = lombokVersionSupplier.get();
        val lombokDep = new DefaultExternalModuleDependency("org.projectlombok", "lombok", lombokVersion);

        createLombokConfiguration(project, lombokDep);
        addLombokCompileOnlyDependency(project, lombokDep);
    }


    private static void addLombokCompileOnlyDependency(Project project, Dependency lombokDep) {
        project.getConfigurations().
            getByName(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME).
            getDependencies().
            add(lombokDep);
    }


    private static Configuration lombokConfiguration(Project project) {
        return project.getConfigurations().findByName(LOMBOK_CONFIGURATION_NAME);
    }


    private static Configuration createLombokConfiguration(Project project, Dependency lombokDep) {
        final Configuration configuration = project.getConfigurations().
            create(LOMBOK_CONFIGURATION_NAME).
            setVisible(false).
            setDescription("Lombok classes");
        configuration.
            getDependencies().
            add(lombokDep);
        return configuration;
    }

}
