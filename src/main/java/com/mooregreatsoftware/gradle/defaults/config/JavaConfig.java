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
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.bundling.Jar;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static com.mooregreatsoftware.gradle.defaults.DefaultsPlugin.userEmail;
import static com.mooregreatsoftware.gradle.defaults.Utils.opt;
import static org.gradle.api.plugins.JavaPlugin.COMPILE_JAVA_TASK_NAME;
import static org.gradle.api.plugins.JavaPlugin.JAVADOC_TASK_NAME;

@SuppressWarnings("WeakerAccess")
public class JavaConfig extends AbstractLanguageConfig<JavaPlugin> {
    public static final String SOURCES_JAR_TASK_NAME = "sourcesJar";

    private Jar sourcesJarTask;


    protected JavaConfig(Project project, Supplier<String> compatibilityVersionSupplier) {
        super(project, compatibilityVersionSupplier);
    }


    public static JavaConfig create(Project prj, Supplier<String> compatibilityVersionSupplier) {
        return (JavaConfig)new JavaConfig(prj, compatibilityVersionSupplier).config();
    }


    @Override
    protected void configLanguage() {
        super.configLanguage();

        project.afterEvaluate(prj -> {
            setManifestAttributes();
        });
    }

    public Jar sourcesJarTask() {
        if (sourcesJarTask == null) {
            sourcesJarTask = createSourcesJarTask();
        }
        return sourcesJarTask;
    }


    private Jar createSourcesJarTask() {
        val sourceJarTask = tasks().create(SOURCES_JAR_TASK_NAME, Jar.class);
        sourceJarTask.setClassifier("sources");
        sourceJarTask.from((FileCollection)sourceSets().findByName("main").getAllSource());
        return sourceJarTask;
    }


    @Override
    public void registerArtifacts(MavenPublication publication) {
        super.registerArtifacts(publication);

        publication.artifact(sourcesJarTask());
        artifacts().add("archives", sourcesJarTask());
    }


    private void setManifestAttributes() {
        debug("Setting MANIFEST.MF attributes");
        configureManifestAttributes(jarTask());
    }


    protected void configureManifestAttributes(Jar jarTask) {
        val attrs = manifestAttributes();

        jarTask.getManifest().attributes(attrs);
    }


    protected Map<String, String> manifestAttributes() {
        val attrs = new HashMap<String, String>();
        attrs.put("Implementation-Title", description());
        attrs.put("Implementation-Version", version());
        attrs.put("Built-By", builtBy());
        attrs.put("Built-Date", Instant.now().toString());
        attrs.put("Built-JDK", opt(System.getProperty("java.version")).orElse("1.8"));
        attrs.put("Built-Gradle", gradle().getGradleVersion());
        return attrs;
    }


    protected String builtBy() {
        return userEmail(project);
    }


    @Override
    protected String docTaskName() {
        return JAVADOC_TASK_NAME;
    }


    @Override
    protected Class<JavaPlugin> pluginClass() {
        return JavaPlugin.class;
    }


    @Override
    protected String compileTaskName() {
        return COMPILE_JAVA_TASK_NAME;
    }
}
