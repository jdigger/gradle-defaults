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

import org.gradle.api.GradleException;
import org.gradle.api.Project;

import java.util.Map;
import java.util.Set;

import static com.mooregreatsoftware.gradle.defaults.Utils.opt;

@SuppressWarnings({"unused", "WeakerAccess"})
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
    private String lobokVersion = "1.16.8";


    public DefaultsExtension(final Project project) {
        this.project = project;
    }


    public String getId() {
        if (this.id == null) {
            throw new IllegalStateException("\"id\" is not set for " + DefaultsExtension.class.getName());
        }
        return id;
    }


    public void setId(String id) {
        this.id = id;
    }


    public String getOrgName() {
        return orgName;
    }


    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }


    public String getOrgUrl() {
        return orgUrl;
    }


    public void setOrgUrl(String orgUrl) {
        this.orgUrl = orgUrl;
    }


    public String getBintrayRepo() {
        return bintrayRepo;
    }


    public void setBintrayRepo(String bintrayRepo) {
        this.bintrayRepo = bintrayRepo;
    }


    public String getBintrayPkg() {
        return opt(bintrayPkg).orElse(project.getName());
    }


    public void setBintrayPkg(String bintrayPkg) {
        this.bintrayPkg = bintrayPkg;
    }


    public Set<String> getBintrayLabels() {
        return bintrayLabels;
    }


    public void setBintrayLabels(Set<String> bintrayLabels) {
        this.bintrayLabels = bintrayLabels;
    }


    public boolean getBintrayToCentral() {
        return bintrayToCentral;
    }


    public boolean isBintrayToCentral() {
        return bintrayToCentral;
    }


    public void setBintrayToCentral(boolean bintrayToCentral) {
        this.bintrayToCentral = bintrayToCentral;
    }


    public Set<Map> getDevelopers() {
        return developers;
    }


    public void setDevelopers(Set<Map> developers) {
        this.developers = developers;
    }


    public Set<Map> getContributors() {
        return contributors;
    }


    public void setContributors(Set<Map> contributors) {
        this.contributors = contributors;
    }


    public String getCompatibilityVersion() {
        return compatibilityVersion;
    }


    public void setCompatibilityVersion(String compatibilityVersion) {
        this.compatibilityVersion = compatibilityVersion;
    }


    public String getSiteUrl() {
        return opt(siteUrl).orElseGet(() -> "https://github.com/" + getId() + "/" + project.getName());
    }


    public void setSiteUrl(String siteUrl) {
        this.siteUrl = siteUrl;
    }


    public String getIssuesUrl() {
        return opt(issuesUrl).orElseGet(() -> getSiteUrl() + "/issues");
    }


    public void setIssuesUrl(String issuesUrl) {
        this.issuesUrl = issuesUrl;
    }


    public String getVcsReadUrl() {
        return opt(vcsReadUrl).orElseGet(() -> getSiteUrl() + ".git");
    }


    public void setVcsReadUrl(String vcsReadUrl) {
        this.vcsReadUrl = vcsReadUrl;
    }


    public String getVcsWriteUrl() {
        return opt(vcsWriteUrl).orElseGet(() -> "git@github.com:" + getId() + "/" + project.getName() + ".git");
    }


    public void setVcsWriteUrl(String vcsWriteUrl) {
        this.vcsWriteUrl = vcsWriteUrl;
    }


    public String getLicenseKey() {
        return licenseKey;
    }


    public void setLicenseKey(String licenseKey) {
        this.licenseKey = licenseKey;
    }


    public String getLicenseName() {
        return licenseName;
    }


    public void setLicenseName(String licenseName) {
        this.licenseName = licenseName;
    }


    public String getLicenseUrl() {
        return licenseUrl;
    }


    public void setLicenseUrl(String licenseUrl) {
        this.licenseUrl = licenseUrl;
    }


    public String getCopyrightYears() {
        return copyrightYears;
    }


    public void setCopyrightYears(String copyrightYears) {
        this.copyrightYears = copyrightYears;
    }


    public String getLobokVersion() {
        return lobokVersion;
    }


    public void setLobokVersion(String lobokVersion) {
        this.lobokVersion = lobokVersion;
    }

}
