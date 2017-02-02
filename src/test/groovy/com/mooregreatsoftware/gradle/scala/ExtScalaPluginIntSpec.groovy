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
package com.mooregreatsoftware.gradle.scala

import com.mooregreatsoftware.gradle.defaults.AbstractIntSpec
import com.mooregreatsoftware.gradle.defaults.DefaultsPlugin

class ExtScalaPluginIntSpec extends AbstractIntSpec {

    def "build"() {
        writeScalaHelloWorld('com.mooregreatsoftware.gradle.defaults')

        buildFile << """
            ${applyPlugin(DefaultsPlugin)}
            apply plugin: '${ExtScalaPlugin.PLUGIN_ID}'

            group = "com.mooregreatsoftware.gradle.defaults"

            defaults {
                orgId = "tester"
                compatibilityVersion = 1.7
            }

            dependencies {
                compile "org.scala-lang:scala-library:2.10.5"
            }
        """.stripIndent()

        def subprojDir = addSubproject("submod", """
            apply plugin: 'scala'

            dependencies {
                compile "org.scala-lang:scala-library:2.10.5"
            }
        """.stripIndent())
        writeScalaHelloWorld('com.mooregreatsoftware.gradle.defaults.asubmod', subprojDir)

        when:
        def result = runTasks('assemble')

        then:
        result.success
        fileExists('build/classes/main/com/mooregreatsoftware/gradle/defaults/HelloWorldScala.class')
        fileExists('submod/build/classes/main/com/mooregreatsoftware/gradle/defaults/asubmod/HelloWorldScala.class')
        [":compileJava", ":submod:compileScala", ":sourcesJar", ":javadocJar", ":scaladocJar", ":submod:sourcesJar", ":submod:scaladocJar"].each {
            assert result.wasExecuted(it)
        }

        cleanup:
        println result?.standardOutput
        println result?.standardError
    }

}
