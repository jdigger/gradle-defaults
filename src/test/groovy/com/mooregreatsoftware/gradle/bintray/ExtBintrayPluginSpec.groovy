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
package com.mooregreatsoftware.gradle.bintray

import com.mooregreatsoftware.gradle.defaults.DefaultsExtensionKt
import nebula.test.PluginProjectSpec
import org.gradle.api.GradleException

import static com.mooregreatsoftware.gradle.Projects.evaluate

@SuppressWarnings("GroovyPointlessBoolean")
class ExtBintrayPluginSpec extends PluginProjectSpec {

    @Override
    String getPluginName() {
        return "com.mooregreatsoftware.bintray"
    }


    def "setting the bintray extension"() {
        project.plugins.apply(pluginName)
        project.group = "test.test"
        def bintrayExtension = ExtBintrayPlugin.bintrayExtension(project)

        expect:
        bintrayExtension.publish == false
        bintrayExtension.pkg.publicDownloadNumbers == false
        bintrayExtension.pkg.repo == null
        bintrayExtension.pkg.websiteUrl == null
        bintrayExtension.pkg.issueTrackerUrl == null
        bintrayExtension.pkg.vcsUrl == null

        when:
        bintrayExtension.pkg.repo = "javaFiles"
        bintrayExtension.pkg.websiteUrl = "test://web"
        bintrayExtension.pkg.issueTrackerUrl = "test://issues"
        bintrayExtension.pkg.vcsUrl = "test://vcs"

        evaluate(project)

        then:
        bintrayExtension.publish == true
        bintrayExtension.pkg.publicDownloadNumbers == true
        bintrayExtension.pkg.repo == "javaFiles"
        bintrayExtension.pkg.websiteUrl == "test://web"
        bintrayExtension.pkg.issueTrackerUrl == "test://issues"
        bintrayExtension.pkg.vcsUrl == "test://vcs"
        bintrayExtension.publications as Set == ["main"] as Set
    }


    def "setting on the defaults extension"() {
        project.plugins.apply(pluginName)
        def bintrayExtension = ExtBintrayPlugin.bintrayExtension(project)
        project.group = "test.test"
        def defaultsExt = DefaultsExtensionKt.defaultsExtension(project)

        expect:
        bintrayExtension.publish == false
        bintrayExtension.pkg.publicDownloadNumbers == false
        bintrayExtension.pkg.repo == null
        bintrayExtension.pkg.websiteUrl == null
        bintrayExtension.pkg.issueTrackerUrl == null
        bintrayExtension.pkg.vcsUrl == null

        when:
        defaultsExt.bintrayRepo = "javaFiles"
        defaultsExt.orgId = 'tester'

        evaluate(project)

        then:
        bintrayExtension.publish == true
        bintrayExtension.pkg.publicDownloadNumbers == true
        bintrayExtension.pkg.repo == "javaFiles"
        bintrayExtension.pkg.websiteUrl == "https://github.com/tester/setting-on-the-defaults-extension"
        bintrayExtension.pkg.issueTrackerUrl == "https://github.com/tester/setting-on-the-defaults-extension/issues"
        bintrayExtension.pkg.vcsUrl == "https://github.com/tester/setting-on-the-defaults-extension.git"
    }


    def "exception when no repo set"() {
        project.plugins.apply(pluginName)

        when:
        evaluate(project)

        then:
        thrown(GradleException)
    }

}
