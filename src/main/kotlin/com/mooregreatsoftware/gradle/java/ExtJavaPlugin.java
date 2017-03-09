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
package com.mooregreatsoftware.gradle.java;

import com.mooregreatsoftware.gradle.util.JavacUtils;
import com.mooregreatsoftware.gradle.lang.AbstractLanguagePlugin;
import lombok.val;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.compile.JavaCompile;

import static org.gradle.api.plugins.JavaPlugin.JAVADOC_TASK_NAME;

/**
 * Extended Java Plugin.
 * <p>
 * Applies common defaults to how the Java plugin works.
 */
@SuppressWarnings("Convert2MethodRef")
public class ExtJavaPlugin extends AbstractLanguagePlugin {

    public static final String PLUGIN_ID = "com.mooregreatsoftware.java";


    @Override
    protected String pluginId() {
        return PLUGIN_ID;
    }


    @Override
    protected String basePluginId() {
        return "org.gradle.java";
    }


    @Override
    protected void doApply(Project project) {
        project.getTasks().withType(JavaCompile.class, new ConfigureJavaCompileTask());
    }


    private static Task configureJavac(JavaCompile jcTask) {
        return jcTask.doFirst(new ConfigCompilerAction());
    }


    @Override
    protected String docTaskName() {
        return JAVADOC_TASK_NAME;
    }


    // **********************************************************************
    //
    // HELPER CLASSES
    //
    // **********************************************************************


    public static class ConfigCompilerAction implements Action<Task> {
        @Override
        public void execute(Task javaCompileTask) {
            val compilerArgs = JavacUtils.createJavacArgs(javaCompileTask.getProject());
            val javaCompile = (JavaCompile)javaCompileTask;
            val options = javaCompile.getOptions();

            options.setCompilerArgs(compilerArgs);
        }
    }

    private static class ConfigureJavaCompileTask implements Action<JavaCompile> {
        @Override
        public void execute(JavaCompile javaCompile) {
            ExtJavaPlugin.configureJavac(javaCompile);
        }
    }

}
