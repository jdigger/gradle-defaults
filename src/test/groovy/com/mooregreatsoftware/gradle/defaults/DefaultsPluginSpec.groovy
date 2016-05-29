/*
 * Copyright 2014-2015 the original author or authors.
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
package com.mooregreatsoftware.gradle.defaults

import com.energizedwork.spock.extensions.TempDirectory
import groovy.transform.CompileStatic
import nebula.test.IntegrationSpec
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.URIish
import org.gradle.api.logging.LogLevel

class DefaultsPluginSpec extends IntegrationSpec {
    @TempDirectory(clean = false, baseDir = 'build/git-bare')
    protected File remoteOrigin

    Git git


    @CompileStatic
    def setup() {
        // logLevel = LogLevel.DEBUG

        git = Git.init().setDirectory(projectDir).call()
        createFile(".gitignore", projectDir) << """
            .gradle-test-kit/
        """.stripIndent()
        git.add().addFilepattern(".gitignore").call()
        git.commit().setMessage("init").call()
        git.tag().setName("v1.0.0").call()
        createRemoteOrigin(git)
        git.push().setRemote("origin").setPushAll().call()
    }


    def "build"() {
        logLevel = LogLevel.DEBUG
        writeHelloWorld('com.mooregreatsoftware.gradle.defaults')

        buildFile << """
            ${applyPlugin(DefaultsPlugin.class)}
            apply plugin: 'java'

            defaults {
                compatibilityVersion = 1.7
            }
        """.stripIndent()

        createLicenseHeader()

        when:
        def result = runTasksSuccessfully('licenseFormat', 'build')

        then:
        fileExists('build/classes/main/com/mooregreatsoftware/gradle/defaults/HelloWorld.class')
        result.wasExecuted(':classes')

        cleanup:
        println result.standardOutput
        println result.standardError
    }


    def "release"() {
        writeHelloWorld('com.mooregreatsoftware.gradle.defaults')

        buildFile << """
            plugins {
                id 'com.jfrog.bintray' version '1.4'
            }
            apply plugin: 'java'

            group = 'com.mooregreatsoftware.gradle.defaults'
            description = 'Nice Gradle defaults'

            bintray {
                pkg {
                    licenses = ['Apache-2.0']
                    attributes = ['plat': ['jvm']]
                }
            }

            ${applyPlugin(DefaultsPlugin.class)}
            defaults {
                compatibilityVersion = 1.7
                orgName = "testing org"
            }
        """.stripIndent()

        createLicenseHeader()

        git.add().addFilepattern(".").call()
        git.commit().setMessage("the files").call()

        when:
        def result = runTasks('licenseFormat', 'generatePomFileForMainPublication', 'release',
            '-x', 'bintrayUpload', '-x', 'prepareGhPages', '-Prelease.scope=patch', '-Prelease.stage=final')

        then:
        result.failure == null
        fileExists('build/classes/main/com/mooregreatsoftware/gradle/defaults/HelloWorld.class')
        result.wasExecuted(':classes')

        cleanup:
        println result.standardOutput
        println result.standardError
    }


    @CompileStatic
    void createLicenseHeader() {
        createFile("gradle/HEADER") << "THIS CAN BE USED FREELY"
    }


    @CompileStatic
    protected void writeHelloWorld(String packageDotted, File baseDir = getProjectDir()) {
        def path = 'src/main/java/' + packageDotted.replace('.', '/') + '/HelloWorld.java'
        def javaFile = createFile(path, baseDir)
        javaFile << """
            package ${packageDotted};

            public class HelloWorld {
                public static void main(String[] args) {
                    System.out.println("Hello Integration Test");
                }
            }
        """.stripIndent()
    }


    @CompileStatic
    void createRemoteOrigin(Git git) {
        def remoteGit = Git.init().setDirectory(remoteOrigin).setBare(true).call()
        def remotePath = remoteGit.repository.directory.absolutePath
        def uri = new URIish("file://${remotePath}")

        def config = git.repository.config
        config.setString("remote", "origin", "url", uri.toString())
        config.setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*")
        config.setString("branch", "master", "remote", "origin")
        config.setString("branch", "master", "merge", "refs/heads/master")
        config.save()
    }

}
