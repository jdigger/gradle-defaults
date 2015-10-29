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

import com.google.common.base.Stopwatch
import org.ajoberstar.grgit.Grgit
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.StandardOutputListener
import org.gradle.api.tasks.testing.TestOutputListener
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

import java.util.concurrent.TimeUnit

class DefaultsPluginSpec extends Specification {

    def "Apply"() {
        Stopwatch stopwatch = Stopwatch.createStarted()

        def projDir = new File('./src/test/resources/projects/proj1').canonicalFile

        expect:
        projDir.exists()

        when:
        println "Started test: ${stopwatch.elapsed(TimeUnit.MILLISECONDS)}"
        stopwatch.reset().start()

        Project project = ProjectBuilder.builder().withProjectDir(projDir).build()

        project.gradle.useLogger(({ println "logging ${it}" } as StandardOutputListener) as TestOutputListener)

        println "Built project: ${stopwatch.elapsed(TimeUnit.MILLISECONDS)}"
        stopwatch.reset().start()

        Grgit git = Grgit.init(dir: project.projectDir)

        println "Init git: ${stopwatch.elapsed(TimeUnit.MILLISECONDS)}"
        stopwatch.reset().start()

        git.repository.jgit.commit().with {
            message = 'initial'
            it
        }.call()

        println "first commit: ${stopwatch.elapsed(TimeUnit.MILLISECONDS)}"
        stopwatch.reset().start()

        project.apply plugin: DefaultsPlugin

        project.repositories.jcenter()

        println "Applied plugin: ${stopwatch.elapsed(TimeUnit.MILLISECONDS)}"
        stopwatch.reset().start()

        project.defaults {
            vcsWriteUrl = 'git@github.com:jdigger/gradle-defaults.git'
        }

        println "Setup defaults: ${stopwatch.elapsed(TimeUnit.MILLISECONDS)}"
        stopwatch.reset().start()

        def task = project.tasks.getByPath(":tasks")
        executeTask(task)

        println "Called tasks: ${stopwatch.elapsed(TimeUnit.MILLISECONDS)}"
        stopwatch.reset().start()

        then:
        true

        cleanup:
        println "Cleaning up: ${stopwatch.elapsed(TimeUnit.MILLISECONDS)}"
        stopwatch.reset().start()

        git?.close()
        assert new File(projDir, '.git').deleteDir()
        assert new File(projDir, 'userHome').deleteDir()

        println "Finished: ${stopwatch.elapsed(TimeUnit.MILLISECONDS)}"
        stopwatch.stop()
    }


    private static void executeTask(Task task) {
        task.dependsOn.each { dep ->
            if (dep instanceof Task) {
                executeTask((Task)dep)
            }
            else {
                task.project.logger.info("Got dependency ${dep}")
            }
        }
        executeTaskActions(task)
    }


    private static void executeTaskActions(Task task) {
        println "Executing ${task.name}"
        task.project.logger.info "Executing ${task.name}"
        task.actions.each { it.execute(task) }
    }

}
