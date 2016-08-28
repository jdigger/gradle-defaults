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
class DefaultsPluginIntSpec extends AbstractConfigIntSpec {

    def "build"() {
        logLevel = LogLevel.DEBUG
        writeJavaHelloWorld('com.mooregreatsoftware.gradle.defaults')

        buildFile << """
            ${applyPlugin(DefaultsPlugin)}
            apply plugin: 'java'

            group = "com.mooregreatsoftware.gradle.defaults"

            defaults {
                orgId = "tester"
                compatibilityVersion = 1.7
                copyrightYears = '2014-2015'
            }
        """.stripIndent()

        createLicenseHeader()

        when:
        def result = runTasks('licenseFormat', 'build', 'idea')

        then:
        result.rethrowFailure()
        fileExists('build/classes/main/com/mooregreatsoftware/gradle/defaults/HelloWorldJava.class')
        result.wasExecuted(':classes')

        cleanup:
        println result?.standardOutput
        println result?.standardError
    }


    def "build kotlin"() {
        logLevel = LogLevel.DEBUG
        fork = true // has Xerces errors without this
        writeKotlinHelloWorld('com.mooregreatsoftware.gradle.defaults')

        buildFile << """
            buildscript {
                ext.kotlin_version = '1.0.3'
                repositories {
                    jcenter()
                }

                dependencies {
                    classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:\$kotlin_version"
                }
            }
            ${applyPlugin(DefaultsPlugin)}
            apply plugin: 'kotlin'

            group = "com.mooregreatsoftware.gradle.defaults"

            defaults {
                orgId = "tester"
                compatibilityVersion = 1.8
                copyrightYears = '2014-2016'
            }

            dependencies {
                compile "org.jetbrains.kotlin:kotlin-stdlib:\$kotlin_version"
            }
        """.stripIndent()

        createLicenseHeader()

        when:
        def result = runTasks('licenseFormat', 'build')

        then:
        result.rethrowFailure()
        fileExists('build/classes/main/com/mooregreatsoftware/gradle/defaults/HelloWorldKotlinKt.class')
        result.wasExecuted(':classes')

        cleanup:
        println result?.standardOutput
        println result?.standardError
    }


    def "build scala"() {
        logLevel = LogLevel.DEBUG
        fork = true // has TravisCI errors without this
        writeScalaHelloWorld('com.mooregreatsoftware.gradle.defaults')

        buildFile << """
            ${applyPlugin(DefaultsPlugin)}
            apply plugin: 'scala'

            group = "com.mooregreatsoftware.gradle.defaults"

            defaults {
                orgId = "tester"
                compatibilityVersion = 1.8
                copyrightYears = '2014-2016'
            }

            dependencies {
                compile "org.scala-lang:scala-library:2.10.5"
            }
        """.stripIndent()

        createLicenseHeader()

        when:
        def result = runTasks('licenseFormat', 'build')

        then:
        result.rethrowFailure()
        fileExists('build/classes/main/com/mooregreatsoftware/gradle/defaults/HelloWorldScala.class')
        result.wasExecuted(':classes')

        cleanup:
        println result?.standardOutput
        println result?.standardError
    }


    def "release open source"() {
        logLevel = LogLevel.DEBUG
        writeJavaHelloWorld('com.mooregreatsoftware.gradle.defaults')

        buildFile << """
            plugins {
                id 'com.jfrog.bintray' version '1.7.1'
            }

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
                orgId = "tester"
                openSource = true
                bintrayRepo = "java"
                compatibilityVersion = 1.8
                orgName = "testing org"
            }
        """.stripIndent()

        createLicenseHeader()

        def javaProjDir = addSubproject("javaProj")
        createFile("build.gradle", javaProjDir) << """
            apply plugin: 'java'
        """.stripIndent()
        writeJavaHelloWorld('com.mooregreatsoftware.gradle.defaults.javaproj', javaProjDir)

        def otherlangsProjDir = addSubproject("otherlangsProj")
        createFile("build.gradle", otherlangsProjDir) << """
            apply plugin: 'groovy'

            dependencies {
                compile "org.codehaus.groovy:groovy:2.4.4"
            }
        """.stripIndent()
        writeGroovyHelloWorld('com.mooregreatsoftware.gradle.defaults.otherlangsProj', otherlangsProjDir)

        git.add().addFilepattern(".").call()
        git.commit().setMessage("the files").call()

        when:
        def result = runTasks('licenseFormat', 'generatePomFileForMainPublication', 'release',
            '-x', 'bintrayUpload', '-x', 'prepareGhPages', '-Prelease.scope=patch', '-Prelease.stage=final')

        then:
        result.rethrowFailure()
        fileExists('javaProj/build/classes/main/com/mooregreatsoftware/gradle/defaults/javaproj/HelloWorldJava.class')
        fileExists('otherlangsProj/build/classes/main/com/mooregreatsoftware/gradle/defaults/otherlangsProj/HelloWorldGroovy.class')

        cleanup:
        println result?.standardOutput
        println result?.standardError
    }


    @CompileStatic
    void createLicenseHeader() {
        createFile("gradle/HEADER") << 'Copyright ${year} the original author or authors.\nTHIS CAN BE USED FREELY'
    }

}
