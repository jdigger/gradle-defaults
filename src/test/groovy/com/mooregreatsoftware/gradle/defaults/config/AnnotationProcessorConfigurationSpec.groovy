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

import com.mooregreatsoftware.gradle.defaults.DefaultsExtensionKt
import groovy.transform.CompileStatic
import nebula.test.ProjectSpec
import nebula.test.dependencies.DependencyGraph
import nebula.test.dependencies.GradleDependencyGenerator
import org.gradle.api.Action
import org.gradle.api.DomainObjectCollection
import org.gradle.api.internal.project.AbstractProject
import org.gradle.api.internal.project.DefaultProject
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.plugins.ide.idea.IdeaPlugin

import java.util.concurrent.TimeUnit

@SuppressWarnings("GroovyAssignabilityCheck")
abstract class AnnotationProcessorConfigurationSpec extends ProjectSpec {
    String mavenRepoPath
    String version = LombokExtension.DEFAULT_LOMBOK_VERSION


    def setup() {
        def generator = new GradleDependencyGenerator(
            new DependencyGraph(dependencies())
        )
        mavenRepoPath = generator.generateTestMavenRepo().absolutePath
        project.repositories.maven({ it.url = mavenRepoPath })

        project.setGroup("com.mooregreatsoftware.gradle.defaults")
    }


    abstract String getVersion()


    abstract List<String> dependencies()


    def "javac and IDEA project are setup correctly"() {
        DefaultsExtensionKt.defaultsExtension(project)
        createConf()

        project.plugins.apply(JavaPlugin)
        project.plugins.apply(IdeaPlugin)
        JavaConfig.of(project)
        def intellijFuture = IntellijConfig.of(project)

        when:
        evaluateProject()

        then:
        verifyCompileOnlyDependencies()

        when:
        configureTasks()

        then:
        project.tasks.withType(JavaCompile, { task ->
            verifyCompilerArgs(task)
        })

        when:
        def rootNode = new Node(null, "project")
        def intellijConfig = intellijFuture.get(1, TimeUnit.SECONDS)
        intellijConfig.setupCompiler(rootNode)
        def annotationProcessing = rootNode.component.find {
            it.@name == "CompilerConfiguration"
        }.annotationProcessing[0]
        def profile = annotationProcessing.profile[0]

//        XmlUtil.serialize(profile, System.err)

        then:
        verifyIdeaProcessorXml(profile)
    }


    protected DomainObjectCollection<JavaCompile> configureTasks() {
        project.tasks.withType(JavaCompile, { task ->
            task.actions.stream().
                map({ unwrap(it) }).
                filter({ it instanceof JavaConfig.ConfigCompilerAction }).
                forEach({ it.execute(task) })
        })
    }


    @CompileStatic
    static Action unwrap(Action action) {
        if (action.class.simpleName == "TaskActionWrapper") {
            def actionField = action.class.getDeclaredField("action")
            actionField.accessible = true
            return actionField.get(action) as Action
        }
        return action
    }


    void verifyIdeaProcessorXml(Node profile) {
        verifyProcessorClass(profile)
        verifyProcessorOptions(profile)
        verifyProcessorClasspath(profile)
    }


    void verifyProcessorClass(Node profile) {
        assert profile.processor.size() == 1
        assert profile.processor[0]["@name"] == processorClassname()
    }


    void verifyProcessorOptions(Node profile) {
    }


    void verifyProcessorClasspath(Node profile) {
        assert profile.processorPath.size() == 1
        assert profile.processorPath.entry[0] != null
        assert profile.processorPath.entry[0]["@name"] == processorJarLocation()
    }


    void verifyCompilerArgs(JavaCompile task) {
        def args = task.options.compilerArgs
        println "Compiler args: ${args.join(" ")}"
        verifyProcessorClassname(args)
        verifyProcessorJarLocation(args)
        verifyBootClasspath(args)
        verifyOtherCompilerArgs(args)
    }


    void verifyProcessorClassname(List<String> args) {
        assert args[1] == processorClassname()
    }


    void verifyProcessorJarLocation(List<String> args) {
        assert args[3] == processorJarLocation()
    }


    void verifyBootClasspath(List<String> args) {
        // none
    }


    void verifyOtherCompilerArgs(List<String> args) {
        // none
    }


    void verifyCompileOnlyDependencies() {
        assert project.configurations.getByName("compileOnly").files.size() == 1
    }


    abstract String processorClassname()


    abstract String processorJarLocation()


    abstract AnnotationProcessorConfiguration createConf()


    AbstractProject evaluateProject() {
        return ((DefaultProject)project).evaluate()
    }

}
