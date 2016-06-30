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
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.AbstractCompile;

import java.util.function.Supplier;

public class GroovyConfig extends AbstractConfig {
    public GroovyConfig(Project project) {
        super(project);
    }


    public void config(Supplier<String> compatibilityVersionSupplier) {
        plugins().withId("groovy", plugin -> {
            debug("Configuring the 'groovy' plugin");

            project.afterEvaluate(prj -> {
                String compatibilityVersion = compatibilityVersionSupplier.get();
                debug("Setting Groovy source and target compatibility to " + compatibilityVersion);
                AbstractCompile compileGroovy = (AbstractCompile)tasks().getByName("compileGroovy");
                compileGroovy.setSourceCompatibility(compatibilityVersion);
                compileGroovy.setTargetCompatibility(compatibilityVersion);
            });

            Jar groovydocJar = tasks().create("groovydocJar", Jar.class);
            groovydocJar.setClassifier("groovydoc");
            groovydocJar.from(tasks().getByName("groovydoc").getOutputs().getFiles());

            artifacts().add("archives", groovydocJar);
        });
    }


}
