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

import org.gradle.api.Project;
import org.gradle.api.plugins.GroovyPlugin;

import java.util.function.Supplier;

import static org.gradle.api.plugins.GroovyPlugin.GROOVYDOC_TASK_NAME;

@SuppressWarnings("WeakerAccess")
public class GroovyConfig extends AbstractLanguageConfig<GroovyPlugin> {

    protected GroovyConfig(Project project, Supplier<String> compatibilityVersionSupplier) {
        super(project, compatibilityVersionSupplier);
    }


    public static GroovyConfig create(Project project, Supplier<String> compatibilityVersionSupplier) {
        return (GroovyConfig)new GroovyConfig(project, compatibilityVersionSupplier).config();
    }


    @Override
    protected Class<GroovyPlugin> pluginClass() {
        return GroovyPlugin.class;
    }


    @Override
    protected String compileTaskName() {
        return "compileGroovy";
    }


    @Override
    protected String docTaskName() {
        return GROOVYDOC_TASK_NAME;
    }

}
