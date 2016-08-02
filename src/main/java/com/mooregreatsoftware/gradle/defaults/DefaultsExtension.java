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
import lombok.val;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.gradle.api.Project;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.mooregreatsoftware.gradle.defaults.Utils.opt;
import static lombok.AccessLevel.NONE;

@Data
@Accessors(fluent = false)
@SuppressWarnings({"DefaultAnnotationParam", "WeakerAccess", "unused", "RedundantTypeArguments"})
public class DefaultsExtension {
    private final Project project;

    private @Nullable String id;
    private @Nullable String orgName;
    private @Nullable String orgUrl;
    private @Nullable String bintrayRepo;
    private @Nullable String bintrayPkg;
    private @Nullable Set<String> bintrayLabels;
    private boolean bintrayToCentral = false;

    @Setter(NONE)
    private @MonotonicNonNull Set<Developer> developers;

    private @Nullable Set<Map> contributors;
    private String compatibilityVersion = "1.8";
    private @Nullable String siteUrl;
    private @Nullable String issuesUrl;
    private @Nullable String vcsReadUrl;
    private @Nullable String vcsWriteUrl;
    private String licenseKey = "Apache-2.0";
    private String licenseName = "The Apache Software License, Version 2.0";
    private String licenseUrl = "http://www.apache.org/licenses/LICENSE-2.0";
    private @Nullable String copyrightYears;
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


    public void setUseLombok(boolean useLombok) {
        _useLombok = useLombok ? 1 : 0;
    }


    public boolean getUseCheckerFramework() {
        return (_useCheckerFramework > -1) ?
            (_useCheckerFramework == 1) :
            ProjectUtils.hasJavaSource(project);
    }


    public void setUseCheckerFramework(boolean useCheckerFramework) {
        _useCheckerFramework = useCheckerFramework ? 1 : 0;
    }


    public void setDevelopers(Set<Map<String, Object>> devs) {
        developers = devs.stream().map(DefaultsExtension::devFromMap).collect(Collectors.<@NonNull Developer>toSet());
    }


    @SuppressWarnings("unchecked")
    private static Developer devFromMap(Map<String, Object> map) {
        // TODO Check to see how much of this is necessary, or if can apply directly to the Maven configuration
        val dev = new Developer();
        val email = (@Nullable String)map.get("email");
        if (email == null) throw new IllegalArgumentException("The email address for a developer must be set");
        dev.setEmail(email);
        dev.setId((String)map.getOrDefault("id", email));
        dev.setName((String)map.getOrDefault("name", email));
        return dev;
    }


    @Getter
    @Setter
    public static class Developer {
        private @MonotonicNonNull String id;
        private @MonotonicNonNull String name;
        private @MonotonicNonNull String email;
    }

}
