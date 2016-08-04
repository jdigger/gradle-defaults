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

import com.mooregreatsoftware.gradle.defaults.StreamableIterable;
import com.mooregreatsoftware.gradle.defaults.Utils;
import lombok.Value;
import lombok.val;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.JavaCompile;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.mooregreatsoftware.gradle.defaults.DefaultsPlugin.userEmail;
import static com.mooregreatsoftware.gradle.defaults.Utils.opt;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static org.gradle.api.plugins.JavaPlugin.COMPILE_JAVA_TASK_NAME;
import static org.gradle.api.plugins.JavaPlugin.JAVADOC_TASK_NAME;

@SuppressWarnings({"WeakerAccess", "RedundantTypeArguments", "RedundantCast"})
public class JavaConfig extends AbstractLanguageConfig<JavaPlugin> {
    public static final String PATH_SEPARATOR = System.getProperty("path.separator");

    public static final String SOURCES_JAR_TASK_NAME = "sourcesJar";

    @MonotonicNonNull
    private Jar sourcesJarTask;

    private final Set<File> processorLibFiles = new HashSet<>();
    private final Set<File> bootClasspath = new HashSet<>();
    private final Set<Option> annotationProcessorOptions = new HashSet<>();
    private final Set<String> annotationProcessorClassNames = new HashSet<>();
    private final Set<String> javacOptions = new HashSet<>();


    protected JavaConfig(Project project) {
        super(project);
    }


    /**
     * Returns the JavaConfig for in the given Project.
     *
     * @param project the project containing the JavaConfig
     */
    public static JavaConfig of(Project project) {
        val javaConfig = (JavaConfig)project.getExtensions().findByName(JavaConfig.class.getName());
        return javaConfig != null ? javaConfig : (JavaConfig)new JavaConfig(project).config();
    }


    @Override
    protected void configLanguage() {
        super.configLanguage();

        project.afterEvaluate(prj -> setManifestAttributes()); // TODO doFirst for task

        final TaskContainer tasks = project.getTasks(); // TODO FIX
        tasks.withType(JavaCompile.class, this::configureJavac);
    }


    private Task configureJavac(JavaCompile jcTask) {
        return jcTask.doFirst(new ConfigCompilerAction());
    }


    public Jar sourcesJarTask() {
        if (sourcesJarTask == null) {
            sourcesJarTask = createSourcesJarTask();
        }
        return sourcesJarTask;
    }


    private Jar createSourcesJarTask() {
        val sourceJarTask = tasks().create(SOURCES_JAR_TASK_NAME, Jar.class);
        sourceJarTask.setClassifier("sources");
        sourceJarTask.from((FileCollection)sourceSets().findByName("main").getAllSource());
        return sourceJarTask;
    }


    @Override
    public void registerArtifacts(MavenPublication publication) {
        super.registerArtifacts(publication);

        publication.artifact(sourcesJarTask());
        artifacts().add("archives", sourcesJarTask());
    }


    private void setManifestAttributes() {
        debug("Setting MANIFEST.MF attributes");
        configureManifestAttributes(jarTask());
    }


    protected void configureManifestAttributes(Jar jarTask) {
        val attrs = manifestAttributes();

        jarTask.getManifest().attributes(attrs);
    }


    protected Map<String, String> manifestAttributes() {
        val attrs = new HashMap<String, String>();
        attrs.put("Implementation-Title", description());
        attrs.put("Implementation-Version", version());
        attrs.put("Built-By", builtBy());
        attrs.put("Built-Date", Instant.now().toString());
        attrs.put("Built-JDK", (@NonNull String)opt(System.getProperty("java.version")).orElse("1.8"));
        attrs.put("Built-Gradle", gradle().getGradleVersion());
        return attrs;
    }


    protected String builtBy() {
        return userEmail(project);
    }


    @Override
    protected String docTaskName() {
        return JAVADOC_TASK_NAME;
    }


    @Override
    protected Class<JavaPlugin> pluginClass() {
        return JavaPlugin.class;
    }


    @Override
    protected String compileTaskName() {
        return COMPILE_JAVA_TASK_NAME;
    }


    public StreamableIterable<File> annotationProcessorLibFiles() {
        return StreamableIterable.of(this.processorLibFiles);
    }


    public void registerAnnotationProcessorLibFiles(Collection<File> files) {
        processorLibFiles.addAll(files);
    }


    public StreamableIterable<String> annotationProcessorClassNames() {
        return StreamableIterable.of(this.annotationProcessorClassNames);
    }


    public void registerAnnotationProcessorClassnames(Collection<String> classnames) {
        this.annotationProcessorClassNames.addAll(classnames);
    }


    public StreamableIterable<File> bootClasspath() {
        return StreamableIterable.of(this.bootClasspath);
    }


    public void registerBootClasspath(Collection<File> files) {
        this.bootClasspath.addAll(files);
    }


    public StreamableIterable<Option> annotationProcessorOptions() {
        return StreamableIterable.of(this.annotationProcessorOptions);
    }


    /**
     * Register annotation processor arguments. Do not include the "-A". (e.g., instead of "-Awarn" use "warn")
     */
    @SuppressWarnings("Convert2MethodRef")
    public void registerAnnotationProcessorOptions(Collection<Option> options) {
        options.stream().
            map(JavaConfig::stripLeadingDashA).
            forEach(e -> this.annotationProcessorOptions.add(e));
    }


    private static Option stripLeadingDashA(Option o) {
        return (o.name().startsWith("-A")) ? new Option(o.name().substring(2), o.value()) : o;
    }


    public StreamableIterable<String> javacOptions() {
        return StreamableIterable.of(this.javacOptions);
    }


    /**
     * Register "raw" javac arguments.
     */
    public void registerJavacOptions(Collection<String> options) {
        this.javacOptions.addAll(options);
    }


    protected List<String> createJavacArgs() {
        val compilerArgs = new ArrayList<String>();

        addProcessor(compilerArgs);
        addProcessorPath(compilerArgs);
        addAnnotationProcessorOptions(compilerArgs);
        registerJavacOptions(singletonList("-Xlint:unchecked"));
        addOtherCompilerArgs(compilerArgs);
        addBootClasspath(compilerArgs);

        return compilerArgs;
    }


    private void addBootClasspath(List<String> compilerArgs) {
        if (bootClasspath().iterator().hasNext()) {
            compilerArgs.add("-Xbootclasspath/p:" +
                bootClasspath().stream().
                    map(File::getAbsolutePath).
                    sorted().
                    collect(joining(PATH_SEPARATOR)));
        }
    }


    private void addProcessor(List<String> compilerArgs) {
        compilerArgs.add("-processor");
        compilerArgs.add(annotationProcessorClassNames().stream().
            sorted().collect(joining(",")));
    }


    private void addProcessorPath(List<String> compilerArgs) {
        compilerArgs.add("-processorpath");
        compilerArgs.add(
            annotationProcessorLibFiles().stream().
                map(File::getAbsolutePath).
                sorted().
                collect(joining(PATH_SEPARATOR))
        );
    }


    @SuppressWarnings("Convert2MethodRef")
    private void addAnnotationProcessorOptions(List<String> compilerArgs) {
        annotationProcessorOptions().stream().
            sorted().
            map(arg -> "-A" + arg.name() + "=" + arg.value()).
            forEach(e -> compilerArgs.add(e));
    }


    @SuppressWarnings("Convert2MethodRef")
    private void addOtherCompilerArgs(List<String> compilerArgs) {
        javacOptions().stream().
            sorted().
            forEach(e -> compilerArgs.add(e));
    }

    // **********************************************************************
    //
    // HELPER CLASSES
    //
    // **********************************************************************

    protected class ConfigCompilerAction implements Action<Task> {

        @Override
        public void execute(Task javaCompileTask) {
            val compilerArgs = createJavacArgs();
            val options = ((JavaCompile)javaCompileTask).getOptions();

            if (Utils.isBuggyJavac()) {
                val extensions = project.getExtensions();
                val checkerConf = (CheckerFrameworkConfiguration)extensions.findByName(CheckerFrameworkConfiguration.class.getName());
                if (checkerConf != null) {
                    options.setFork(true);
                    options.getForkOptions().setJvmArgs(
                        singletonList("-Xbootclasspath/p:" + checkerConf.compilerLibraryFile().getAbsolutePath())
                    );
                }
            }

            options.setCompilerArgs(compilerArgs);
        }

    }


    @Value
    public static class Option implements Comparable<Option> {
        String name;
        String value;


        @Override
        public int compareTo(Option o) {
            val nameComp = name.compareTo(o.name());
            return nameComp != 0 ? nameComp : value.compareTo(o.value());
        }
    }

}
