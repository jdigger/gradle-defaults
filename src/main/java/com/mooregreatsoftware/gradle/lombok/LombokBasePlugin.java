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
package com.mooregreatsoftware.gradle.lombok;

import com.mooregreatsoftware.gradle.annotationprocessor.AbstractAnnotationProcessorPlugin;
import com.mooregreatsoftware.gradle.defaults.ProjectUtils;
import com.mooregreatsoftware.gradle.defaults.config.LombokConfiguration;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency;

import java.io.File;
import java.util.Collection;
import java.util.function.Supplier;

import static java.util.Collections.singletonList;

public class LombokBasePlugin extends AbstractAnnotationProcessorPlugin {

    private Supplier<String> versionSupplier = () -> LombokConfiguration.DEFAULT_LOMBOK_VERSION;


    @Override
    public void apply(Project project) {
        super.apply(project);
    }


    @Override
    protected Collection<String> myProcessorClassNames() {
        return singletonList(LombokConfiguration.LOMBOK_LAUNCH_ANNOTATION_PROCESSOR);
    }


    private Configuration processorLibConf(ConfigurationContainer configurations) {
        return ProjectUtils.getConfiguration("lombok.processor.lib.conf", deps ->
                deps.add(lombokDependency()),
            configurations);
    }


    @Override
    protected Collection<File> myProcessorLibFiles(ConfigurationContainer configurations) {
        return processorLibConf(configurations).getFiles();
    }


    public File processorLibraryFile(ConfigurationContainer configurations) {
        return processorLibConf(configurations).getSingleFile();
    }


    @Override
    protected void addCompileOnlyDependencies(ConfigurationContainer configurations) {
//        addCompileOnlyDependency(project, lombokDependency());
    }


    private Dependency lombokDependency() {
        return new DefaultExternalModuleDependency("org.projectlombok", "lombok", versionSupplier.get());
    }


    public Supplier<String> getVersionSupplier() {
        return this.versionSupplier;
    }


    public void setVersionSupplier(Supplier<String> versionSupplier) {
        this.versionSupplier = versionSupplier;
    }

}
