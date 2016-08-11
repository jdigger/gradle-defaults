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

class ScalaConfig private constructor(project: Project) : AbstractLanguageConfig(project) {

    override fun compileTaskName(): String {
        return "compileScala"
    }


    override fun docTaskName(): String {
        return ScalaPlugin.SCALA_DOC_TASK_NAME
    }

    companion object {

        private fun create(project: Project): Future<ScalaConfig?> {
            return confFuture(project, "Scala",
                { if (project.plugins.hasPlugin(ScalaPlugin::class.java)) Ternary.TRUE else Ternary.MAYBE },
                { project.plugins.hasPlugin(ScalaPlugin::class.java) },
                { ScalaConfig(project).config(ScalaPlugin::class.java) as ScalaConfig },
                null
            )
        }

        /**
         * Returns the ScalaConfig for the given Project.
         *
         * @param project the project containing the ScalaConfig
         */
        @JvmStatic fun of(project: Project): Future<ScalaConfig?> {
            return ofFuture(project, { create(project) })
        }
    }
}