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

import org.ajoberstar.gradle.git.release.base.ReleasePluginExtension;
import org.ajoberstar.grgit.Grgit;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.tasks.TaskContainer;

public class ReleaseConfig extends AbstractConfig {

    private final Grgit grgit;


    public ReleaseConfig(Project project, Grgit grgit) {
        super(project);
        this.grgit = grgit;
    }


    public void config() {
        plugins().apply("org.ajoberstar.release-opinion");
        ReleasePluginExtension release = project.getConvention().getByType(ReleasePluginExtension.class);
        release.setGrgit(grgit);
        final Task releaseTask = tasks().getByName("release");
        releaseTask.dependsOn("publishGhPages");
        project.allprojects(prj -> {
            final PluginContainer prjPlugins = prj.getPlugins();
            final TaskContainer prjTasks = prj.getTasks();
            prjPlugins.withId("org.gradle.base", plugin ->
                releaseTask.dependsOn(prjTasks.getByName("clean"), prjTasks.getByName("build"))
            );
            prjPlugins.withId("com.jfrog.bintray", plugin ->
                releaseTask.dependsOn(prjTasks.getByName("bintrayUpload"))
            );
        });
    }

}
