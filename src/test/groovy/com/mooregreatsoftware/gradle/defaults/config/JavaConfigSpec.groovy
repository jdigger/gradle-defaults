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

import nebula.test.ProjectSpec
import org.eclipse.jgit.api.Git
import org.gradle.api.internal.project.AbstractProject
import org.gradle.api.internal.project.DefaultProject
import org.gradle.api.plugins.JavaPlugin

class JavaConfigSpec extends ProjectSpec {


    def setup() {
        project.plugins.apply(JavaPlugin)
        project.setGroup("com.mooregreatsoftware.gradle.defaults")
    }


    def "defaults"() {
        def javaConfig = JavaConfig.create(project, { "1.8" })

        when:
        evaluateProject()

        then:
        javaConfig.compileTask().targetCompatibility == "1.8"
        javaConfig.jarTask().manifest.attributes.get("Built-By") == "unknown@unknown"
        javaConfig.docJarTask().name == "javadocJar"

        and:
        def artifacts = project.configurations.getByName("archives").allArtifacts
        artifacts.every { it.type == "jar" }
        artifacts.collect { it.classifier } as Set == ["", "sources", "javadoc"] as Set
    }


    def "with git setup"() {
        def git = Git.init().setDirectory(projectDir).call()
        def gitConfig = git.repository.config
        gitConfig.setString("user", null, "email", "tester@test.com")
        gitConfig.save()

        def javaConfig = JavaConfig.create(project, { "1.6" })

        when:
        evaluateProject()

        then:
        javaConfig.compileTask().targetCompatibility == "1.6"
        javaConfig.jarTask().manifest.attributes.get("Built-By") == "tester@test.com"
    }


    def "custom built by"() {
        def javaConfig = new JavaConfig(project, { "1.7" }) {
            protected String builtBy() {
                return "Fooble Booble"
            }
        }.config() as JavaConfig

        when:
        evaluateProject()

        then:
        javaConfig.compileTask().targetCompatibility == "1.7"
        javaConfig.jarTask().manifest.attributes.get("Built-By") == "Fooble Booble"
    }


    AbstractProject evaluateProject() {
        return ((DefaultProject)project).evaluate()
    }

}
