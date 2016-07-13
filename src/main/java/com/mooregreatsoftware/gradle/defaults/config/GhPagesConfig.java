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

import lombok.val;
import org.ajoberstar.gradle.git.ghpages.GithubPagesPluginExtension;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.CopySpec;
import org.gradle.api.plugins.GroovyPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.scala.ScalaPlugin;

import java.util.function.Supplier;

import static org.gradle.api.plugins.GroovyPlugin.GROOVYDOC_TASK_NAME;
import static org.gradle.api.plugins.JavaPlugin.JAVADOC_TASK_NAME;
import static org.gradle.api.plugins.scala.ScalaPlugin.SCALA_DOC_TASK_NAME;

@SuppressWarnings("WeakerAccess")
public class GhPagesConfig extends AbstractConfig {

    public GhPagesConfig(Project project) {
        super(project);
    }


    public void config(Supplier<String> vcsWriteUrlSupplier) {
        info("Applying plugin 'org.ajoberstar.github-pages'");
        plugins().apply("org.ajoberstar.github-pages");

        project.allprojects(this::associateDocTasks);

        githubPages().getPages().from("src/gh-pages");

        project.afterEvaluate(prj -> {
            debug("Continuing configuring githubPages extension");
            githubPages().setRepoUri(vcsWriteUrlSupplier.get());
        });
    }


    private void associateDocTasks(Project prj) {
        val prjPlugins = prj.getPlugins();
        val prjTasks = prj.getTasks();

        // associate the "*doc" tasks of the different languages with the gh-pages output
        prjPlugins.withType(JavaPlugin.class, p -> addOutput(prjTasks.getByName(JAVADOC_TASK_NAME)));
        prjPlugins.withType(GroovyPlugin.class, p -> addOutput(prjTasks.getByName(GROOVYDOC_TASK_NAME)));
        prjPlugins.withType(ScalaPlugin.class, p -> addOutput(prjTasks.getByName(SCALA_DOC_TASK_NAME)));
    }


    private CopySpec addOutput(final Task task) {
        val pages = githubPages().getPages();
        val from = pages.from(task.getOutputs().getFiles());
        val replace = ("docs" + task.getPath().replace(":", "/"));

        return from.into(replace);
    }


    public GithubPagesPluginExtension githubPages() {
        return project.getExtensions().getByType(GithubPagesPluginExtension.class);
    }

}
