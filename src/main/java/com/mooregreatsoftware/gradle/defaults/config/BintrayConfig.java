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
package com.mooregreatsoftware.gradle.defaults.config;

import com.google.common.io.Files;
import com.jfrog.bintray.gradle.BintrayExtension;
import com.jfrog.bintray.gradle.BintrayPlugin;
import com.mooregreatsoftware.gradle.defaults.DefaultsExtension;
import lombok.val;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.gradle.api.Project;

import java.io.File;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Optional.ofNullable;

@SuppressWarnings({"WeakerAccess", "RedundantCast", "RedundantTypeArguments"})
public class BintrayConfig extends AbstractConfigWithExtension {
    public BintrayConfig(Project project, DefaultsExtension extension) {
        super(project, extension);
    }


    public void config() {
        plugins().withType(BintrayPlugin.class, plugin -> {
            if (project.hasProperty("bintrayUser") && project.hasProperty("bintrayKey")) {
                project.afterEvaluate(prj -> configBintray());
            }
        });
    }


    private void configBintray() {
        val bintray = bintrayExtension(project);

        bintray.setUser((String)project.property("bintrayUser"));
        bintray.setKey((String)project.property("bintrayKey"));

        bintray.setPublications("main");
        bintray.setPublish(true);

        val pkgConfig = bintray.getPkg();

        if (extension.getOrgName() != null) pkgConfig.setUserOrg(extension.getId());

        ofNullable(extension.getBintrayRepo()).ifPresent(pkgConfig::setRepo);
        ofNullable(extension.getBintrayPkg()).ifPresent(pkgConfig::setName);
        ofNullable(project.getDescription()).ifPresent(pkgConfig::setDesc);
        ofNullable(extension.getSiteUrl()).ifPresent(pkgConfig::setWebsiteUrl);
        ofNullable(extension.getIssuesUrl()).ifPresent(pkgConfig::setIssueTrackerUrl);
        ofNullable(extension.getVcsReadUrl()).ifPresent(pkgConfig::setVcsUrl);
        ofNullable(extension.getLicenseKey()).ifPresent(pkgConfig::setLicenses);

        val bintrayLabels = toStringArray(extension.getBintrayLabels());
        pkgConfig.setLabels(bintrayLabels);
        pkgConfig.setPublicDownloadNumbers(true);

        val bintrayPkgVersion = pkgConfig.getVersion();

        bintrayPkgVersion.setVcsTag("v" + project.getVersion());
        bintrayPkgVersion.setAttributes(bintrayAttributes());

        val gpg = bintrayPkgVersion.getGpg();
        gpg.setSign(true);
        if (project.hasProperty("gpgPassphrase")) {
            gpg.setPassphrase((String)project.property("gpgPassphrase"));
        }
    }


    private static String[] toStringArray(@Nullable Set<String> labels) {
        return (@NonNull String @NonNull [])ofNullable(labels).
            map(set -> set.toArray(new String[set.size()])).
            orElse(new String[0]);
    }


    private HashMap<String, Object> bintrayAttributes() {
        val bintrayAttributes = new HashMap<String, Object>();
        if (ofNullable(extension.getBintrayLabels()).filter(lbl -> lbl.contains("gradle")).isPresent()) {
            info("bintrayLabels does includes 'gradle' so generating 'gradle-plugins' attribute");
            val filesTree = gradlePluginPropertyFiles(project);
            val pluginIdBintrayAttributeValues = filesToPluginIds(filesTree).
                map(this::pluginIdToBintrayAttributeValue).
                collect(Collectors.<@NonNull String>toList());
            bintrayAttributes.put("gradle-plugins", pluginIdBintrayAttributeValues);
        }
        else {
            info("bintrayLabels does not include 'gradle' so not generating 'gradle-plugins' attribute");
        }

        info("bintrayAttributes: " + bintrayAttributes);
        return bintrayAttributes;
    }


    private String pluginIdToBintrayAttributeValue(String pluginId) {
        return pluginId + ":" + project.getGroup() + ":" + name();
    }


    private static Stream<String> filesToPluginIds(Iterable<File> filesTree) {
        return StreamSupport.stream(filesTree.spliterator(), false).
            map(BintrayConfig::filenameNoExtension);
    }


    private static String filenameNoExtension(File file) {
        return Files.getNameWithoutExtension(file.getName());
    }


    private static Iterable<File> gradlePluginPropertyFiles(Project project) {
        val fileTreeConf = new HashMap<String, String>();
        fileTreeConf.put("dir", "src/main/resources/META-INF/gradle-plugins");
        fileTreeConf.put("include", "*.properties");
        return project.fileTree(fileTreeConf);
    }


    public static BintrayExtension bintrayExtension(Project project) {
        return project.getConvention().getByType(BintrayExtension.class);
    }

}
