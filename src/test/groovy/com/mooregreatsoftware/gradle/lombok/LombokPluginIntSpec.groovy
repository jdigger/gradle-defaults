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
package com.mooregreatsoftware.gradle.lombok

import com.mooregreatsoftware.gradle.defaults.AbstractIntSpec
import com.mooregreatsoftware.gradle.defaults.DefaultsPlugin
import com.mooregreatsoftware.gradle.java.ExtJavaPlugin
import groovy.transform.CompileStatic

class LombokPluginIntSpec extends AbstractIntSpec {

    def "build"() {
        writeLombokHelloWorld('com.mooregreatsoftware.gradle.defaults')

        buildFile << """
            ${applyPlugin(DefaultsPlugin)}
            apply plugin: '${ExtJavaPlugin.PLUGIN_ID}'

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
        fileExists('build/classes/main/com/mooregreatsoftware/gradle/defaults/LombokHelloWorld.class')

        cleanup:
        println result?.standardOutput
        println result?.standardError
    }


    @CompileStatic
    protected File writeLombokHelloWorld(String packageDotted, File baseDir = getProjectDir()) {
        def path = 'src/main/java/' + packageDotted.replace('.', '/') + '/LombokHelloWorld.java'
        def javaFile = createFile(path, baseDir)
        javaFile << """
        package ${packageDotted};

        @lombok.Value
        public class LombokHelloWorld {
            String aVal;

            public static void main(String[] args) {
                lombok.val aStr = "Hello, Lombok test";
                new LombokHelloWorld(aStr);
            }
        }
        """.stripIndent()
        return javaFile
    }

}
