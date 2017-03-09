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
package com.mooregreatsoftware.gradle.util;

import javaslang.collection.TreeSet;
import lombok.val;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import static java.util.Collections.singletonList;

@SuppressWarnings("Convert2MethodRef")
public final class JavacUtils {
    public static final String PATH_SEPARATOR = System.getProperty("path.separator");


    private JavacUtils() {
    }


    public static LinkedHashSet<File> getMutableBootClasspath(Project project) {
        return getSetFromExtraProperties(project, "javac.bootclasspath");
    }


    public static LinkedHashSet<File> getMutableAnnotationProcessorLibFiles(Project project) {
        return getSetFromExtraProperties(project, "javac.annotationProcessor.lib.files");
    }


    public static LinkedHashSet<String> getMutableAnnotationProcessorClassNames(Project project) {
        return getSetFromExtraProperties(project, "javac.annotationProcessor.class.names");
    }


    private static LinkedHashSet<String> getMutableJavacOptions(Project project) {
        return getSetFromExtraProperties(project, "javac.javac.options");
    }


    public static LinkedHashSet<Option> getMutableAnnotationProcessorOptions(Project project) {
        return getSetFromExtraProperties(project, "javac.annotationProcessor.options");
    }


    @SuppressWarnings("unchecked")
    private static <T> LinkedHashSet<T> getSetFromExtraProperties(Project project, String keyname) {
        val ext = project.getExtensions().getExtraProperties();
        if (ext.has(keyname)) {
            return (@NonNull LinkedHashSet<T>)ext.get(keyname);
        }
        else {
            val set = new LinkedHashSet<T>();
            ext.set(keyname, set);
            return set;
        }
    }


    public static void registerBootClasspath(Project project, Collection<File> files) {
        getMutableBootClasspath(project).addAll(files);
    }


    public static void registerAnnotationProcessorLibFiles(Project project, Collection<File> files) {
        getMutableAnnotationProcessorLibFiles(project).addAll(files);
    }


    public static void registerAnnotationProcessorClassnames(Project project, Collection<String> classnames) {
        getMutableAnnotationProcessorClassNames(project).addAll(classnames);
    }


    /**
     * Register annotation processor arguments. Do not include the "-A". (e.g., instead of "-Awarn" use "warn")
     */
    public static void registerAnnotationProcessorOptions(Project project, Iterable<Option> options) {
        getMutableAnnotationProcessorOptions(project).addAll(
            TreeSet.ofAll(options).map(it -> stripLeadingDashA(it)).toJavaSet()
        );
    }


    private static Option stripLeadingDashA(Option o) {
        if (o.name.startsWith("-A")) return new Option(o.name.substring(2), o.value);
        else return o;
    }


    public static List<String> createJavacArgs(Project project) {
        val compilerArgs = new ArrayList<String>();

        addProcessor(project, compilerArgs);
        addProcessorPath(project, compilerArgs);
        addAnnotationProcessorOptions(project, compilerArgs);
        registerJavacOptions(project, singletonList("-Xlint:unchecked"));
        addOtherCompilerArgs(project, compilerArgs);
        addBootClasspath(project, compilerArgs);

        return compilerArgs;
    }


    private static void addBootClasspath(Project project, List<String> compilerArgs) {
        final LinkedHashSet<File> bootClasspath = getMutableBootClasspath(project);
        if (!bootClasspath.isEmpty()) {
            compilerArgs.add("-Xbootclasspath/p:" +
                TreeSet.ofAll(bootClasspath).
                    map(f -> f.getAbsolutePath()).
                    mkString(PATH_SEPARATOR)
            );
        }
    }


    private static void addProcessor(Project project, List<String> compilerArgs) {
        val classNames = getMutableAnnotationProcessorClassNames(project);
        if (classNames.isEmpty()) return;
        compilerArgs.add("-processor");
        compilerArgs.add(TreeSet.ofAll(classNames).mkString(","));
    }


    private static void addProcessorPath(Project project, List<String> compilerArgs) {
        val libFiles = getMutableAnnotationProcessorLibFiles(project);
        if (libFiles.isEmpty()) return;
        compilerArgs.add("-processorpath");
        compilerArgs.add(
            TreeSet.ofAll(libFiles).map(f -> f.getAbsolutePath()).mkString(PATH_SEPARATOR)
        );
    }


    private static void addAnnotationProcessorOptions(Project project, List<String> compilerArgs) {
        TreeSet.ofAll(getMutableAnnotationProcessorOptions(project)).
            map(o -> "-A" + o.name + "=" + o.value).
            forEach(it -> compilerArgs.add(it));
    }


    private static void addOtherCompilerArgs(Project project, List<String> compilerArgs) {
        getMutableJavacOptions(project).forEach(it -> compilerArgs.add(it));
    }


    /**
     * Register "raw" javac arguments.
     */
    private static void registerJavacOptions(Project project, Collection<String> options) {
        getMutableJavacOptions(project).addAll(options);
    }


    public static class Option implements Comparable<Option> {
        public final String name;
        public final String value;


        public Option(String name, String value) {
            this.name = name;
            this.value = value;
        }


        @Override
        public int compareTo(@NotNull Option o) {
            val nameComp = name.compareTo(o.name);
            return (nameComp != 0) ? nameComp : value.compareTo(o.value);
        }
    }
}
