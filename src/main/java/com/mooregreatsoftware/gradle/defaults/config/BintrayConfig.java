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

import com.jfrog.bintray.gradle.BintrayExtension;
import com.mooregreatsoftware.gradle.defaults.DefaultsExtension;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileTree;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Optional.ofNullable;

@SuppressWarnings("WeakerAccess")
public class BintrayConfig extends AbstractConfigWithExtension {
    public BintrayConfig(Project project, DefaultsExtension extension) {
        super(project, extension);
    }


    public void config() {
        plugins().withId("com.jfrog.bintray", plugin -> {
            if (project.hasProperty("bintrayUser") && project.hasProperty("bintrayKey")) {
                project.afterEvaluate(prj -> {
                    Map<String, Object> bintrayAttributes = new HashMap<>();
                    if (ofNullable(extension.getBintrayLabels()).filter(lbls -> lbls.contains("gradle")).isPresent()) {
                        info("bintrayLabels does includes 'gradle' so generating 'gradle-plugins' attributes");
                        Map<String, String> fileTreeConf = new HashMap<>();
                        fileTreeConf.put("dir", "src/main/resources/META-INF/gradle-plugins");
                        fileTreeConf.put("include", "*.properties");
                        final ConfigurableFileTree files = project.fileTree(fileTreeConf);
                        List<String> pluginIds = StreamSupport.stream(files.spliterator(), false).
                            map(file -> file.getName().substring(0, (file.getName().lastIndexOf(".") - 1))).
                            collect(Collectors.toList());
                        List<String> pluginIdStrs = pluginIds.stream().map(pid ->
                            pid + ":" + String.valueOf(project.getGroup()) + ":" + name()
                        ).collect(Collectors.toList());
                        bintrayAttributes.put("gradle-plugin", pluginIdStrs);
                    }
                    else {
                        info("bintrayLabels does not include 'gradle' so not generating 'gradle-plugins' attributes");
                    }

                    info("bintrayAttributes: " + bintrayAttributes);

                    BintrayExtension bintray = project.getConvention().getByType(BintrayExtension.class);

                    bintray.setUser((String)project.property("bintrayUser"));
                    bintray.setKey((String)project.property("bintrayKey"));

                    bintray.setPublications("main");
                    bintray.setPublish(true);

                    BintrayExtension.PackageConfig pkgConfig = bintray.getPkg();

                    if (extension.getOrgName() != null) pkgConfig.setUserOrg(extension.getId());

                    pkgConfig.setRepo(extension.getBintrayRepo());
                    pkgConfig.setName(extension.getBintrayPkg());
                    pkgConfig.setDesc(project.getDescription());
                    pkgConfig.setWebsiteUrl(extension.getSiteUrl());
                    pkgConfig.setIssueTrackerUrl(extension.getIssuesUrl());
                    pkgConfig.setVcsUrl(extension.getVcsReadUrl());
                    pkgConfig.setLicenses(extension.getLicenseKey());
                    final String[] bintrayLabels = ofNullable(extension.getBintrayLabels()).
                        map(set -> set.toArray(new String[set.size()])).
                        orElse(new String[0]);
                    pkgConfig.setLabels(bintrayLabels);
                    pkgConfig.setPublicDownloadNumbers(true);

                    BintrayExtension.VersionConfig version = pkgConfig.getVersion();

                    version.setVcsTag("v" + String.valueOf(project.getVersion()));
                    version.setAttributes(bintrayAttributes);

                    BintrayExtension.GpgConfig gpg = version.getGpg();
                    gpg.setSign(true);
                    if (project.hasProperty("gpgPassphrase")) {
                        gpg.setPassphrase((String)project.property("gpgPassphrase"));
                    }
                });
            }
        });
    }

}
