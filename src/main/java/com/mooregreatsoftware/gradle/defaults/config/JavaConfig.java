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

import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.AbstractCompile;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static com.mooregreatsoftware.gradle.defaults.DefaultsPlugin.userEmail;
import static com.mooregreatsoftware.gradle.defaults.Utils.opt;

@SuppressWarnings("WeakerAccess")
public class JavaConfig extends AbstractConfig {

    public JavaConfig(Project project) {
        super(project);
    }


    public void config(Supplier<String> compatibilityVersionSupplier) {
        plugins().withId("java", plugin -> {
            debug("Configuring the 'java' plugin");

            project.afterEvaluate(prj -> {
                setCompileCompatibility(compatibilityVersionSupplier.get());
                setManifestAttributes();
            });

            sourcesJar();
            javadocJar();
        });
    }


    private void genJar(String taskName, String classifier, FileCollection files) {
        Jar javadocJar = tasks().create(taskName, Jar.class);
        javadocJar.setClassifier(classifier);
        javadocJar.from(files);
        artifacts().add("archives", javadocJar);
    }


    private void sourcesJar() {
        genJar("sourcesJar", "sources", sourceSets().findByName("main").getAllSource());
    }


    private void javadocJar() {
        genJar("javadocJar", "javadoc", tasks().findByName("javadoc").getOutputs().getFiles());
    }


    private void setManifestAttributes() {
        debug("Setting MANIFEST.MF attributes");
        final Jar jar = (Jar)tasks().findByName("jar");
        final Map<String, Serializable> map = new HashMap<>();
        map.put("Implementation-Title", description());
        map.put("Implementation-Version", version());
        map.put("Built-By", userEmail(project));
        map.put("Built-Date", Instant.now());
        final String javaVersion = opt(System.getProperty("java.version")).orElse("1.8");
        map.put("Built-JDK", javaVersion);
        map.put("Built-Gradle", gradle().getGradleVersion());

        jar.getManifest().attributes(map);
    }


    private void setCompileCompatibility(String compatibilityVersion) {
        debug("Setting Java source and target compatibility to " + compatibilityVersion);
        final AbstractCompile compileJava = (AbstractCompile)tasks().getByName("compileJava");
        compileJava.setSourceCompatibility(compatibilityVersion);
        compileJava.setTargetCompatibility(compatibilityVersion);
    }

}
