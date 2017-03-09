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
package com.mooregreatsoftware.gradle.bintray

import com.mooregreatsoftware.gradle.defaults.AbstractIntSpec
import com.mooregreatsoftware.gradle.java.ExtJavaPlugin
import org.gradle.api.logging.LogLevel

import java.nio.file.Files

class ExtBintrayPluginIntSpec extends AbstractIntSpec {

    def "setting BinTray user and apiKey in GRADLE_USER_HOME/gradle.properties"() {
        fork = true // prevent Xerces errors

        Files.write(gradleUserHome.resolve("gradle.properties"), "bintrayUser=tester\nbintrayKey=testerKey".bytes)

        writeJavaHelloWorld('com.mooregreatsoftware.gradle.defaults')

        def bintrayServer = new StubBintrayServer()
        bintrayServer.start()
        def port = bintrayServer.listeningPort

        buildFile << """
            buildscript {
                dependencies {
                    classpath 'com.jfrog.bintray.gradle:gradle-bintray-plugin:1.7.3'
                }
            }

            apply plugin: "${ExtBintrayPlugin.PLUGIN_ID}"
            apply plugin: "${ExtJavaPlugin.PLUGIN_ID}"

            group = "com.mooregreatsoftware.gradle.defaults"

            defaults {
                orgId = "tester"
                bintrayRepo = 'java-test'
            }

            bintray.apiUrl = 'http://localhost:${port}/test-api'
        """.stripIndent()

        when:
        def result = runTasks('bintrayUpload')

        then:
        result.success
        bintrayServer.putCount == 4 // one PUT per file being uploaded
        bintrayServer.postCount >= 1 // one POST to send "publish" command, but there's a (static) bug in the base plugin that retains publish requests

        cleanup:
        bintrayServer?.stop()
        println result?.standardOutput
        println result?.standardError
    }


    def "setting BinTray user and apiKey in project gradle.properties"() {
        fork = true // prevent Xerces errors

        Files.write(projectDir.toPath().resolve("gradle.properties"), "bintrayUser=tester\nbintrayKey=testerKey".bytes)

        writeJavaHelloWorld('com.mooregreatsoftware.gradle.defaults')

        def bintrayServer = new StubBintrayServer()
        bintrayServer.start()
        def port = bintrayServer.listeningPort

        buildFile << """
            buildscript {
                dependencies {
                    classpath 'com.jfrog.bintray.gradle:gradle-bintray-plugin:1.7.3'
                }
            }

            apply plugin: "${ExtBintrayPlugin.PLUGIN_ID}"
            apply plugin: "${ExtJavaPlugin.PLUGIN_ID}"

            group = "com.mooregreatsoftware.gradle.defaults"

            defaults {
                orgId = "tester"
                bintrayRepo = 'java-test'
            }

            bintray.apiUrl = 'http://localhost:${port}/test-api'
        """.stripIndent()

        when:
        def result = runTasks('bintrayUpload')

        then:
        result.success
        bintrayServer.putCount == 4 // one PUT per file being uploaded
        bintrayServer.postCount >= 1 // one POST to send "publish" command, but there's a (static) bug in the base plugin that retains publish requests

        cleanup:
        bintrayServer?.stop()
        println result?.standardOutput
        println result?.standardError
    }

}
