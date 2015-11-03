/*
 * Copyright 2014-2015 the original author or authors.
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

import groovy.util.logging.Slf4j
import nebula.test.IntegrationSpec
import org.ajoberstar.grgit.Grgit

@Slf4j
class DefaultsPluginSpec extends IntegrationSpec {

    def "Apply"() {
        writeHelloWorld('com.mooregreatsoftware.gradle.defaults')

        buildFile << """
            ${applyPlugin(DefaultsPlugin)}
            apply plugin: 'java'
        """.stripIndent()

        createGitRepo()
        createLicenseHeader()

        when:
        def result = runTasksSuccessfully('licenseFormat', 'classes')

        then:
        fileExists('build/classes/main/com/mooregreatsoftware/gradle/defaults/HelloWorld.class')
        println result.standardOutput
        result.wasExecuted(':classes')
    }


    void createLicenseHeader() {
        createFile("gradle/HEADER") << "THIS CAN BE USED FREELY"
    }


    void createGitRepo() {
        Grgit git = Grgit.init(dir: projectDir)
        git.repository.jgit.commit().with {
            message = 'initial'
            it
        }.call()
    }


    protected void writeHelloWorld(String packageDotted, File baseDir = getProjectDir()) {
        def path = 'src/main/java/' + packageDotted.replace('.', '/') + '/HelloWorld.java'
        def javaFile = createFile(path, baseDir)
        javaFile << """
            package ${packageDotted};

            public class HelloWorld {
                public static void main(String[] args) {
                    System.out.println("Hello Integration Test");
                }
            }
        """.stripIndent()
    }

}
