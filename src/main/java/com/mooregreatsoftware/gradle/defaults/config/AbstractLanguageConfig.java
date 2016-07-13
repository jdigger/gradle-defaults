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

import lombok.val;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.AbstractCompile;

import java.util.function.Supplier;

import static org.gradle.api.plugins.JavaPlugin.JAR_TASK_NAME;

@SuppressWarnings("WeakerAccess")
public abstract class AbstractLanguageConfig<PT extends Plugin> extends AbstractConfig implements ArtifactPublisher {
    private final Supplier<String> compatibilityVersionSupplier;

    private Jar docJarTask;


    protected AbstractLanguageConfig(Project project, Supplier<String> compatibilityVersionSupplier) {
        super(project);
        this.compatibilityVersionSupplier = compatibilityVersionSupplier;
    }


    public AbstractLanguageConfig config() {
        plugins().withType(pluginClass(), plugin -> configLanguage());
        return this;
    }


    protected void configLanguage() {
        info("Configuring {}", pluginClass().getSimpleName());

        project.afterEvaluate(prj -> configCompatibilityVersion());

        registerArtifacts(MavenPublishingConfig.mainPublication(project));
    }


    @Override
    public void registerArtifacts(MavenPublication publication) {
        publication.artifact(docJarTask());
        artifacts().add("archives", docJarTask());
    }


    public Jar docJarTask() {
        if (docJarTask == null) {
            docJarTask = createDocJarTask();
        }
        return docJarTask;
    }


    protected Jar createDocJarTask() {
        val docTaskName = docTaskName();
        val docJarTask = tasks().create(docTaskName + "Jar", Jar.class);
        docJarTask.setClassifier(docTaskName);
        docJarTask.from(docTask().getOutputs().getFiles());
        return docJarTask;
    }


    public final Task docTask() {
        return tasks().getByName(docTaskName());
    }


    protected abstract String docTaskName();


    protected abstract Class<PT> pluginClass();


    public Jar jarTask() {
        return (Jar)tasks().findByName(JAR_TASK_NAME);
    }


    protected void configCompatibilityVersion() {
        val compatibilityVersion = compatibilityVersion();
        debug("Setting {} source and target compatibility to {}", pluginClass().getSimpleName(), compatibilityVersion);

        // TODO add compatibility setting to "compileTest*" tasks too
        val compileGroovy = compileTask();
        compileGroovy.setSourceCompatibility(compatibilityVersion);
        compileGroovy.setTargetCompatibility(compatibilityVersion);
    }


    public final AbstractCompile compileTask() {
        return (AbstractCompile)tasks().getByName(compileTaskName());
    }


    protected abstract String compileTaskName();


    public String compatibilityVersion() {
        return compatibilityVersionSupplier.get();
    }
}
