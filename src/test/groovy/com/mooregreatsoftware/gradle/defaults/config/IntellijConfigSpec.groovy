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
package com.mooregreatsoftware.gradle.defaults.config

import com.mooregreatsoftware.gradle.defaults.AbstractConfigSpec
import com.mooregreatsoftware.gradle.defaults.DefaultsPlugin

class IntellijConfigSpec extends AbstractConfigSpec {

    def "build"() {
        writeJavaHelloWorld('com.mooregreatsoftware.gradle.defaults')

        buildFile << """
            ${applyPlugin(DefaultsPlugin.class)}
            apply plugin: 'java'

            defaults {
                id = "tester"
                compatibilityVersion = 1.7
            }
        """.stripIndent()

        addSubproject("submod")

        when:
        def result = runTasks('idea')

        then:
        result.success
        fileExists("${moduleName}.ipr")
        result.wasExecuted(':ideaProject')

        when:
        def xml = new XmlParser(false, false).parse(new File(projectDir, "${moduleName}.ipr"))

        then:
        xml.component.find { it.@name == "GradleSettings" } != null
        xml.component.'**'.find { it.@name == "OTHER_INDENT_OPTIONS" } != null

        cleanup:
        println result?.standardOutput
        println result?.standardError
    }

}
