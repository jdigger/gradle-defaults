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
import lombok.val;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency;

import java.io.File;
import java.util.Collection;
import java.util.function.Supplier;

import static java.util.Collections.singletonList;

@SuppressWarnings("WeakerAccess")
public class LombokConfiguration extends AnnotationProcessorConfiguration {
    public static final String DEFAULT_LOMBOK_VERSION = "1.16.8";

    public static final String LOMBOK_LAUNCH_ANNOTATION_PROCESSOR = "lombok.launch.AnnotationProcessorHider$AnnotationProcessor";


    protected LombokConfiguration(Project project, Supplier<String> lombokVersionSupplier) {
        super(project, lombokVersionSupplier);
    }


    public static LombokConfiguration create(Project project, Supplier<String> lombokVersionSupplier,
                                             JavaConfig javaConfig) {
        val lombokConfig = new LombokConfiguration(project, lombokVersionSupplier);

        lombokConfig.configure(javaConfig);

        return lombokConfig;
    }


    @Override
    protected Collection<String> myProcessorClassNames() {
        return singletonList(LOMBOK_LAUNCH_ANNOTATION_PROCESSOR);
    }


    private Configuration processLibConf() {
        return ProjectUtils.getConfiguration("lombok.processor.lib.conf", deps ->
            deps.add(lombokDependency()), project.getConfigurations()
        );
    }


    @Override
    protected Collection<File> myProcessorLibFiles() {
        return processLibConf().getFiles();
    }


    public File processorLibraryFile() {
        return processLibConf().getSingleFile();
    }


    @Override
    protected void addCompileOnlyDependencies() {
        addCompileOnlyDependency(project, lombokDependency());
    }


    private Dependency lombokDependency() {
        return new DefaultExternalModuleDependency("org.projectlombok", "lombok", versionSupplier.get());
    }

}
