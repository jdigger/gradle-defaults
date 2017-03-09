/*
 * Copyright 2014-2017 the original author or authors.
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

import com.mooregreatsoftware.gradle.lang.AbstractAnnotationProcessorPlugin;
import com.mooregreatsoftware.gradle.util.ProjectUtilsKt;
import kotlin.Unit;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

/**
 * Configures the project for [Lombok](https://projectlombok.org/features/index.html)
 */
// TODO Offer to auto-create lombok.config file
public class LombokPlugin extends AbstractAnnotationProcessorPlugin {

    public static final String PLUGIN_ID = "com.mooregreatsoftware.lombok";

    public static final String LOMBOK_LAUNCH_ANNOTATION_PROCESSOR = "lombok.launch.AnnotationProcessorHider$AnnotationProcessor";


    @Override
    protected String pluginId() {
        return PLUGIN_ID;
    }


    @Override
    protected Collection<String> myProcessorClassNames() {
        return Collections.singleton(LOMBOK_LAUNCH_ANNOTATION_PROCESSOR);
    }


    private static Configuration processLibConf(Project project) {
        return ProjectUtilsKt.getConfiguration("lombok.processor.lib.conf", it -> {
            it.add(lombokDependency(project));
            return Unit.INSTANCE;
        }, project.getConfigurations());
    }


    @Override
    protected Collection<File> myProcessorLibFiles(Project project) {
        return processLibConf(project).getFiles();
    }


    public static File processorLibraryFile(Project project) {
        return processLibConf(project).getSingleFile();
    }


    @Override
    protected void addCompileOnlyDependencies(Project project) {
        AbstractAnnotationProcessorPlugin.addCompileOnlyDependency(project, lombokDependency(project));
    }


    private static Dependency lombokDependency(Project project) {
        return new DefaultExternalModuleDependency("org.projectlombok", "lombok", lombokExtension(project).getVersion());
    }


    public static LombokExtension lombokExtension(Project project) {
        final @Nullable LombokExtension lombokExt = project.getExtensions().findByType(LombokExtension.class);
        return (lombokExt == null) ? project.getExtensions().create(LombokExtension.NAME, LombokExtension.class) : lombokExt;
    }

}
