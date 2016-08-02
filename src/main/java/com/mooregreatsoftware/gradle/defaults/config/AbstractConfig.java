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
import org.checkerframework.checker.nullness.qual.NonNull;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.ArtifactHandler;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;

@SuppressWarnings("WeakerAccess")
public abstract class AbstractConfig {
    protected final Project project;


    @SuppressWarnings("argument.type.incompatible")
    public AbstractConfig(Project project) {
        this.project = project;
        project.getExtensions().add(this.getClass().getName(), this);
    }


    protected static boolean isRootProject(Project project) {
        return project.equals(project.getRootProject());
    }


    protected void debug(String msg) {
        project.getLogger().debug(msg);
    }


    protected void debug(String format, String... msgArgs) {
        project.getLogger().debug(format, (Object[])msgArgs);
    }


    protected void info(String msg) {
        project.getLogger().info(msg);
    }


    protected void info(String format, String... msgArgs) {
        project.getLogger().info(format, (Object[])msgArgs);
    }


    protected PluginContainer plugins() {
        return project.getPlugins();
    }


    protected Gradle gradle() {
        return project.getGradle();
    }


    protected ArtifactHandler artifacts() {
        return project.getArtifacts();
    }


    protected TaskContainer tasks() {
        return project.getTasks();
    }


    @SuppressWarnings("RedundantCast")
    protected String description() {
        return (@NonNull String)Utils.opt(project.getDescription()).orElse(name());
    }


    protected String version() {
        return project.getVersion().toString();
    }


    protected String name() {
        return project.getName();
    }


    protected ExtraPropertiesExtension ext() {
        return project.getExtensions().getExtraProperties();
    }


    protected SourceSetContainer sourceSets() {
        return project.getConvention().findPlugin(JavaPluginConvention.class).getSourceSets();
    }

}
