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
package com.mooregreatsoftware.gradle.release

import com.mooregreatsoftware.gradle.defaults.AbstractIntSpec
import groovy.transform.CompileStatic
import org.gradle.api.logging.LogLevel
import spock.lang.Subject

@Subject(ExtReleasePlugin)
class ExtReleasePluginIntSpec extends AbstractIntSpec {

    def "bare release"() {
        logLevel = LogLevel.INFO

        buildFile << """
            apply plugin: "${ExtReleasePlugin.PLUGIN_ID}"
        """.stripIndent()

        createLicenseHeader()

        def javaProjDir = addSubproject("javaProj")
        createFile("build.gradle", javaProjDir) << """
            apply plugin: 'java'
        """.stripIndent()
        writeJavaHelloWorld('com.mooregreatsoftware.gradle.defaults.javaproj', javaProjDir)

        def otherlangsProjDir = addSubproject("otherlangsProj")
        createFile("build.gradle", otherlangsProjDir) << """
            apply plugin: 'groovy'

            dependencies {
                compile "org.codehaus.groovy:groovy-all:2.4.4"
            }
        """.stripIndent()
        writeGroovyHelloWorld('com.mooregreatsoftware.gradle.defaults.otherlangsProj', otherlangsProjDir)

        git.add().addFilepattern(".").call()
        git.commit().setMessage("the files").call()

        when:
        def result = runTasks('release',
            '-Prelease.scope=patch', '-Prelease.stage=final')

        then:
        result.rethrowFailure()
        result.wasExecuted(":javaProj:clean")
        result.wasExecuted(":javaProj:build")
        result.wasExecuted(":otherlangsProj:clean")
        result.wasExecuted(":otherlangsProj:build")
        fileExists('javaProj/build/classes/main/com/mooregreatsoftware/gradle/defaults/javaproj/HelloWorldJava.class')
        fileExists('otherlangsProj/build/classes/main/com/mooregreatsoftware/gradle/defaults/otherlangsProj/HelloWorldGroovy.class')
        parseVersion(result.standardOutput) == '1.0.1'

        cleanup:
        println result?.standardOutput
        println result?.standardError
    }


    @CompileStatic
    static String parseVersion(String stdout) {
        def matcher = stdout =~ /(?s).*Inferred project: [^,]*, version:\s+(\d\S+)\n.*/
        return matcher.matches() ? matcher.group(1) : "NO_MATCH"
    }


    @CompileStatic
    void createLicenseHeader() {
        createFile("gradle/HEADER") << 'Copyright ${year} the original author or authors.\nTHIS CAN BE USED FREELY'
    }

}
