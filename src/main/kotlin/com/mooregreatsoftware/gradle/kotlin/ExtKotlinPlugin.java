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
package com.mooregreatsoftware.gradle.kotlin;

import com.mooregreatsoftware.gradle.lang.AbstractLanguagePlugin;
import lombok.val;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.gradle.api.Project;

// TODO Make this automatically set up the buildscript, register the plugin, set up dokka, add the stdlib to the classpath, etc.
public class ExtKotlinPlugin extends AbstractLanguagePlugin {
    public static final String PLUGIN_ID = "com.mooregreatsoftware.kotlin";


    @Override
    protected void doApply(Project project) {
        super.doApply(project);
        project.getTasks().getByName("classes").mustRunAfter("copyMainKotlinClasses");
    }


    @Override
    protected String pluginId() {
        return PLUGIN_ID;
    }


    @Override
    protected String basePluginId() {
        return "kotlin";
    }


    @Override
    protected @Nullable String docTaskName() {
        val classLoader = this.getClass().getClassLoader();
        if (classLoader == null) return null;
        // check for the existence of Dokka on the classpath
        val resource = classLoader.getResource("/org/jetbrains/dokka/DocumentationBuilder.class");
        return (resource != null) ? "dokka" : null;
    }

}
