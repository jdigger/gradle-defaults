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
package com.mooregreatsoftware.gradle.checkerframework

import com.mooregreatsoftware.gradle.defaults.AbstractIntSpec
import com.mooregreatsoftware.gradle.defaults.DefaultsPlugin
import groovy.transform.CompileStatic

class CheckerFrameworkPluginIntSpec extends AbstractIntSpec {

    def "build"() {
        writeCheckerHelloWorld('com.mooregreatsoftware.gradle.defaults')

        buildFile << """
            apply plugin: '${CheckerFrameworkPlugin.PLUGIN_ID}'
        """.stripIndent()

        when:
        def result = runTasks('classes')

        then:
        result.success
        fileExists('build/classes/main/com/mooregreatsoftware/gradle/defaults/CheckerHelloWorld.class')

        cleanup:
        println result?.standardOutput
        println result?.standardError
    }


    def "with defaults"() {
        writeCheckerHelloWorld('com.mooregreatsoftware.gradle.defaults')

        buildFile << """
            apply plugin: '${DefaultsPlugin.PLUGIN_ID}'
            apply plugin: '${CheckerFrameworkPlugin.PLUGIN_ID}'

            group = "com.mooregreatsoftware.gradle.defaults"

            defaults {
                orgId = "tester"
                compatibilityVersion = 1.8
            }
        """.stripIndent()

        when:
        def result = runTasks('classes')

        then:
        result.success
        fileExists('build/classes/main/com/mooregreatsoftware/gradle/defaults/CheckerHelloWorld.class')

        cleanup:
        println result?.standardOutput
        println result?.standardError
    }


    @CompileStatic
    protected File writeCheckerHelloWorld(String packageDotted, File baseDir = getProjectDir()) {
        def path = 'src/main/java/' + packageDotted.replace('.', '/') + '/CheckerHelloWorld.java'
        def javaFile = createFile(path, baseDir)
        javaFile << """
        package ${packageDotted};

        public class CheckerHelloWorld {
            @org.checkerframework.checker.nullness.qual.Nullable String aVal;

            public static void main(String[] args) {
                CheckerHelloWorld world = new CheckerHelloWorld();
                world.aVal = "Hello, Checker Framework test";
            }
        }
        """.stripIndent()
        return javaFile
    }

}
