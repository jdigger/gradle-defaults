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
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency;
import org.gradle.api.plugins.JavaPlugin;

import java.util.function.Supplier;

public class LombokConfiguration extends AbstractConfig {
    private static final String LOMBOK_CONFIGURATION_NAME = "lombok";


    public LombokConfiguration(Project project) {
        super(project);
    }


    public void config(Supplier<String> lombokVersionSupplier) {
        plugins().withType(JavaPlugin.class, plugin -> {
            Configuration lombokConfiguration = project.getConfigurations().
                create(LOMBOK_CONFIGURATION_NAME).
                setVisible(false).
                setDescription("Lombok classes");

            project.afterEvaluate(prj -> {
                final String lombokVersion = lombokVersionSupplier.get();
                final Dependency lombokDep = new DefaultExternalModuleDependency("org.projectlombok", "lombok", lombokVersion);

                lombokConfiguration.
                    getDependencies().
                    add(lombokDep);

                project.getConfigurations().
                    getByName(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME).
                    getDependencies().
                    add(lombokDep);
            });
        });
    }

}
