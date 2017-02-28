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
package com.mooregreatsoftware.gradle.scala

import com.mooregreatsoftware.gradle.Projects
import com.mooregreatsoftware.gradle.defaults.DefaultsPlugin
import com.mooregreatsoftware.gradle.java.ExtJavaPlugin
import nebula.test.PluginProjectSpec
import spock.lang.Subject

class ExtScalaPluginSpec extends PluginProjectSpec {
    @Subject
    ExtScalaPlugin plugin


    @Override
    String getPluginName() {
        return ExtScalaPlugin.PLUGIN_ID
    }


    def setup() {
        project.group = "com.mooregreatsoftware.gradle.defaults"
        plugin = project.plugins.apply(pluginName) as ExtScalaPlugin
    }


    def "by itself"() {
        when:
        Projects.evaluate(project)

        then:
        plugin.docJarTask(project).name == "scaladocJar"

        and:
        def artifacts = project.configurations.getByName("archives").allArtifacts
        artifacts.every { it.type == "jar" }
        artifacts.collect { it.classifier } as Set == ["", "sources", "scaladoc"] as Set

        when:
        def jarTask = ExtJavaPlugin.jarTask(project)
        jarTask.execute()

        then:
        jarTask.manifest.attributes.get("Built-By") == "unknown@unknown"
    }


    def "with defaults"() {
        given:
        project.plugins.apply(DefaultsPlugin.PLUGIN_ID)

        when:
        Projects.evaluate(project)

        then:
        plugin.docJarTask(project).name == "scaladocJar"

        and:
        def artifacts = project.configurations.getByName("archives").allArtifacts
        artifacts.every { it.type == "jar" }
        artifacts.collect { it.classifier } as Set == ["", "sources", "javadoc", "scaladoc"] as Set

        when:
        def jarTask = ExtJavaPlugin.jarTask(project)
        jarTask.execute()

        then:
        jarTask.manifest.attributes.get("Built-By") == "unknown@unknown"
    }

}
