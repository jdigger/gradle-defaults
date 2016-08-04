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

import static com.mooregreatsoftware.gradle.defaults.config.LombokConfiguration.DEFAULT_LOMBOK_VERSION
import static com.mooregreatsoftware.gradle.defaults.config.LombokConfiguration.LOMBOK_LAUNCH_ANNOTATION_PROCESSOR

@SuppressWarnings("GroovyAssignabilityCheck")
class LombokConfigSpec extends AnnotationProcessorConfigurationSpec {

    @Override
    String getVersion() {
        DEFAULT_LOMBOK_VERSION
    }


    @Override
    @CompileStatic
    List<String> dependencies() {
        ["org.projectlombok:lombok:${version}"]
    }


    @Override
    @CompileStatic
    public LombokConfiguration createConf() {
        return LombokConfiguration.create(project, { version }, JavaConfig.@Companion.of(project))
    }


    @Override
    @CompileStatic
    String processorJarLocation() {
        "${mavenRepoPath}/org/projectlombok/lombok/${version}/lombok-${version}.jar"
    }


    @Override
    String processorClassname() {
        LOMBOK_LAUNCH_ANNOTATION_PROCESSOR
    }

}
