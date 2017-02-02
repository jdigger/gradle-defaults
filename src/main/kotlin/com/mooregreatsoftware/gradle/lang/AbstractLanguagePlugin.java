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
package com.mooregreatsoftware.gradle.lang;

import com.mooregreatsoftware.gradle.defaults.DefaultsExtensionKt;
import com.mooregreatsoftware.gradle.defaults.DefaultsPlugin;
import com.mooregreatsoftware.gradle.defaults.ProjectUtilsKt;
import com.mooregreatsoftware.gradle.maven.MavenPublishPublications;
import lombok.val;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.gradle.api.plugins.JavaPlugin.JAR_TASK_NAME;

public abstract class AbstractLanguagePlugin implements Plugin<Project> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractLanguagePlugin.class);
    private static final String SOURCES_JAR_TASK_NAME = "sourcesJar";


    protected static void configureLanguageTasks(Project project) {
        project.getTasks().withType(Jar.class, jarTask ->
            jarTask.doFirst(task -> configureManifestAttributes(jarTask))
        );

        project.getTasks().withType(AbstractCompile.class,
            abstractCompile -> abstractCompile.doFirst(task -> {
                val defaults = DefaultsExtensionKt.defaultsExtension(task.getProject());
                abstractCompile.setSourceCompatibility(defaults.getCompatibilityVersion().toString());
                abstractCompile.setTargetCompatibility(defaults.getCompatibilityVersion().toString());
            })
        );
    }


    private static void configureManifestAttributes(Jar jarTask) {
        LOG.debug("Setting MANIFEST.MF attributes for {}", jarTask);
        val attrs = manifestAttributes(jarTask.getProject());

        jarTask.getManifest().attributes(attrs);
    }


    private static Map<String, String> manifestAttributes(Project project) {
        val attrs = new HashMap<String, String>();
        attrs.put("Implementation-Title", project.getDescription() != null ? project.getDescription() : project.getName());
        attrs.put("Implementation-Version", project.getVersion().toString());
        attrs.put("Built-By", builtBy(project));
        attrs.put("Built-Date", Instant.now().toString());
        attrs.put("Built-JDK", System.getProperty("java.version"));
        attrs.put("Built-Gradle", project.getGradle().getGradleVersion());
        return attrs;
    }


    private static String builtBy(Project project) {
        return DefaultsPlugin.Companion.userEmail(project);
    }


    @Override
    public final void apply(Project project) {
        DefaultsExtensionKt.defaultsExtension(project);

        LOG.info("Applying {} to {}", pluginId(), project);

        project.getPlugins().apply(basePluginId());

        registerArtifacts(project, MavenPublishPublications.mainPublication(project));

        doApply(project);

        configureLanguageTasks(project);
    }


    protected abstract String pluginId();

    /**
     * The "base" language plugin ID. For example, "java"
     */
    protected abstract String basePluginId();


    protected void doApply(Project project) {
    }


    private void registerArtifacts(Project project, MavenPublication publication) {
        val mainJarTask = (Jar)project.getTasks().getByName("jar");
        addArtifactToPublication(publication, mainJarTask);
        addArtifactToArchives(project, mainJarTask);

        addArtifactToPublication(publication, sourcesJarTask(project));
        addArtifactToArchives(project, sourcesJarTask(project));

        val docJarTask = docJarTask(project);
        if (docJarTask != null) {
            addArtifactToPublication(publication, docJarTask);
            addArtifactToArchives(project, docJarTask);
        }
    }


    private static void addArtifactToArchives(Project project, Jar jarTask) {
        val hasArtifactAlready = project.getConfigurations().
            findByName("archives").
            getArtifacts().
            stream().
            anyMatch(artifact -> jarTask.getOutputs().getFiles().contains(artifact.getFile()));
        if (!hasArtifactAlready) {
            project.getArtifacts().add("archives", jarTask);
        }
    }


    private static void addArtifactToPublication(MavenPublication publication, Jar jarTask) {
        val hasArtifactAlready = publication.getArtifacts().
            stream().
            anyMatch(artifact -> jarTask.getOutputs().getFiles().contains(artifact.getFile()));
        if (!hasArtifactAlready) {
            publication.artifact(jarTask);
        }
    }


    private Jar sourcesJarTask(Project project) {
        final Task task = project.getTasks().findByName(SOURCES_JAR_TASK_NAME);
        return task != null ? (Jar)task : createSourcesJarTask(project);
    }


    /**
     * This will pick up all sources, regardless of language or resource location,
     * as long as they use the "main" [SourceSetContainer].
     */
    private Jar createSourcesJarTask(Project project) {
        val sourceJarTask = project.getTasks().create(SOURCES_JAR_TASK_NAME, Jar.class);
        sourceJarTask.setClassifier("sources");
        val ss = ProjectUtilsKt.sourceSets(project.getConvention());
        if (ss != null) {
            sourceJarTask.from(ss.findByName("main").getAllSource());
        }
        return sourceJarTask;
    }


    @Nullable
    public Jar docJarTask(Project project) {
        val docJarTaskName = docJarTaskName();
        if (docJarTaskName == null) return null;
        final Task task = project.getTasks().findByName(docJarTaskName);
        return task != null ? (Jar)task : createDocJarTask(project);
    }


    protected abstract @Nullable String docTaskName();


    private @Nullable Jar createDocJarTask(Project project) {
        val docTaskName = docTaskName();
        if (docTaskName == null) return null;

        val docJarTaskName = docJarTaskName();
        if (docJarTaskName == null) return null;
        val docJarTask = project.getTasks().create(docJarTaskName, Jar.class);
        docJarTask.setClassifier(docTaskName);

        val docTask = docTask(project);
        if (docTask == null) return null;
        docJarTask.from(docTask.getOutputs().getFiles());
        return docJarTask;
    }


    private @Nullable String docJarTaskName() {
        return docTaskName() + "Jar";
    }


    private @Nullable Task docTask(Project project) {
        val docTaskName = docTaskName();
        return (docTaskName != null) ? project.getTasks().getByName(docTaskName) : null;
    }


    public static Jar jarTask(Project project) {
        return (Jar)project.getTasks().findByName(JAR_TASK_NAME);
    }

}
