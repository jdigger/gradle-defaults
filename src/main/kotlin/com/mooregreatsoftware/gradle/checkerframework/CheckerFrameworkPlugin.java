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
package com.mooregreatsoftware.gradle.checkerframework;

import com.mooregreatsoftware.gradle.JavacUtils.Option;
import com.mooregreatsoftware.gradle.defaults.ProjectUtilsKt;
import com.mooregreatsoftware.gradle.defaults.UtilsKt;
import com.mooregreatsoftware.gradle.lang.AbstractAnnotationProcessorPlugin;
import kotlin.Unit;
import lombok.val;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency;
import org.gradle.api.tasks.compile.JavaCompile;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static com.mooregreatsoftware.gradle.JavacUtils.createJavacArgs;
import static com.mooregreatsoftware.gradle.JavacUtils.registerAnnotationProcessorOptions;
import static com.mooregreatsoftware.gradle.JavacUtils.registerBootClasspath;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

/**
 * Configures the project for
 * <a href="http://types.cs.washington.edu/checker-framework/current/checker-framework-manual.html">Checker Framework</a>
 */
@SuppressWarnings("Convert2MethodRef")
public class CheckerFrameworkPlugin extends AbstractAnnotationProcessorPlugin {

    public static final String PLUGIN_ID = "com.mooregreatsoftware.checker-framework";

    public static final String CHECKERFRAMEWORK_NULLNESS_CHECKER = "org.checkerframework.checker.nullness.NullnessChecker";


    @Override
    protected void registerWithJavac(Project project) {
        super.registerWithJavac(project);

        // "skipUses=javaslang"
        // remove warns to turn the checks into errors
        val options = asList(new Option("warns", "true"), new Option("lint", "-cast:unsafe"));
        registerAnnotationProcessorOptions(project, options);

        registerBootClasspath(project, bootClasspathFiles(project));

        project.getTasks().withType(JavaCompile.class, it -> this.configureJavac(project, it));
    }


    private Task configureJavac(Project project, JavaCompile jcTask) {
        return jcTask.doFirst(new ConfigCompilerAction(project));
    }


    private class ConfigCompilerAction implements Action<Task> {
        private final Project project;


        ConfigCompilerAction(Project project) {
            this.project = project;
        }


        @Override
        public void execute(Task javaCompileTask) {
            val javaCompile = (JavaCompile)javaCompileTask;
            val options = javaCompile.getOptions();

            if (UtilsKt.isBuggyJavac()) {
                options.setFork(true);
                options.getForkOptions().
                    setJvmArgs(singletonList("-Xbootclasspath/p:" + compilerLibraryFile(project).getAbsolutePath()));
            }

            val compilerArgs = createJavacArgs(javaCompileTask.getProject());

            options.setCompilerArgs(compilerArgs);
        }
    }


    private File compilerLibraryFile(Project project) {
        return ProjectUtilsKt.getConfiguration("checkerframework.compiler.lib.conf",
            deps -> {
                deps.add(new DefaultExternalModuleDependency("org.checkerframework", "compiler", checkerFrameworkExtension(project).getVersion()));
                return Unit.INSTANCE;
            },
            project.getConfigurations()).getSingleFile();
    }


    private Set<File> bootClasspathFiles(Project project) {
        return ProjectUtilsKt.getConfiguration("checkerframework.bootclasspath.lib.conf",
            deps -> {
                deps.add(new DefaultExternalModuleDependency("org.checkerframework", "jdk8", checkerFrameworkExtension(project).getVersion()));
                return Unit.INSTANCE;
            },
            project.getConfigurations()).getFiles();
    }


    @Override
    protected Collection<String> myProcessorClassNames() {
        return Collections.singleton(CHECKERFRAMEWORK_NULLNESS_CHECKER);
    }


    private static Configuration processLibConf(Project project) {
        return ProjectUtilsKt.getConfiguration("checkerframework.processor.lib.conf",
            deps -> {
                deps.add(new DefaultExternalModuleDependency("org.checkerframework", "checker", checkerFrameworkExtension(project).getVersion()));
                return Unit.INSTANCE;
            },
            project.getConfigurations());
    }


    @Override
    protected Collection<File> myProcessorLibFiles(Project project) {
        return processLibConf(project).getFiles();
    }


    static File processorLibFile(Project project) {
        return processLibConf(project).getSingleFile();
    }


    @Override
    protected void addCompileOnlyDependencies(Project project) {
        val dep = new DefaultExternalModuleDependency("org.checkerframework", "checker-qual", checkerFrameworkExtension(project).getVersion());
        AbstractAnnotationProcessorPlugin.addCompileOnlyDependency(project, dep);
    }


    /**
     * The [CheckerFrameworkExtension] for the project.
     */
    public static CheckerFrameworkExtension checkerFrameworkExtension(Project project) {
        val extension = project.getExtensions().findByType(CheckerFrameworkExtension.class);
        return (extension == null) ?
            project.getExtensions().create(CheckerFrameworkExtension.NAME, CheckerFrameworkExtension.class) :
            extension;
    }

}
