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
package com.mooregreatsoftware.gradle.ide

import com.mooregreatsoftware.gradle.defaults.AbstractIntSpec

class IntellijPluginIntSpec extends AbstractIntSpec {

    def "build"() {
        writeJavaHelloWorld('com.mooregreatsoftware.gradle.defaults')

        buildFile << """
            apply plugin: '${ExtIntellijPlugin.PLUGIN_ID}'
            apply plugin: 'java'
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
        def componetChildren = xml.component.'**'
        componetChildren.find { it.@name == "OTHER_INDENT_OPTIONS" } != null

        cleanup:
        println result?.standardOutput
        println result?.standardError
    }

}
