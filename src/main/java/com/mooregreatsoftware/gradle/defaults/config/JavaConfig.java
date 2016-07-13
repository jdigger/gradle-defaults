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

import com.mooregreatsoftware.gradle.defaults.Utils;
import lombok.Value;
import lombok.val;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.JavaCompile;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static com.mooregreatsoftware.gradle.defaults.DefaultsPlugin.userEmail;
import static com.mooregreatsoftware.gradle.defaults.Utils.opt;
import static com.mooregreatsoftware.gradle.defaults.Utils.setFromExt;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.gradle.api.plugins.JavaPlugin.COMPILE_JAVA_TASK_NAME;
import static org.gradle.api.plugins.JavaPlugin.JAVADOC_TASK_NAME;

@SuppressWarnings("WeakerAccess")
public class JavaConfig extends AbstractLanguageConfig<JavaPlugin> {
    public static final String PATH_SEPARATOR = System.getProperty("path.separator");

    public static final String SOURCES_JAR_TASK_NAME = "sourcesJar";

    private Jar sourcesJarTask;


    protected JavaConfig(Project project, Supplier<String> compatibilityVersionSupplier) {
        super(project, compatibilityVersionSupplier);
    }


    public static JavaConfig create(Project prj, Supplier<String> compatibilityVersionSupplier) {
        return (JavaConfig)new JavaConfig(prj, compatibilityVersionSupplier).config();
    }


    @Override
    protected void configLanguage() {
        super.configLanguage();

        project.afterEvaluate(prj -> setManifestAttributes());

        project.getTasks().withType(JavaCompile.class, this::configureJavac);
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
        attrs.put("Built-JDK", opt(System.getProperty("java.version")).orElse("1.8"));
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


    public static Collection<File> annotationProcessorLibFiles(Project project) {
        return registerAnnotationProcessorLibFiles(project, emptyList());
    }


    public static Collection<File> registerAnnotationProcessorLibFiles(Project project, Collection<File> files) {
        return setFromExt(project, "annotationProcessorConf.processorLibFiles", files);
    }


    public static Collection<String> annotationProcessorClassNames(Project project) {
        return registerAnnotationProcessorClassnames(project, emptyList());
    }


    public static Collection<String> registerAnnotationProcessorClassnames(Project project,
                                                                           Collection<String> classnames) {
        return setFromExt(project, "annotationProcessorConf.processorClassnames", classnames);
    }


    public static Collection<File> bootClasspath(Project project) {
        return registerBootClasspath(project, emptyList());
    }


    public static Collection<File> registerBootClasspath(Project project,
                                                         Collection<File> files) {
        return setFromExt(project, "javac.bootClasspathFiles", files);
    }


    public static Collection<Option> annotationProcessorOptions(Project project) {
        return registerAnnotationProcessorOptions(project, emptyList());
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


    /**
     * Register annotation processor arguments. Do not include the "-A". (e.g., instead of "-Awarn" use "warn")
     */
    public static Collection<Option> registerAnnotationProcessorOptions(Project project, Collection<Option> options) {
        return setFromExt(project, "annotationProcessorConf.annotationProcessorOptions",
            options.stream().
                map(JavaConfig::stripLeadingDashA).
                collect(toList())
        );
    }


    private static Option stripLeadingDashA(Option o) {
        return (o.name().startsWith("-A")) ? new Option(o.name().substring(2), o.value()) : o;
    }


    public static Collection<String> javacOptions(Project project) {
        return registerJavacOptions(project, emptyList());
    }


    /**
     * Register "raw" javac arguments.
     */
    public static Collection<String> registerJavacOptions(Project project, Collection<String> options) {
        return setFromExt(project, "javac.otherOtions",
            options.stream().map(s -> (s.startsWith("-A")) ? s.substring(2) : s).collect(toList()));
    }


    protected List<String> createJavacArgs() {
        val compilerArgs = new ArrayList<String>();

        addProcessor(compilerArgs);
        addProcessorPath(compilerArgs);
        addAnnotationProcessorOptions(compilerArgs);
        registerJavacOptions(project, singletonList("-Xlint:unchecked"));
        addOtherCompilerArgs(compilerArgs);
        addBootClasspath(compilerArgs);

        return compilerArgs;
    }


    private void addBootClasspath(List<String> compilerArgs) {
        if (!bootClasspath(project).isEmpty()) {
            compilerArgs.add("-Xbootclasspath/p:" +
                bootClasspath(project).stream().
                    map(File::getAbsolutePath).
                    sorted().
                    collect(joining(PATH_SEPARATOR)));
        }
    }


    private void addProcessor(List<String> compilerArgs) {
        compilerArgs.add("-processor");
        compilerArgs.add(annotationProcessorClassNames(project).stream().sorted().collect(joining(",")));
    }


    private void addProcessorPath(List<String> compilerArgs) {
        compilerArgs.add("-processorpath");
        compilerArgs.add(
            annotationProcessorLibFiles(project).stream().
                map(File::getAbsolutePath).
                sorted().
                collect(joining(PATH_SEPARATOR))
        );
    }


    private void addAnnotationProcessorOptions(List<String> compilerArgs) {
        annotationProcessorOptions(project).stream().
            sorted().
            map(arg -> "-A" + arg.name() + "=" + arg.value()).
            forEach(compilerArgs::add);
    }


    private void addOtherCompilerArgs(List<String> compilerArgs) {
        javacOptions(project).stream().
            sorted().
            forEach(compilerArgs::add);
    }


    protected class ConfigCompilerAction implements Action<Task> {

        public ConfigCompilerAction() {
        }


        @Override
        public void execute(Task javaCompileTask) {
            val compilerArgs = createJavacArgs();
            val options = ((JavaCompile)javaCompileTask).getOptions();

            if (Utils.isBuggyJavac()) {
                val checkerConf = (CheckerFrameworkConfiguration)project.getExtensions().findByName(CheckerFrameworkConfiguration.class.getName());
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

}
