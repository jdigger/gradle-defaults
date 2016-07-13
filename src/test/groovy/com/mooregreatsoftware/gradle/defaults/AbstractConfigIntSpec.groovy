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
import groovy.transform.CompileStatic
import nebula.test.IntegrationSpec
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.URIish
import org.gradle.api.logging.LogLevel

abstract class AbstractConfigIntSpec extends IntegrationSpec {
    @TempDirectory(clean = false, baseDir = 'build/git-bare')
    protected File remoteOrigin

    Git git


    @CompileStatic
    def setup() {
        logLevel = LogLevel.DEBUG

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
        return javaFile
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
