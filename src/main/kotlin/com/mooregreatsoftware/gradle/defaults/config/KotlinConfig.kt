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

import com.mooregreatsoftware.gradle.defaults.Ternary
import org.gradle.api.Project
import org.gradle.api.plugins.scala.ScalaPlugin
import java.util.concurrent.Future

// TODO Make this automatically set up the buildscript, register the plugin, set up dokka, add the stdlib to the classpath, etc.
class KotlinConfig private constructor(project: Project) : AbstractLanguageConfig(project) {

    override fun compileTaskName(): String {
        return "compileKotlin"
    }


    override fun docTaskName(): String {
        return "dokka"
    }

    companion object {
        const val KOTLIN_PLUGIN_NAME = "org.jetbrains.kotlin.gradle.plugin.KotlinPlugin"
        const val DOKKA_PLUGIN_NAME = "org.jetbrains.dokka"

        private fun create(project: Project): Future<KotlinConfig?> {
            return confFuture(project, "Kotlin",
                { if (project.plugins.hasPlugin(KOTLIN_PLUGIN_NAME)) Ternary.TRUE else Ternary.MAYBE },
                { project.plugins.hasPlugin(KOTLIN_PLUGIN_NAME) },
                { KotlinConfig(project).config(ScalaPlugin::class.java) as KotlinConfig },
                null
            )
        }

        /**
         * Returns the KotlinConfig for the given Project.
         *
         * @param project the project containing the KotlinConfig
         */
        @JvmStatic fun of(project: Project): Future<KotlinConfig?> {
            return ofFuture(project, { create(project) })
        }
    }
}
