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

import groovy.transform.CompileStatic

import static com.mooregreatsoftware.gradle.defaults.config.CheckerFrameworkConfiguration.CHECKERFRAMEWORK_NULLNESS_CHECKER
import static com.mooregreatsoftware.gradle.defaults.config.CheckerFrameworkConfiguration.DEFAULT_CHECKER_VERSION
import static com.mooregreatsoftware.gradle.defaults.config.JavaConfig.PATH_SEPARATOR
import static com.mooregreatsoftware.gradle.defaults.config.LombokConfiguration.LOMBOK_LAUNCH_ANNOTATION_PROCESSOR

@SuppressWarnings("GroovyAssignabilityCheck")
class CheckerFrameworkConfigurationSpec extends AnnotationProcessorConfigurationSpec {

    @Override
    String getVersion() {
        DEFAULT_CHECKER_VERSION
    }


    @Override
    @CompileStatic
    List<String> dependencies() {
        [
            "org.checkerframework:checker:${version}",
            "org.checkerframework:checker-qual:${version}",
            "org.checkerframework:jdk8:${version}",
            "org.checkerframework:compiler:${version}",
        ]
    }


    @Override
    @CompileStatic
    public AnnotationProcessorConfiguration createConf() {
        CheckerFrameworkConfiguration.create(project, { version }, JavaConfig.of(project))
    }


    @Override
    @CompileStatic
    String processorJarLocation() {
        "${mavenRepoPath}/org/checkerframework/checker/${version}/checker-${version}.jar"
    }


    @Override
    @CompileStatic
    String processorClassname() {
        CHECKERFRAMEWORK_NULLNESS_CHECKER
    }


    static class CheckerFrameworkConfigurationWithLombokSpec extends CheckerFrameworkConfigurationSpec {


        @Override
        @CompileStatic
        public AnnotationProcessorConfiguration createConf() {
            def javaConfig = JavaConfig.of(project)
            CheckerFrameworkConfiguration.create(project, { version }, javaConfig)
            LombokConfiguration.create(project, { LombokConfiguration.DEFAULT_LOMBOK_VERSION }, javaConfig)
        }


        @Override
        @CompileStatic
        List<String> dependencies() {
            def deps = super.dependencies()
            deps.add("org.projectlombok:lombok:${LombokConfiguration.DEFAULT_LOMBOK_VERSION}".toString())
            return deps
        }


        @Override
        void verifyCompileOnlyDependencies() {
            assert project.configurations.getByName("compileOnly").files.size() == 2
        }


        @Override
        @CompileStatic
        String processorJarLocation() {
            return project.extensions.getByType(CheckerFrameworkConfiguration).processorLibraryFile().absolutePath +
                PATH_SEPARATOR +
                project.extensions.getByType(LombokConfiguration).processorLibraryFile().absolutePath
        }


        void verifyProcessorClass(Node profile) {
            assert profile.processor.size() == 2
            assert profile.processor[0]["@name"] == LOMBOK_LAUNCH_ANNOTATION_PROCESSOR
            assert profile.processor[1]["@name"] == CHECKERFRAMEWORK_NULLNESS_CHECKER
        }


        void verifyProcessorOptions(Node profile) {
            assert profile.option.size() == 2
            assert profile.option[0]["@name"] == "lint"
            assert profile.option[1]["@name"] == "warns"
        }


        void verifyProcessorClasspath(Node profile) {
            assert profile.processorPath.entry.size() == 2
            assert profile.processorPath.entry[0]["@name"] == project.extensions.getByType(CheckerFrameworkConfiguration).processorLibraryFile().absolutePath
            assert profile.processorPath.entry[1]["@name"] == project.extensions.getByType(LombokConfiguration).processorLibraryFile().absolutePath
        }


        @Override
        @CompileStatic
        String processorClassname() {
            LOMBOK_LAUNCH_ANNOTATION_PROCESSOR + "," + CHECKERFRAMEWORK_NULLNESS_CHECKER
        }

    }

}
