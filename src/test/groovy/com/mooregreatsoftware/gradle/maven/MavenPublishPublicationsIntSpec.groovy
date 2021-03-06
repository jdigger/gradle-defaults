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
package com.mooregreatsoftware.gradle.maven

import com.mooregreatsoftware.gradle.defaults.AbstractIntSpec
import com.mooregreatsoftware.gradle.defaults.DefaultsPlugin
import nebula.test.functional.ExecutionResult

@SuppressWarnings("GroovyPointlessBoolean")
class MavenPublishPublicationsIntSpec extends AbstractIntSpec {

    @SuppressWarnings("GroovyAssignabilityCheck")
    void "good POM generation"() {
        writeJavaHelloWorld('com.mooregreatsoftware.gradle.defaults')

        buildFile << """
            apply plugin: '${DefaultsPlugin.PLUGIN_ID}'
            apply plugin: 'java'

            // these are after applying plugin to verify they get picked up for POM
            group = "com.mooregreatsoftware.gradle.defaults"
            description = "a test project"

            defaults {
                orgId = "testing"
                compatibilityVersion = 1.7
            }

            dependencies {
                compile "org.scala-lang:scala-library:2.10.5"
            }
        """.stripIndent()

        def subprojDir = addSubproject("submod", """
            apply plugin: 'java'
        """.stripIndent())
        writeJavaHelloWorld('com.mooregreatsoftware.gradle.defaults.asubmod', subprojDir)

        when:
        def result = runTasks('generatePomFileForMainPublication')

        then:
        result.success

        when:
        def pomFile = file('build/publications/main/pom-default.xml')

        then:
        pomFile.exists()

        when:
        def xml = new XmlParser(false, false).parse(pomFile)

        then:
        xml.groupId.text() == 'com.mooregreatsoftware.gradle.defaults'
        xml.artifactId.text() == moduleName
        xml.description.text() == "a test project"
        xml.version.text().trim().isEmpty() == false
        xml.dependencies.size() == 1
        xml.dependencies[0].dependency.groupId.text() == "org.scala-lang"

        cleanup:
        println result?.standardOutput
        println result?.standardError
    }


    def "good POM generation - auto group discovery"() {
        writeJavaHelloWorld('com.mooregreatsoftware.gradle.defaults.test')

        buildFile << """
            apply plugin: '${DefaultsPlugin.PLUGIN_ID}'
            apply plugin: 'java'

            defaults {
                orgId = "testing"
                compatibilityVersion = 1.7
            }
        """.stripIndent()

        def subprojDir = addSubproject("submod", """
            apply plugin: 'java'
        """.stripIndent())
        writeJavaHelloWorld('com.mooregreatsoftware.gradle.defaults.asubmod', subprojDir)

        when:
        def result = runTasks('generatePomFileForMainPublication')

        then:
        result.success

        when:
        def pomFile = file('build/publications/main/pom-default.xml')

        then:
        pomFile.exists()

        when:
        def xml = new XmlParser(false, false).parse(pomFile)

        then:
        xml.groupId.text() == 'com.mooregreatsoftware.gradle.defaults.test'
        xml.artifactId.text() == moduleName
        xml.version.text().trim().isEmpty() == false

        cleanup:
        println result?.standardOutput
        println result?.standardError
    }


    def "bad POM generation - no id"() {
        writeJavaHelloWorld('com.mooregreatsoftware.gradle.defaults')

        buildFile << """
            apply plugin: '${DefaultsPlugin.PLUGIN_ID}'
            apply plugin: 'java'

            group = 'com.mooregreatsoftware.gradle.defaults'

            defaults {
                compatibilityVersion = 1.7
            }
        """.stripIndent()

        def subprojDir = addSubproject("submod", """
            apply plugin: 'java'
        """.stripIndent())
        writeJavaHelloWorld('com.mooregreatsoftware.gradle.defaults.asubmod', subprojDir)

        when:
        def result = runTasks('generatePomFileForMainPublication')

        then:
        result.success == false
        rootCause(result).message.contains('"orgId" is not set')

//        cleanup:
//        println result?.standardOutput
//        println result?.standardError
    }


    protected static Throwable rootCause(ExecutionResult result) {
        def exp = result.failure
        if (exp == null) return null
        while (exp.cause != null) {
            exp = exp.cause
        }
        return exp
    }

}
