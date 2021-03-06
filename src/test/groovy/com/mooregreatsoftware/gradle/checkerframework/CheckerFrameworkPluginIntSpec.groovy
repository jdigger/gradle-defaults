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

class CheckerFrameworkPluginIntSpec extends AbstractIntSpec {

    def "build"() {
        writeCheckerHelloWorld('com.mooregreatsoftware.gradle.defaults')

        buildFile << """
            apply plugin: '${CheckerFrameworkPlugin.PLUGIN_ID}'
        """.stripIndent()

        when:
        // the 'java' plugin is applied automatically
        def result = runTasks('classes')

        then:
        result.success
        fileExists('build/classes/main/com/mooregreatsoftware/gradle/defaults/CheckerHelloWorld.class')

        cleanup:
        println result?.standardOutput
        println result?.standardError
    }

}
