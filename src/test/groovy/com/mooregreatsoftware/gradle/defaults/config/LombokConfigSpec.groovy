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

import java.util.concurrent.TimeUnit


@SuppressWarnings("GroovyAssignabilityCheck")
class LombokConfigSpec extends AnnotationProcessorConfigurationSpec {

    @Override
    @CompileStatic
    String getVersion() {
        LombokExtension.DEFAULT_LOMBOK_VERSION
    }


    @Override
    @CompileStatic
    List<String> dependencies() {
        ["org.projectlombok:lombok:${version}"]
    }


    @Override
    @CompileStatic
    public LombokConfiguration createConf() {
        LombokConfigurationKt.lombokExtension(project).enabled = true
        LombokConfiguration.of(project).get(1, TimeUnit.SECONDS)
    }


    @Override
    @CompileStatic
    String processorJarLocation() {
        "${mavenRepoPath}/org/projectlombok/lombok/${version}/lombok-${version}.jar"
    }


    @Override
    @CompileStatic
    String processorClassname() {
        LombokConfiguration.LOMBOK_LAUNCH_ANNOTATION_PROCESSOR
    }

}
