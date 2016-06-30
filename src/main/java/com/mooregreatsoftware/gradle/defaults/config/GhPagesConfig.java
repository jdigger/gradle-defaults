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

import org.ajoberstar.gradle.git.ghpages.GithubPagesPluginExtension;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.CopySpec;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.tasks.TaskContainer;

@SuppressWarnings("WeakerAccess")
public class GhPagesConfig extends AbstractConfig {

    public GhPagesConfig(Project project) {
        super(project);
    }


    public void config(String vcsWriteUrl) {
        info("Applying plugin 'org.ajoberstar.github-pages'");
        plugins().apply("org.ajoberstar.github-pages");

        project.allprojects(prj -> {
            final PluginContainer prjPlugins = prj.getPlugins();
            final TaskContainer prjTasks = prj.getTasks();

            // associate the "*doc" tasks of the different languages with the gh-pages output
            prjPlugins.withId("java", p -> addOutput(prjTasks.getByName("javadoc")));
            prjPlugins.withId("groovy", p -> addOutput(prjTasks.getByName("groovydoc")));
            prjPlugins.withId("scala", p -> addOutput(prjTasks.getByName("scaladoc")));
        });

        project.afterEvaluate(prj -> {
            debug("Continuing configuring githubPages extension");
            GithubPagesPluginExtension gheExt = githubPages();
            gheExt.setRepoUri(vcsWriteUrl);
            gheExt.getPages().from("src/gh-pages");
        });
    }


    private CopySpec addOutput(final Task task) {
        CopySpec pages = githubPages().getPages();

        CopySpec from = pages.from(task.getOutputs().getFiles());

        String replace = ("docs" + task.getPath()).replace(":", "/");

        return from.into(replace);
    }


    public GithubPagesPluginExtension githubPages() {
        return project.getExtensions().getByType(GithubPagesPluginExtension.class);
    }

}
