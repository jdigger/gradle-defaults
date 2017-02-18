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
package com.mooregreatsoftware.gradle.java

import com.mooregreatsoftware.gradle.defaults.AbstractIntSpec
import com.mooregreatsoftware.gradle.defaults.DefaultsPlugin

class ExtJavaPluginIntSpec extends AbstractIntSpec {

    def "build"() {
        writeJavaHelloWorld('com.mooregreatsoftware.gradle.defaults')

        buildFile << """
            ${applyPlugin(DefaultsPlugin)}
            apply plugin: '${ExtJavaPlugin.PLUGIN_ID}'

            group = "com.mooregreatsoftware.gradle.defaults"

            defaults {
                orgId = "tester"
                compatibilityVersion = 1.7
            }
        """.stripIndent()

        def subprojDir = addSubproject("submod", """
            apply plugin: 'java'
        """.stripIndent())
        writeJavaHelloWorld('com.mooregreatsoftware.gradle.defaults.asubmod', subprojDir)

        when:
        def result = runTasks('assemble')

        then:
        result.success
        fileExists('build/classes/main/com/mooregreatsoftware/gradle/defaults/HelloWorldJava.class')
        fileExists('submod/build/classes/main/com/mooregreatsoftware/gradle/defaults/asubmod/HelloWorldJava.class')
        [":compileJava", ":submod:compileJava", ":sourcesJar", ":javadocJar", ":submod:sourcesJar", ":submod:javadocJar"].each {
            assert result.wasExecuted(it)
        }
//        result.standardOutput.readLines().find({
//            it.contains("Compiler arguments: -source 1.7 -target 1.7 ") && it.contains("submod")
//        })

        cleanup:
        println result?.standardOutput
        println result?.standardError
    }


    def "normal java plugin doesn't break things"() {
        writeJavaHelloWorld('com.mooregreatsoftware.gradle.defaults')

        buildFile << """
            ${applyPlugin(DefaultsPlugin)}
            apply plugin: 'java'

            group = "com.mooregreatsoftware.gradle.defaults"

            defaults {
                orgId = "tester"
                compatibilityVersion = 1.7
            }
        """.stripIndent()

        def subprojDir = addSubproject("submod", """
            apply plugin: 'java'
        """.stripIndent())
        writeJavaHelloWorld('com.mooregreatsoftware.gradle.defaults.asubmod', subprojDir)

        when:
        def result = runTasks('assemble')

        then:
        result.success
        fileExists('build/classes/main/com/mooregreatsoftware/gradle/defaults/HelloWorldJava.class')
        fileExists('submod/build/classes/main/com/mooregreatsoftware/gradle/defaults/asubmod/HelloWorldJava.class')
        [":compileJava", ":submod:compileJava", ":sourcesJar", ":javadocJar", ":submod:sourcesJar", ":submod:javadocJar"].each {
            assert result.wasExecuted(it)
        }
//        result.standardOutput.readLines().find({
//            it.contains("Compiler arguments: -source 1.7 -target 1.7 ") && it.contains("submod")
//        })

        cleanup:
        println result?.standardOutput
        println result?.standardError
    }

}
