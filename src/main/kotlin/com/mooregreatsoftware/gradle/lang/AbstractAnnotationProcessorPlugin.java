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

import com.mooregreatsoftware.gradle.JavacUtils;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.plugins.JavaPlugin;

import java.io.File;
import java.util.Collection;

public abstract class AbstractAnnotationProcessorPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        configJavaPlugin(project);
    }


    private void configJavaPlugin(Project project) {
        project.getPlugins().withId("org.gradle.java", plugin -> {
            registerWithJavac(project);
            addCompileOnlyDependencies(project);
        });
    }


    /**
     * @see JavacUtils#registerAnnotationProcessorLibFiles(Project, Collection)
     * @see JavacUtils#registerAnnotationProcessorClassnames(Project, Collection)
     * @see JavacUtils#registerAnnotationProcessorOptions(Project, Iterable)
     * @see JavacUtils#registerBootClasspath(Project, Collection)
     */
    protected void registerWithJavac(Project project) {
        JavacUtils.registerAnnotationProcessorLibFiles(project, myProcessorLibFiles(project));
        JavacUtils.registerAnnotationProcessorClassnames(project, myProcessorClassNames());
    }


    /**
     * Returns the fully-qualified class names of the annotation processors to use.
     */
    protected abstract Collection<String> myProcessorClassNames();


    /**
     * Returns the files for the annotation processor libraries.
     */
    protected abstract Collection<File> myProcessorLibFiles(Project project);


    protected abstract void addCompileOnlyDependencies(Project project);


    protected static void addCompileOnlyDependency(Project project, Dependency checkerDep) {
        project.getConfigurations().getByName(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME).getDependencies().add(checkerDep);
    }

}
