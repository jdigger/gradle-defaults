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
package com.mooregreatsoftware.gradle.defaults

import com.mooregreatsoftware.gradle.Projects
import com.mooregreatsoftware.gradle.groovy.ExtGroovyPlugin
import com.mooregreatsoftware.gradle.java.ExtJavaPlugin
import com.mooregreatsoftware.gradle.kotlin.ExtKotlinPlugin
import nebula.test.PluginProjectSpec

@SuppressWarnings("GroovyPointlessBoolean")
class DefaultPluginSpec extends PluginProjectSpec {

    @Override
    String getPluginName() {
        return "com.mooregreatsoftware.defaults"
    }


    def "extension"() {
        project.plugins.apply(DefaultsPlugin)
        def rootExt = DefaultsExtensionKt.defaultsExtension(project)
        rootExt.openSource = true
        rootExt.orgId = "theId"

        def subProj = addSubproject("subproj")
        def subExt = DefaultsExtensionKt.defaultsExtension(subProj)

        expect:
        subExt.openSource == true
        subExt.siteUrl == "https://github.com/theId/subproj"
    }


    def "with groovy"() {
        given:
        def plugin = project.plugins.apply(ExtGroovyPlugin.PLUGIN_ID) as ExtGroovyPlugin

        project.plugins.apply(DefaultsPlugin.PLUGIN_ID)
        project.group = "com.mooregreatsoftware.gradle.defaults"

        when:
        Projects.evaluate(project)

        then:
        plugin.docJarTask(project).name == "groovydocJar"

        and:
        def artifacts = project.configurations.getByName("archives").allArtifacts
        artifacts.every { it.type == "jar" }
        artifacts.collect { it.classifier } as Set == ["", "sources", "javadoc", "groovydoc"] as Set

        when:
        def jarTask = ExtJavaPlugin.jarTask(project)
        jarTask.execute()

        then:
        jarTask.manifest.attributes.get("Built-By") == "unknown@unknown"
    }


    def "with kotlin"() {
        given:
        def plugin = project.plugins.apply(ExtKotlinPlugin.PLUGIN_ID) as ExtKotlinPlugin

        project.plugins.apply(DefaultsPlugin.PLUGIN_ID)
        project.group = "com.mooregreatsoftware.gradle.defaults"

        when:
        Projects.evaluate(project)

        then:
        plugin.docJarTask(project) == null

        and:
        def artifacts = project.configurations.getByName("archives").allArtifacts
        artifacts.every { it.type == "jar" }
        artifacts.collect { it.classifier } as Set == ["", "sources", "javadoc"] as Set

        when:
        def jarTask = ExtJavaPlugin.jarTask(project)
        jarTask.execute()

        then:
        jarTask.manifest.attributes.get("Built-By") == "unknown@unknown"
    }

}
