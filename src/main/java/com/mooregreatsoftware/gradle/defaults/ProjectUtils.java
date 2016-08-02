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
package com.mooregreatsoftware.gradle.defaults;

import lombok.val;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.plugins.Convention;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.lang.Boolean.FALSE;
import static java.util.Optional.ofNullable;

@SuppressWarnings("WeakerAccess")
public class ProjectUtils {

    /**
     * Does the given Project have any Java source code under it?
     * <p>
     * If the project applies the Java plugin, that does not necessarily mean it has Java source, since it may
     * be another JVM language, source as Groovy or Scala. So its SourceSets are checked for .java files.
     * <p>
     * If the project does not apply the Java plugin, the existence of .java files is still checked in the "standard"
     * locations: src/main/java, src/main/groovy, src/main/scala.
     */
    public static boolean hasJavaSource(Project project) {
        try {
            if (hasJavaPlugin(project)) {
                return hasJavaSourceWithJavaPlugin(project.getConvention());
            }
            else {
                final boolean foundJavaFile = hasJavaSourceWithoutJavaPlugin(project.getProjectDir());
                if (foundJavaFile) {
                    project.getLogger().warn("Found Java source files in a standard source directory, " +
                        "but the Java plugin has not been applied");
                }
                return foundJavaFile;
            }
        }
        catch (IOException exp) {
            project.getLogger().error("Could not verify if " + project.getName() + " has Java source", exp);
            return false;
        }
    }


    /**
     * Returns all of the projects that have the Java plugin and have Java source files.
     *
     * @param project a handle into the Project structures, gets resolved to the root Project
     */
    public static Stream<Project> allJavaProjects(Project project) {
        return project.getRootProject().getAllprojects().stream().
            filter(ProjectUtils::hasJavaPlugin).
            filter(ProjectUtils::hasJavaSource);
    }


    @SuppressWarnings("SimplifiableIfStatement")
    private static boolean hasJavaSourceWithoutJavaPlugin(File projectDir) throws IOException {
        val srcMain = projectDir.toPath().resolve("src").resolve("main");
        if (Files.exists(srcMain) && Files.isDirectory(srcMain)) {
            return pathHasAFile(srcMain.resolve("java"), ProjectUtils::isJavaFile) ||
                (pathHasAFile(srcMain.resolve("groovy"), ProjectUtils::isJavaFile) ||
                    pathHasAFile(srcMain.resolve("scala"), ProjectUtils::isJavaFile));
        }
        else {
            return false;
        }
    }


    private static boolean pathHasAFile(Path path, Predicate<Path> matcher) throws IOException {
        if (Files.exists(path)) {
            if (Files.isDirectory(path)) {
                return dirHasFile(path, matcher);
            }
            else return matcher.test(path);
        }
        return false;
    }


    private static boolean dirHasFile(Path dirPath, Predicate<Path> matcher) throws IOException {
        return Files.walk(dirPath).
            filter(matcher).
            findAny().isPresent();
    }


    private static boolean isJavaFile(Path p) {
        return p.getFileName().toString().endsWith(".java");
    }


    @SuppressWarnings("RedundantCast")
    private static boolean hasJavaSourceWithJavaPlugin(Convention projectConvention) {
        return (@NonNull Boolean)sourceSets(projectConvention).
            map(ssc ->
                ssc.stream().
                    filter(ProjectUtils::hasJavaSource).
                    findAny().isPresent()
            ).
            orElse(FALSE);
    }


    private static boolean hasJavaSource(SourceSet ss) {
        final SourceDirectorySet allJava = ss.getAllJava();
        return !allJava.isEmpty();
    }


    public static Optional<SourceSetContainer> sourceSets(Convention projectConvention) {
        return ofNullable(projectConvention.findPlugin(JavaPluginConvention.class)).
            flatMap(javaPluginConvention -> ofNullable(javaPluginConvention.getSourceSets()));
    }


    private static boolean hasJavaPlugin(Project prj) {
        return prj.getPlugins().hasPlugin(JavaBasePlugin.class);
    }


    /**
     * Gets the named {@link Configuration} from the {@link Project}. If it does not yet exist, it is created and the
     * dependencies are configured using the {@link Consumer}.
     *
     * @param confName             the name of the {@link Configuration}
     * @param dependenciesConsumer used to set up the given {@link DependencySet} for the newly created {@link Configuration}
     * @param configurations       container of {@link Configuration}s
     * @return the named {@link Configuration}
     * @see #createConfiguration(String, Consumer, ConfigurationContainer)
     */
    public static Configuration getConfiguration(String confName, Consumer<DependencySet> dependenciesConsumer,
                                                 ConfigurationContainer configurations) {
        return ofNullable(configurations.findByName(confName)).
            orElseGet(() -> createConfiguration(confName, dependenciesConsumer, configurations));
    }


    /**
     * Creates a new {@link Configuration} with the given name, configuring the dependencies using the {@link Consumer}.
     *
     * @param confName             the name of the new {@link Configuration}
     * @param dependenciesConsumer used to set up the given {@link DependencySet} for the newly created {@link Configuration}
     * @param configurations       container of {@link Configuration}s
     * @return the newly created {@link Configuration}
     * @throws InvalidUserDataException if the Configuration by already exists
     */
    public static Configuration createConfiguration(String confName,
                                                    Consumer<DependencySet> dependenciesConsumer,
                                                    ConfigurationContainer configurations) throws InvalidUserDataException {
        return configurations.create(confName, conf -> dependenciesConsumer.accept(conf.getDependencies()));
    }

}
