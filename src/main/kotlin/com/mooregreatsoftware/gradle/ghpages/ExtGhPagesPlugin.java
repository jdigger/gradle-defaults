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
package com.mooregreatsoftware.gradle.ghpages;

import com.mooregreatsoftware.gradle.defaults.ReadableDefaultsExtension;
import com.mooregreatsoftware.gradle.defaults.ReadableDefaultsExtensionKt;
import lombok.val;
import org.ajoberstar.gradle.git.ghpages.GithubPagesPlugin;
import org.ajoberstar.gradle.git.ghpages.GithubPagesPluginExtension;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.file.copy.CopySpecInternal;
import org.gradle.api.plugins.GroovyPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.scala.ScalaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.mooregreatsoftware.LangUtils.tryGet;
import static com.mooregreatsoftware.LangUtils.tryRun;
import static com.mooregreatsoftware.gradle.GrGitUtils.grgit;

/**
 * Applies Andrew Oberstar's "GH-Pages" plugin
 * (see https://github.com/ajoberstar/gradle-git/wiki/Github%20Pages%20Plugin) and makes sure task
 * dependencies are correct.
 */
public class ExtGhPagesPlugin implements Plugin<Project> {
    private static final Logger LOG = LoggerFactory.getLogger(ExtGhPagesPlugin.class);
    public static final String PLUGIN_ID = "com.mooregreatsoftware.gh-pages";

    public static final String GITHUB_PAGES_PLUGIN_ID = "org.ajoberstar.github-pages";

    private static final String PREPARE_TASK_NAME = GithubPagesPlugin.getPREPARE_TASK_NAME();


    @Override
    public void apply(Project project) {
        if (project != project.getRootProject()) {
            LOG.warn("{} can only be applied to the root project", PLUGIN_ID);
            return;
        }

        val grgit = grgit(project);
        if (grgit == null) {
            LOG.warn("{} only works with a git repository", PLUGIN_ID);
            return;
        }

        val readableDefaultsFuture = ReadableDefaultsExtensionKt.readableDefaultsExtension(project);
        LOG.info("Applying plugin \'" + GITHUB_PAGES_PLUGIN_ID + "\'");
        project.getPlugins().apply(GITHUB_PAGES_PLUGIN_ID);

        associateDocTasks(project);
        pagesCopySpec(project).addChild().from("src/gh-pages");

        val prepareTask = project.getTasks().getByName(PREPARE_TASK_NAME);
        prepareTask.doFirst(it -> setRepoUri(project, readableDefaultsFuture));
    }


    private static void setRepoUri(Project project, Future<ReadableDefaultsExtension> readableDefaultsFuture) {
        val readableDefaultsExtension = tryGet(() -> readableDefaultsFuture.get(1L, TimeUnit.SECONDS));
        val githubPages = githubPages(project);
        if (githubPages == null) throw new IllegalStateException("No GithubPagesPluginExtension");
        githubPages.setRepoUri(readableDefaultsExtension.getVcsWriteUrl());
    }


    private void associateDocTasks(Project project) {
        val plugins = project.getPlugins();
        plugins.withType(JavaPlugin.class, it -> addOutput(project.getTasks().getByName("javadoc")));
        plugins.withType(GroovyPlugin.class, it -> addOutput(project.getTasks().getByName("groovydoc")));
        plugins.withType(ScalaPlugin.class, it -> addOutput(project.getTasks().getByName("scaladoc")));
        plugins.withId("kotlin", kotlinPlugin -> configKotlin(project));
    }


    private void configKotlin(Project project) {
        project.getPlugins().withId("org.jetbrains.dokka", dokkaPlugin -> configDokkaPlugin(project));

        project.afterEvaluate(p -> {
            if (!project.getPlugins().hasPlugin("org.jetbrains.dokka")) {
                LOG.warn("Using the Kotlin plugin, but Dokka is not being used for API documentation");
            }
        });
    }


    private void configDokkaPlugin(Project project) {
        Task dokkaTask = project.getTasks().getByName("dokka");
        addOutput(dokkaTask);

        Task dokkaJavadoc = project.getTasks().findByName("dokkaJavadoc");
        if (dokkaJavadoc == null) {
            dokkaJavadoc = tryGet(() -> createDokkaTask(project));
        }

        addOutput(dokkaJavadoc);
    }


    @SuppressWarnings("unchecked")
    private static Task createDokkaTask(Project project) throws ClassNotFoundException {
        Class<Task> clazz = (Class<Task>)Class.forName("org.jetbrains.dokka.gradle.DokkaTask");
        return project.getTasks().create("dokkaJavadoc",
            clazz, task -> tryRun(() -> configDokkaTask(project, task)));
    }


    @SuppressWarnings("unchecked")
    private static void configDokkaTask(Project project, Object task) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Class taskClass = task.getClass();
        taskClass.getMethod("setOutputFormat", String.class).invoke(task, "javadoc");
        taskClass.getMethod("setOutputDirectory", String.class).invoke(task, project.getBuildDir() + "/dokkaJavadoc");
    }


    private CopySpec addOutput(Task task) {
        Project project = task.getProject();
        LOG.info("Creating CopySpec for GhPages of " + project + " from " + task.getName());
        FileCollection fromFiles = task.getOutputs().getFiles();
        String intoDir = task.getPath().replace(':', '/');
        LOG.debug("will copy {} to {}", fromFiles, intoDir);
        return pagesCopySpec(project).addChild().into(intoDir).from(fromFiles);
    }


    private static CopySpecInternal pagesCopySpec(Project project) {
        val githubPages = githubPages(project);
        if (githubPages == null) throw new IllegalStateException("No GithubPagesPluginExtension");
        return (CopySpecInternal)githubPages.getPages();
    }


    private static @Nullable GithubPagesPluginExtension githubPages(Project project) {
        return project.getExtensions().findByType(GithubPagesPluginExtension.class);
    }

}
