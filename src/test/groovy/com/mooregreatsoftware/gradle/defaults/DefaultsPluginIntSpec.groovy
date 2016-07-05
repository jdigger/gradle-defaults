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

import groovy.transform.CompileStatic
import org.gradle.api.logging.LogLevel
import spock.lang.Subject

@Subject(DefaultsPlugin)
class DefaultsPluginIntSpec extends AbstractConfigSpec {

    def "build"() {
        logLevel = LogLevel.DEBUG
        writeJavaHelloWorld('com.mooregreatsoftware.gradle.defaults')

        buildFile << """
            ${applyPlugin(DefaultsPlugin)}
            apply plugin: 'java'

            defaults {
                id = "tester"
                compatibilityVersion = 1.7
                copyrightYears = '2014-2015'
            }
        """.stripIndent()

        createLicenseHeader()

        when:
        def result = runTasks('licenseFormat', 'build', 'idea')

        then:
        result.success
        fileExists('build/classes/main/com/mooregreatsoftware/gradle/defaults/HelloWorld.class')
        result.wasExecuted(':classes')

        cleanup:
        println result?.standardOutput
        println result?.standardError
    }


    def "release"() {
        logLevel = LogLevel.INFO
        writeJavaHelloWorld('com.mooregreatsoftware.gradle.defaults')

        buildFile << """
            plugins {
                id 'com.jfrog.bintray' version '1.4'
            }
            apply plugin: 'java'

            group = 'com.mooregreatsoftware.gradle.defaults'
            description = 'Nice Gradle defaults'

            bintray {
                pkg {
                    licenses = ['Apache-2.0']
                    attributes = ['plat': ['jvm']]
                }
            }

            ${applyPlugin(DefaultsPlugin)}
            defaults {
                id = "tester"
                compatibilityVersion = 1.7
                orgName = "testing org"
            }
        """.stripIndent()

        createLicenseHeader()

        git.add().addFilepattern(".").call()
        git.commit().setMessage("the files").call()

        when:
        def result = runTasks('licenseFormat', 'generatePomFileForMainPublication', 'release',
            '-x', 'bintrayUpload', '-x', 'prepareGhPages', '-Prelease.scope=patch', '-Prelease.stage=final')

        then:
        result.success
        fileExists('build/classes/main/com/mooregreatsoftware/gradle/defaults/HelloWorld.class')
        result.wasExecuted(':classes')

        cleanup:
        println result.standardOutput
        println result.standardError
    }


    @CompileStatic
    void createLicenseHeader() {
        createFile("gradle/HEADER") << 'Copyright ${year} the original author or authors.\nTHIS CAN BE USED FREELY'
    }

}
