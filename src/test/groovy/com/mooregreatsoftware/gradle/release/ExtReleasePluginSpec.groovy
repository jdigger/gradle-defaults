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
package com.mooregreatsoftware.gradle.release

import com.mooregreatsoftware.gradle.Projects
import com.mooregreatsoftware.gradle.defaults.GitHelper
import com.mooregreatsoftware.gradle.ghpages.ExtGhPagesPlugin
import nebula.test.PluginProjectSpec
import org.gradle.api.Task
import org.gradle.api.logging.LogLevel

@SuppressWarnings("GroovyPointlessBoolean")
class ExtReleasePluginSpec extends PluginProjectSpec {

    @Override
    String getPluginName() {
        return ExtReleasePlugin.PLUGIN_ID
    }


    def "release depends on a clean build"() {
        Projects.setLogLevel(LogLevel.INFO)
        GitHelper.newRepo(projectDir).setupRepo()

        project.plugins.apply(ExtReleasePlugin)
        project.plugins.apply("java")

        when:
        def releaseTask = project.tasks.getByName("release")

        then:
        releaseTask.dependsOn.findAll { it instanceof Task }.collect { it.name } as Set == ["clean", "build"] as Set
    }


    def "release depends on publishing GH Pages if applicable"() {
        Projects.setLogLevel(LogLevel.INFO)
        GitHelper.newRepo(projectDir).setupRepo()

        project.plugins.apply(ExtReleasePlugin)
        project.plugins.apply(ExtGhPagesPlugin.GITHUB_PAGES_PLUGIN_ID)

        when:
        def releaseTask = project.tasks.getByName("release")

        then:
        releaseTask.dependsOn.findAll { it instanceof String }.contains "publishGhPages"
    }


    def "release depends on publishing BinTray if applicable"() {
        Projects.setLogLevel(LogLevel.INFO)
        GitHelper.newRepo(projectDir).setupRepo()

        project.plugins.apply(ExtReleasePlugin)
        project.plugins.apply("com.jfrog.bintray")

        when:
        def releaseTask = project.tasks.getByName("release")

        then:
        releaseTask.dependsOn.findAll { it instanceof Task }.collect { ((Task)it).name } contains "bintrayUpload"
    }


    def "release does not apply if root project but no git repo"() {
        Projects.setLogLevel(LogLevel.INFO)

        project.plugins.apply(ExtReleasePlugin)

        when:
        def releaseTask = project.tasks.findByName("release")

        then:
        releaseTask == null
    }


    def "release does not apply if git repo but not root project"() {
        Projects.setLogLevel(LogLevel.INFO)
        GitHelper.newRepo(projectDir).setupRepo()

        def subproj = createSubproject(project, "subproj")
        subproj.plugins.apply(ExtReleasePlugin)

        when:
        def releaseTask = subproj.tasks.findByName("release")

        then:
        releaseTask == null
    }

}
