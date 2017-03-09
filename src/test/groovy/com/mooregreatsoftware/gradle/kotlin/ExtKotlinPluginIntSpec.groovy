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
package com.mooregreatsoftware.gradle.kotlin

import com.mooregreatsoftware.gradle.defaults.AbstractIntSpec

class ExtKotlinPluginIntSpec extends AbstractIntSpec {

    def "build"() {
        writeKotlinHelloWorld('com.mooregreatsoftware.gradle.defaults')
        writeHelloWorld('com.mooregreatsoftware.gradle.defaults', projectDir)

        buildFile << """
            buildscript {
                ext.kotlin_version = '1.1.0'
                dependencies {
                    classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:\$kotlin_version"
                }
            }
            apply plugin: '${ExtKotlinPlugin.PLUGIN_ID}'

            dependencies {
                compile "org.jetbrains.kotlin:kotlin-stdlib:\$kotlin_version"
            }
        """.stripIndent()

        def subprojDir = addSubproject("submod", """
            buildscript {
                ext.kotlin_version = '1.1.0'
                dependencies {
                    classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:\$kotlin_version"
                }
            }
            apply plugin: 'kotlin'

            dependencies {
                compile "org.jetbrains.kotlin:kotlin-stdlib:\$kotlin_version"
            }
        """.stripIndent())
        writeKotlinHelloWorld('com.mooregreatsoftware.gradle.defaults.asubmod', subprojDir)

        when:
        def result = runTasks('assemble')

        then:
        result.success
        fileExists('build/classes/main/com/mooregreatsoftware/gradle/defaults/HelloWorldKotlinKt.class')
        fileExists('build/classes/main/com/mooregreatsoftware/gradle/defaults/HelloWorld.class')
        fileExists('submod/build/classes/main/com/mooregreatsoftware/gradle/defaults/asubmod/HelloWorldKotlinKt.class')
        [":compileKotlin", ":submod:compileKotlin", ":sourcesJar", ":jar", ":submod:jar"].each {
            assert result.wasExecuted(it)
        }

        cleanup:
        println result?.standardOutput
        println result?.standardError
    }

}
