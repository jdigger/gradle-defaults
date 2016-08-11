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
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.GroovyPlugin.GROOVYDOC_TASK_NAME
import java.util.concurrent.Future

class GroovyConfig private constructor(project: Project) : AbstractLanguageConfig(project) {

    override fun compileTaskName(): String {
        return "compileGroovy"
    }


    override fun docTaskName(): String {
        return GROOVYDOC_TASK_NAME
    }

    companion object {

        private fun create(project: Project): Future<GroovyConfig?> {
            return confFuture(project, "Groovy",
                { if (project.plugins.hasPlugin(GroovyPlugin::class.java)) Ternary.TRUE else Ternary.MAYBE },
                { project.plugins.hasPlugin(GroovyPlugin::class.java) },
                { GroovyConfig(project).config(GroovyPlugin::class.java) as GroovyConfig },
                null
            )
        }

        /**
         * Returns the GroovyConfig for the given Project.
         *
         * @param project the project containing the GroovyConfig
         */
        @JvmStatic fun of(project: Project): Future<GroovyConfig?> {
            return ofFuture(project, { create(project) })
        }

    }

}
