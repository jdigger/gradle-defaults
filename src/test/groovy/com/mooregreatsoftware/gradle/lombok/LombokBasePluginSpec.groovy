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
package com.mooregreatsoftware.gradle.lombok

import com.mooregreatsoftware.gradle.defaults.config.JavaConfig
import groovy.transform.CompileStatic
import nebula.test.ProjectSpec
import nebula.test.dependencies.DependencyGraph
import nebula.test.dependencies.GradleDependencyGenerator
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.testfixtures.ProjectBuilder

import static com.mooregreatsoftware.gradle.defaults.config.LombokConfiguration.DEFAULT_LOMBOK_VERSION

class LombokBasePluginSpec extends ProjectSpec {
    String mavenRepoPath
    String version = DEFAULT_LOMBOK_VERSION


    def setup() {
        def generator = new GradleDependencyGenerator(
            new DependencyGraph(dependencies())
        )
        mavenRepoPath = generator.generateTestMavenRepo().absolutePath
        applyRepository(project, mavenRepoPath)
        JavaConfig.@Companion.of(project)
    }


    @CompileStatic
    protected static MavenArtifactRepository applyRepository(Project project, String mavenRepoPath) {
        return project.repositories.maven({ MavenArtifactRepository repo -> repo.url = mavenRepoPath })
    }


    @CompileStatic
    String getPluginName() {
        return "com.mooregreatsoftware.lombok-base"
    }


    @CompileStatic
    List<String> dependencies() {
        ["org.projectlombok:lombok:${version}"]
    }


    def "Apply"() {

    }


    def 'apply does not throw exceptions'() {
        when:
        project.apply plugin: pluginName

        then:
        noExceptionThrown()
    }


    def 'apply is idempotent'() {
        when:
        project.apply plugin: pluginName
        project.apply plugin: pluginName

        then:
        noExceptionThrown()
    }


    def 'apply is fine on all levels of multiproject'() {
        def sub = createSubproject(project, 'sub')
        project.subprojects.add(sub)
        applyRepository(sub, mavenRepoPath)
        JavaConfig.@Companion.of(sub)

        when:
        project.apply plugin: pluginName
        sub.apply plugin: pluginName

        then:
        noExceptionThrown()
    }


    def 'apply to multiple subprojects'() {
        def subprojectNames = ['sub1', 'sub2', 'sub3']

        subprojectNames.each { subprojectName ->
            def subproject = createSubproject(project, subprojectName)
            project.subprojects.add(subproject)
            applyRepository(subproject, mavenRepoPath)
            JavaConfig.@Companion.of(subproject)
        }

        when:
        project.apply plugin: pluginName

        subprojectNames.each { subprojectName ->
            def subproject = project.subprojects.find { it.name == subprojectName }
            subproject.apply plugin: pluginName
        }

        then:
        noExceptionThrown()
    }


    @CompileStatic
    Project createSubproject(Project parentProject, String name) {
        ProjectBuilder.builder().withName(name).withProjectDir(new File(projectDir, name)).withParent(parentProject).build()
    }

}
