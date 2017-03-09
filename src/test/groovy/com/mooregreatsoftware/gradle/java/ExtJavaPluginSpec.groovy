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
package com.mooregreatsoftware.gradle.java

import com.mooregreatsoftware.gradle.Projects
import nebula.test.PluginProjectSpec
import org.eclipse.jgit.api.Git
import spock.lang.Subject

class ExtJavaPluginSpec extends PluginProjectSpec {
    @Subject
    ExtJavaPlugin plugin


    @Override
    String getPluginName() {
        return ExtJavaPlugin.PLUGIN_ID
    }


    def setup() {
        project.group = "com.mooregreatsoftware.gradle.defaults"
        plugin = project.plugins.apply(pluginName) as ExtJavaPlugin
    }


    def "default JAR Manifest info"() {
        when:
        Projects.evaluate(project)

        then:
        plugin.docJarTask(project).name == "javadocJar"

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


    def "with git setup"() {
        def git = Git.init().setDirectory(projectDir).call()
        def gitConfig = git.repository.config
        gitConfig.setString("user", null, "email", "tester@test.com")
        gitConfig.save()

        when:
        Projects.evaluate(project)

        and:
        def jarTask = ExtJavaPlugin.jarTask(project)
        jarTask.execute()

        then:
        jarTask.manifest.attributes.get("Built-By") == "tester@test.com"
    }

}
