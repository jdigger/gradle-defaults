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
package com.mooregreatsoftware.gradle.groovy

import com.mooregreatsoftware.gradle.defaults.AbstractIntSpec
import com.mooregreatsoftware.gradle.defaults.DefaultsPlugin

class ExtGroovyPluginIntSpec extends AbstractIntSpec {

    def "build"() {
        writeGroovyHelloWorld('com.mooregreatsoftware.gradle.defaults')

        buildFile << """
            apply plugin: '${ExtGroovyPlugin.PLUGIN_ID}'

            dependencies {
                compile "org.codehaus.groovy:groovy-all:2.4.4"
            }
        """.stripIndent()

        def subprojDir = addSubproject("submod", """
            apply plugin: 'groovy'

            dependencies {
                compile "org.codehaus.groovy:groovy-all:2.4.4"
            }
        """.stripIndent())
        writeGroovyHelloWorld('com.mooregreatsoftware.gradle.defaults.asubmod', subprojDir)

        when:
        def result = runTasks('assemble')

        then:
        result.success
        fileExists('build/classes/main/com/mooregreatsoftware/gradle/defaults/HelloWorldGroovy.class')
        fileExists('submod/build/classes/main/com/mooregreatsoftware/gradle/defaults/asubmod/HelloWorldGroovy.class')
        [":compileJava", ":submod:compileGroovy", ":sourcesJar", ":groovydocJar"].each {
            assert result.wasExecuted(it)
        }

        cleanup:
        println result?.standardOutput
        println result?.standardError
    }


    def "build with defaults"() {
        writeGroovyHelloWorld('com.mooregreatsoftware.gradle.defaults')

        buildFile << """
            ${applyPlugin(DefaultsPlugin)}
            apply plugin: 'groovy'

            defaults {
                orgId = "tester"
                compatibilityVersion = 1.7
            }

            dependencies {
                compile "org.codehaus.groovy:groovy-all:2.4.4"
            }
        """.stripIndent()

        def subprojDir = addSubproject("submod", """
            apply plugin: 'groovy'

            dependencies {
                compile "org.codehaus.groovy:groovy-all:2.4.4"
            }
        """.stripIndent())
        writeGroovyHelloWorld('com.mooregreatsoftware.gradle.defaults.asubmod', subprojDir)

        when:
        def result = runTasks('assemble')

        then:
        result.success
        fileExists('build/classes/main/com/mooregreatsoftware/gradle/defaults/HelloWorldGroovy.class')
        fileExists('submod/build/classes/main/com/mooregreatsoftware/gradle/defaults/asubmod/HelloWorldGroovy.class')
        [":compileJava", ":submod:compileGroovy", ":sourcesJar", ":javadocJar", ":groovydocJar", ":submod:sourcesJar", ":submod:groovydocJar"].each {
            assert result.wasExecuted(it)
        }

        cleanup:
        println result?.standardOutput
        println result?.standardError
    }

}
