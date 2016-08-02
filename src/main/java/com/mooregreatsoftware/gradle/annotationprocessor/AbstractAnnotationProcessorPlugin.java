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
package com.mooregreatsoftware.gradle.annotationprocessor;

import com.mooregreatsoftware.gradle.defaults.config.JavaConfig;
import lombok.val;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.plugins.JavaBasePlugin;

import java.io.File;
import java.util.Collection;

public abstract class AbstractAnnotationProcessorPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getPlugins().apply(JavaBasePlugin.class);

        val configurations = project.getConfigurations();

        val javaConfig = JavaConfig.javaConfig(project).
            orElseThrow(() -> new IllegalStateException("JavaConfig has not been created"));
        registerWithJavac(javaConfig, configurations);
        addCompileOnlyDependencies(configurations);
    }


    /**
     * @see JavaConfig#registerAnnotationProcessorLibFiles(Collection)
     * @see JavaConfig#registerAnnotationProcessorClassnames(Collection)
     * @see JavaConfig#registerAnnotationProcessorOptions(Collection)
     * @see JavaConfig#registerBootClasspath(Collection)
     */
    protected void registerWithJavac(JavaConfig javaConfig, ConfigurationContainer configurations) {
        javaConfig.registerAnnotationProcessorLibFiles(myProcessorLibFiles(configurations));
        javaConfig.registerAnnotationProcessorClassnames(myProcessorClassNames());
    }


    /**
     * Returns the fully-qualified class names of the annotation processors to use.
     */
    protected abstract Collection<String> myProcessorClassNames();


    /**
     * Returns the files for the annotation processor libraries.
     */
    protected abstract Collection<File> myProcessorLibFiles(ConfigurationContainer configurations);


    protected abstract void addCompileOnlyDependencies(ConfigurationContainer configurations);

}
