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
package com.mooregreatsoftware.gradle.defaults;

import com.mooregreatsoftware.gradle.defaults.config.CheckerFrameworkConfiguration;
import com.mooregreatsoftware.gradle.defaults.config.LombokConfiguration;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.gradle.api.Project;

import java.util.Map;
import java.util.Set;

import static com.mooregreatsoftware.gradle.defaults.Utils.opt;
import static lombok.AccessLevel.NONE;

@Data
@Accessors(fluent = false)
@SuppressWarnings("DefaultAnnotationParam")
public class DefaultsExtension {
    private final Project project;

    private String id;
    private String orgName;
    private String orgUrl;
    private String bintrayRepo;
    private String bintrayPkg;
    private Set<String> bintrayLabels;
    private boolean bintrayToCentral = false;
    private Set<Map> developers;
    private Set<Map> contributors;
    private String compatibilityVersion = "1.8";
    private String siteUrl;
    private String issuesUrl;
    private String vcsReadUrl;
    private String vcsWriteUrl;
    private String licenseKey = "Apache-2.0";
    private String licenseName = "The Apache Software License, Version 2.0";
    private String licenseUrl = "http://www.apache.org/licenses/LICENSE-2.0";
    private String copyrightYears;
    private String lombokVersion = LombokConfiguration.DEFAULT_LOMBOK_VERSION;
    private String checkerFrameworkVersion = CheckerFrameworkConfiguration.DEFAULT_CHECKER_VERSION;

    @Setter(NONE)
    @Getter(NONE)
    private int _useLombok = -1;

    @Setter(NONE)
    @Getter(NONE)
    private int _useCheckerFramework = -1;


    public DefaultsExtension(final Project project) {
        this.project = project;
    }


    public String getId() {
        if (this.id == null) {
            throw new IllegalStateException("\"id\" is not set for " + DefaultsExtension.class.getName() + " on " + this.project.getName());
        }
        return id;
    }


    public String getSiteUrl() {
        return opt(siteUrl).orElseGet(() -> "https://github.com/" + getId() + "/" + project.getName());
    }


    public String getIssuesUrl() {
        return opt(issuesUrl).orElseGet(() -> getSiteUrl() + "/issues");
    }


    public String getVcsReadUrl() {
        return opt(vcsReadUrl).orElseGet(() -> getSiteUrl() + ".git");
    }


    public String getVcsWriteUrl() {
        return opt(vcsWriteUrl).orElseGet(() -> "git@github.com:" + getId() + "/" + project.getName() + ".git");
    }


    public boolean getUseLombok() {
        return (_useLombok > -1) ?
            (_useLombok == 1) :
            ProjectUtils.hasJavaSource(project);
    }

}
