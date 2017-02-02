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
package com.mooregreatsoftware.gradle.license

import com.mooregreatsoftware.gradle.defaults.AbstractIntSpec
import com.mooregreatsoftware.gradle.defaults.DefaultsPlugin

class ExtLicensePluginIntSpec extends AbstractIntSpec {

    def "licenseFormat"() {
        def sourceFile = writeJavaHelloWorld('com.mooregreatsoftware.gradle.defaults')

        buildFile << """
            ${applyPlugin(DefaultsPlugin)}
            apply plugin: 'java'

            group = "com.mooregreatsoftware.gradle.defaults"

            defaults {
                orgId = "tester"
                compatibilityVersion = 1.7
                copyrightYears = '2014-2016'
            }
        """.stripIndent()

        def subprojDir = addSubproject("submod", """
            apply plugin: 'java'
        """.stripIndent())
        def subSourceFile = writeJavaHelloWorld('com.mooregreatsoftware.gradle.defaults.asubmod', subprojDir)
        def propFile = createFile("src/main/java/apropertyfile.properties", subprojDir) << """
            name=something
        """.stripIndent()

        createLicenseHeader()

        expect:
        sourceFile.readLines().find({ it.contains("Copyright ") }) == null
        subSourceFile.readLines().find({ it.contains("Copyright ") }) == null
        propFile.readLines().find({ it.contains("Copyright ") }) == null

        when:
        def result = runTasks('licenseFormat')

        then:
        result.success
        sourceFile.readLines().find({ it.contains("* Copyright 2014-2016") }) != null
        subSourceFile.readLines().find({ it.contains("* Copyright 2014-2016") }) != null
        propFile.readLines().find({ it.contains("Copyright ") }) == null

        cleanup:
        println result?.standardOutput
        println result?.standardError
    }

}
