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
package com.mooregreatsoftware.gradle.defaults.config

import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.AbstractCompile

import java.util.function.Supplier

class ScalaConfig(project: Project) : AbstractConfig(project) {

    fun config(compatibilityVersionSupplier: Supplier<String>) {
        plugins().withId("scala") { plugin ->
            debug("Configuring the 'scala' plugin")

            project.afterEvaluate { prj ->
                val compatibilityVersion = compatibilityVersionSupplier.get()
                debug("Setting Scala source and target compatibility to " + compatibilityVersion)
                val compileScala = tasks().getByName("compileScala") as AbstractCompile
                compileScala.sourceCompatibility = compatibilityVersion
                compileScala.targetCompatibility = compatibilityVersion
            }

            val scaladocJar = tasks().create("scaladocJar", Jar::class.java)
            scaladocJar.classifier = "scaladoc"
            scaladocJar.from(tasks().getByName("scaladoc").outputs.files)

            artifacts().add("archives", scaladocJar)
        }
    }

}
