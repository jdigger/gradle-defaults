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
package com.mooregreatsoftware.gradle.checkerframework

import com.mooregreatsoftware.gradle.JavacUtils
import com.mooregreatsoftware.gradle.java.ExtJavaPlugin
import com.mooregreatsoftware.gradle.lang.AbstractAnnotationProcessorPluginSpec
import com.mooregreatsoftware.gradle.lombok.LombokPlugin
import groovy.transform.CompileStatic

import static com.mooregreatsoftware.gradle.checkerframework.CheckerFrameworkPlugin.CHECKERFRAMEWORK_NULLNESS_CHECKER
import static com.mooregreatsoftware.gradle.lombok.LombokExtension.DEFAULT_LOMBOK_VERSION
import static com.mooregreatsoftware.gradle.lombok.LombokPlugin.LOMBOK_LAUNCH_ANNOTATION_PROCESSOR

class CheckerFrameworkPluginSpec extends AbstractAnnotationProcessorPluginSpec {

    @Override
    String getPluginName() {
        return CheckerFrameworkPlugin.PLUGIN_ID
    }


    @Override
    @CompileStatic
    String getVersion() {
        CheckerFrameworkExtension.DEFAULT_CHECKER_VERSION
    }


    @Override
    @CompileStatic
    List<String> dependencies() {
        [
            "org.checkerframework:checker:${version}".toString(),
            "org.checkerframework:checker-qual:${version}".toString(),
            "org.checkerframework:jdk8:${version}".toString(),
            "org.checkerframework:compiler:${version}".toString(),
        ]
    }


    @Override
    @CompileStatic
    CheckerFrameworkPlugin createPlugin() {
        project.plugins.apply(CheckerFrameworkPlugin.PLUGIN_ID) as CheckerFrameworkPlugin
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


    static class CheckerFrameworkPluginWithLombokSpec extends CheckerFrameworkPluginSpec {

        @Override
        @CompileStatic
        CheckerFrameworkPlugin createPlugin() {
            project.plugins.apply(ExtJavaPlugin.PLUGIN_ID)
            project.plugins.apply(LombokPlugin.PLUGIN_ID)
            return project.plugins.apply(CheckerFrameworkPlugin.PLUGIN_ID) as CheckerFrameworkPlugin
        }


        @Override
        @CompileStatic
        List<String> dependencies() {
            def deps = super.dependencies()
            deps.add("org.projectlombok:lombok:${DEFAULT_LOMBOK_VERSION}".toString())
            return deps
        }


        @Override
        void verifyCompileOnlyDependencies() {
            assert project.configurations.getByName("compileOnly").files.size() == 2
        }


        @Override
        @CompileStatic
        String processorJarLocation() {
            return CheckerFrameworkPlugin.processorLibFile(project).absolutePath +
                JavacUtils.PATH_SEPARATOR +
                LombokPlugin.processorLibraryFile(project).absolutePath
        }


        void verifyProcessorClass(Node profile) {
            assert profile.processor.size() == 2
            assert profile.processor[0]["@name"] == LOMBOK_LAUNCH_ANNOTATION_PROCESSOR
            assert profile.processor[1]["@name"] == CHECKERFRAMEWORK_NULLNESS_CHECKER
        }


        void verifyProcessorOptions(Node profile) {
            assert profile.option.size() == 2
            profile.option.collect { it["@name"] } as Set == ["lint", "warns"] as Set
        }


        void verifyProcessorClasspath(Node profile) {
            assert profile.processorPath.entry.size() == 2
            assert profile.processorPath.entry[0]["@name"] == CheckerFrameworkPlugin.processorLibFile(project).absolutePath
            assert profile.processorPath.entry[1]["@name"] == LombokPlugin.processorLibraryFile(project).absolutePath
        }


        @Override
        @CompileStatic
        String processorClassname() {
            LOMBOK_LAUNCH_ANNOTATION_PROCESSOR + "," + CHECKERFRAMEWORK_NULLNESS_CHECKER
        }

    }

}
