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

import com.mooregreatsoftware.gradle.defaults.ProjectUtils;
import com.mooregreatsoftware.gradle.defaults.config.JavaConfig.Option;
import lombok.val;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;

import static java.util.Arrays.asList;

@SuppressWarnings("WeakerAccess")
public class CheckerFrameworkConfiguration extends AnnotationProcessorConfiguration {
    public static final String DEFAULT_CHECKER_VERSION = "2.0.1";

    protected static final String CHECKERFRAMEWORK_NULLNESS_CHECKER = "org.checkerframework.checker.nullness.NullnessChecker";


    protected CheckerFrameworkConfiguration(Project project, Supplier<String> lombokVersionSupplier) {
        super(project, lombokVersionSupplier);
    }


    @Override
    protected Collection<String> myProcessorClassNames() {
        return Collections.singleton(CHECKERFRAMEWORK_NULLNESS_CHECKER);
    }


    /**
     * Creates a new instance of {@link CheckerFrameworkConfiguration}.
     *
     * @param project                the Project to create it against
     * @param checkerVersionSupplier a {@link Supplier} of the version of Checker Framework to use
     * @see #DEFAULT_CHECKER_VERSION
     */
    public static CheckerFrameworkConfiguration create(Project project, Supplier<String> checkerVersionSupplier,
                                                       JavaConfig javaConfig) {
        val checkerConfiguration = new CheckerFrameworkConfiguration(project, checkerVersionSupplier);

        checkerConfiguration.configure(javaConfig);

        return checkerConfiguration;
    }


    @Override
    protected void registerWithJavac(JavaConfig javaConfig) {
        super.registerWithJavac(javaConfig);

        // "skipUses=javaslang"
        // remove warns to turn the checks into errors
        javaConfig.registerAnnotationProcessorOptions(
            asList(new Option("warns", "true"), new Option("lint", "-cast:unsafe")));

        javaConfig.registerBootClasspath(bootClasspathFiles());
    }


    private Set<File> bootClasspathFiles() {
        return ProjectUtils.getConfiguration("checkerframework.bootclasspath.lib.conf", deps ->
            deps.add(new DefaultExternalModuleDependency("org.checkerframework", "jdk8", versionSupplier.get())), project.getConfigurations()
        ).getFiles();
    }


    private Configuration processLibConf() {
        return ProjectUtils.getConfiguration("checkerframework.processor.lib.conf", deps ->
            deps.add(new DefaultExternalModuleDependency("org.checkerframework", "checker", versionSupplier.get())), project.getConfigurations()
        );
    }


    public File processorLibraryFile() {
        return processLibConf().getSingleFile();
    }


    public File compilerLibraryFile() {
        return ProjectUtils.getConfiguration("checkerframework.compiler.lib.conf", deps ->
            deps.add(new DefaultExternalModuleDependency("org.checkerframework", "compiler", versionSupplier.get())), project.getConfigurations()
        ).getSingleFile();
    }


    @Override
    protected Collection<File> myProcessorLibFiles() {
        return processLibConf().getFiles();
    }


    @Override
    protected void addCompileOnlyDependencies() {
        val dep = new DefaultExternalModuleDependency("org.checkerframework", "checker-qual", versionSupplier.get());
        addCompileOnlyDependency(project, dep);
    }

}
