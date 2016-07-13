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
package com.mooregreatsoftware.gradle.defaults

import nebula.test.ProjectSpec
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPlugin

import static com.mooregreatsoftware.gradle.defaults.Utils.deleteDir
import static java.nio.file.Files.createDirectories
import static java.nio.file.Files.createFile

@SuppressWarnings("GroovyPointlessBoolean")
class ProjectUtilsTest extends ProjectSpec {

    def "hasJavaSource with no plugins"() {
        expect:
        ProjectUtils.hasJavaSource(project) == false

        def srcMain = project.projectDir.toPath().resolve("src").resolve("main")

        when:
        createDirectories(srcMain.resolve("java"))

        then:
        ProjectUtils.hasJavaSource(project) == false

        when:
        createFile(srcMain.resolve("java").resolve("AFile.java"))

        then:
        ProjectUtils.hasJavaSource(project) == true

        when:
        deleteDir(srcMain.resolve("java"))
        createDirectories(srcMain.resolve("groovy"))

        then:
        ProjectUtils.hasJavaSource(project) == false

        when:
        createFile(srcMain.resolve("groovy").resolve("AFile.java"))

        then:
        ProjectUtils.hasJavaSource(project) == true

        when:
        deleteDir(srcMain.resolve("groovy"))
        createDirectories(srcMain.resolve("scala"))

        then:
        ProjectUtils.hasJavaSource(project) == false

        when:
        createFile(srcMain.resolve("scala").resolve("AFile.java"))

        then:
        ProjectUtils.hasJavaSource(project) == true

        when:
        deleteDir(srcMain.resolve("scala"))

        and:
        //
        // Check execution path for when the JavaPlugin has been applied
        //
        project.plugins.apply(JavaPlugin)

        then:
        ProjectUtils.hasJavaSource(project) == false

        when:
        createDirectories(srcMain.resolve("java"))
        createFile(srcMain.resolve("java").resolve("AFile.java"))

        then:
        ProjectUtils.hasJavaSource(project) == true

        when:
        deleteDir(srcMain.resolve("java"))

        then:
        ProjectUtils.hasJavaSource(project) == false

        when:
        ProjectUtils.sourceSets(project).ifPresent({
            it.create("gen-some-java").java.srcDir("src/main/some-java")
        })
        createDirectories(srcMain.resolve("some-java"))
        createFile(srcMain.resolve("some-java").resolve("AFile.java"))

        then:
        ProjectUtils.hasJavaSource(project) == true
    }


    def "hasJavaSource with JavaPlugin"() {
        project.plugins.apply(JavaPlugin)
        def srcMain = project.projectDir.toPath().resolve("src").resolve("main")

        expect:
        ProjectUtils.hasJavaSource(project) == false

        when:
        createDirectories(srcMain.resolve("java"))
        createFile(srcMain.resolve("java").resolve("AFile.java"))

        then:
        ProjectUtils.hasJavaSource(project) == true
    }


    def "hasJavaSource with JavaBasePlugin"() {
        project.plugins.apply(JavaBasePlugin)
        def srcMain = project.projectDir.toPath().resolve("src").resolve("main")

        expect:
        ProjectUtils.hasJavaSource(project) == false

        when:
        ProjectUtils.sourceSets(project).ifPresent({
            it.create("gen-some-java").java.srcDir("src/main/some-java")
        })
        createDirectories(srcMain.resolve("some-java"))
        createFile(srcMain.resolve("some-java").resolve("AFile.java"))

        then:
        ProjectUtils.hasJavaSource(project) == true
    }


    def "hasJavaSource with GroovyPlugin"() {
        project.plugins.apply(GroovyPlugin)
        def srcMain = project.projectDir.toPath().resolve("src").resolve("main")

        expect:
        ProjectUtils.hasJavaSource(project) == false
        project.plugins.findPlugin(JavaPlugin) != null

        when:
        createDirectories(srcMain.resolve("groovy"))
        createFile(srcMain.resolve("groovy").resolve("AFile.groovy"))

        then:
        ProjectUtils.hasJavaSource(project) == false

        when:
        createFile(srcMain.resolve("groovy").resolve("BFile.java"))

        then:
        ProjectUtils.hasJavaSource(project) == true
    }

}
