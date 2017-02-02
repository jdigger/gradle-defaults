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
package com.mooregreatsoftware.gradle.release;

import com.jfrog.bintray.gradle.BintrayPlugin;
import com.mooregreatsoftware.gradle.ghpages.ExtGhPagesPlugin;
import lombok.val;
import org.ajoberstar.gradle.git.release.base.ReleasePluginExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.mooregreatsoftware.gradle.GrGitUtils.grgit;

/**
 * Applies Andrew Oberstar's "Opinionated Release" plugin
 * (see https://github.com/ajoberstar/gradle-git/wiki/Release-Plugins) and makes sure task dependencies are correct.
 */
public class ExtReleasePlugin implements Plugin<Project> {
    public static final String PLUGIN_ID = "com.mooregreatsoftware.release";

    private static final Logger LOG = LoggerFactory.getLogger(ExtReleasePlugin.class);
    private static final String RELEASE_OPINION_PLUGIN_ID = "org.ajoberstar.release-opinion";


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

        val plugins = project.getPlugins();

        LOG.info("Applying {} to {}", PLUGIN_ID, project);

        LOG.info("Applying {} to {}", RELEASE_OPINION_PLUGIN_ID, project);
        plugins.apply(RELEASE_OPINION_PLUGIN_ID);

        val release = project.getConvention().getByType(ReleasePluginExtension.class);
        release.setGrgit(grgit);

        val releaseTask = project.getTasks().getByName("release");

        plugins.withId(ExtGhPagesPlugin.GITHUB_PAGES_PLUGIN_ID, plugin -> {
            LOG.debug("Making {} depend on publishGhPages", releaseTask.getPath());
            releaseTask.dependsOn("publishGhPages");
        });

        project.allprojects(prj -> setupTaskDependencies(releaseTask, prj));
    }


    private static void setupTaskDependencies(Task releaseTask, Project prj) {
        val prjPlugins = prj.getPlugins();
        val prjTasks = prj.getTasks();
        prjPlugins.withId("org.gradle.base", p -> {
            final Task clean = prjTasks.getByName("clean");
            final Task build = prjTasks.getByName("build");
            LOG.debug("Making {} depend on {} and {}", releaseTask.getPath(), clean.getPath(), build.getPath());
            releaseTask.dependsOn(clean, build);
        });
        prjPlugins.withType(BintrayPlugin.class, p -> {
            final Task bintrayUpload = prjTasks.getByName("bintrayUpload");
            LOG.debug("Making {} depend on {}", releaseTask.getPath(), bintrayUpload.getPath());
            releaseTask.dependsOn(bintrayUpload);
        });
    }

}
