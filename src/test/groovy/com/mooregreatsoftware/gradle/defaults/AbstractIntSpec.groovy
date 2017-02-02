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
package com.mooregreatsoftware.gradle.defaults

import com.energizedwork.spock.extensions.TempDirectory
import com.mooregreatsoftware.TestUtils
import groovy.transform.CompileStatic
import nebula.test.IntegrationSpec
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.URIish
import org.gradle.api.logging.LogLevel

import java.nio.file.Files
import java.nio.file.Path

abstract class AbstractIntSpec extends IntegrationSpec {
    @TempDirectory(clean = false, baseDir = 'build/git-bare')
    protected File remoteOrigin

    protected Path gradleUserHome

    Git git


    @CompileStatic
    setup() {
        logLevel = LogLevel.DEBUG

        gradleUserHome = projectDir.toPath().resolve(".gradle-home")
        Files.createDirectories(gradleUserHome)

        createFile("test-init.gradle", projectDir) << """
            startParameter.offline=true
            startParameter.refreshDependencies=true
            startParameter.gradleUserHomeDir=file('${gradleUserHome}')

            apply plugin:TestRepositoryPlugin

            class TestRepositoryPlugin implements Plugin<Gradle> {
                void apply(Gradle gradle) {
                    gradle.allprojects{ project ->
                        buildscript {
                            repositories {
                                maven { url '${TestUtils.testLibsPath()}' }
                                flatDir { dir '${TestUtils.testLibsPath()}' }
                            }
                        }
                        project.repositories {
                            // Remove all repositories not pointing to the enterprise repository url
                            all { ArtifactRepository repo ->
                                if ((repo instanceof MavenArtifactRepository) && (!repo.url.toString().contains("test-libs"))) {
                                    project.logger.lifecycle "Repository \${repo.url} removed. Only test repo allowed"
                                    remove repo
                                }
                            }

                            maven { url '${TestUtils.testLibsPath()}' }
                            flatDir { dir '${TestUtils.testLibsPath()}' }
                        }
                    }
                }
            }
        """.stripIndent()
        addInitScript(new File(projectDir, "test-init.gradle"))

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


    @CompileStatic
    void createLicenseHeader() {
        createFile("gradle/HEADER") << 'Copyright ${year} the original author or authors.\nTHIS CAN BE USED FREELY'
    }


    @CompileStatic
    protected File writeJavaHelloWorld(String packageDotted, File baseDir = getProjectDir()) {
        def path = 'src/main/java/' + packageDotted.replace('.', '/') + '/HelloWorldJava.java'
        def srcFile = createFile(path, baseDir)
        srcFile << """
            package ${packageDotted};

            public class HelloWorldJava {
                public static void main(String[] args) {
                    System.out.println("Hello Integration Test");
                }
            }
        """.stripIndent()
        return srcFile
    }


    @CompileStatic
    protected File writeGroovyHelloWorld(String packageDotted, File baseDir = getProjectDir()) {
        def path = 'src/main/groovy/' + packageDotted.replace('.', '/') + '/HelloWorldGroovy.groovy'
        def srcFile = createFile(path, baseDir)
        srcFile << """
            package ${packageDotted}

            @groovy.transform.CompileStatic
            public class HelloWorldGroovy {
                public static void main(String[] args) {
                    println("Hello Integration Test")
                }
            }
        """.stripIndent()
        return srcFile
    }


    @CompileStatic
    protected File writeScalaHelloWorld(String packageDotted, File baseDir = getProjectDir()) {
        def path = 'src/main/scala/' + packageDotted.replace('.', '/') + '/HelloWorldScala.scala'
        def srcFile = createFile(path, baseDir)
        srcFile << """
            package ${packageDotted}

            object HelloWorldScala {
              def main(args: Array[String]): Unit = {
                println("Hello, world!")
              }
            }
        """.stripIndent()
        return srcFile
    }


    @CompileStatic
    protected File writeKotlinHelloWorld(String packageDotted, File baseDir = getProjectDir()) {
        def path = 'src/main/kotlin/' + packageDotted.replace('.', '/') + '/HelloWorldKotlin.kt'
        def srcFile = createFile(path, baseDir)
        srcFile << """
            package ${packageDotted}

            fun main(args : Array<String>) {
                println("Hello, world!")
            }
        """.stripIndent()
        return srcFile
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
