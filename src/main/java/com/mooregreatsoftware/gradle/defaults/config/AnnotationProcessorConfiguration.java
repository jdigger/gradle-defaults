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
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.plugins.JavaPlugin;

import java.io.File;
import java.util.Collection;
import java.util.function.Supplier;

@SuppressWarnings("WeakerAccess")
public abstract class AnnotationProcessorConfiguration extends AbstractConfig {
    protected final Supplier<String> versionSupplier;


    protected AnnotationProcessorConfiguration(Project project, Supplier<String> versionSupplier) {
        super(project);
        this.versionSupplier = versionSupplier;
    }


    protected void configure(JavaConfig javaConfig) {
        configJavaPlugin(javaConfig);
    }


    protected void configJavaPlugin(JavaConfig javaConfig) {
        project.getPlugins().withType(JavaPlugin.class, plugin -> {
            registerWithJavac(javaConfig);
            addCompileOnlyDependencies();
        });
    }


    /**
     * @see JavaConfig#registerAnnotationProcessorLibFiles(Collection)
     * @see JavaConfig#registerAnnotationProcessorClassnames(Collection)
     * @see JavaConfig#registerAnnotationProcessorOptions(Collection)
     * @see JavaConfig#registerBootClasspath(Collection)
     */
    protected void registerWithJavac(JavaConfig javaConfig) {
        javaConfig.registerAnnotationProcessorLibFiles(myProcessorLibFiles());
        javaConfig.registerAnnotationProcessorClassnames(myProcessorClassNames());
    }


    /**
     * Returns the fully-qualified class names of the annotation processors to use.
     */
    protected abstract Collection<String> myProcessorClassNames();


    /**
     * Returns the files for the annotation processor libraries.
     */
    protected abstract Collection<File> myProcessorLibFiles();


    protected abstract void addCompileOnlyDependencies();


    protected static void addCompileOnlyDependency(Project project, Dependency checkerDep) {
        project.getConfigurations().
            getByName(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME).
            getDependencies().
            add(checkerDep);
    }

}
