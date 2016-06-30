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

import org.gradle.api.Project;

import java.util.Map;
import java.util.Set;

import static com.mooregreatsoftware.gradle.defaults.Utils.opt;

@SuppressWarnings({"unused", "WeakerAccess"})
public class DefaultsExtension {
    private String id;
    private String orgName;
    private String orgUrl;
    private String bintrayRepo;
    private String bintrayPkg;
    private Set<String> bintrayLabels;
    private boolean bintrayToCentral = false;
    private Set<Map> developers;
    private Set<Map> contributors;
    private String compatibilityVersion = "1.7";
    private String siteUrl;
    private String issuesUrl;
    private String vcsReadUrl;
    private String vcsWriteUrl;
    private String licenseKey;
    private String licenseName;
    private String licenseUrl;
    private String copyrightYears;


    public DefaultsExtension(final Project project) {
        // TODO change to default values and/or in getters
        project.afterEvaluate(prj -> {
            bintrayPkg = opt(bintrayPkg).orElse(project.getName());
            siteUrl = opt(siteUrl).orElseGet(() -> "https://github.com/" + id + "/" + project.getName());
            issuesUrl = opt(issuesUrl).orElseGet(() -> siteUrl + "/issues");
            vcsReadUrl = opt(vcsReadUrl).orElseGet(() -> siteUrl + ".git");
            vcsWriteUrl = opt(vcsWriteUrl).orElseGet(() -> "git@github.com:" + id + "/" + project.getName() + ".git");
            licenseKey = opt(licenseKey).orElse("Apache-2.0");
            licenseName = opt(licenseName).orElse("The Apache Software License, Version 2.0");
            licenseUrl = opt(licenseUrl).orElse("http://www.apache.org/licenses/LICENSE-2.0");
        });
    }


    public String getId() {
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
        return bintrayPkg;
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
        return siteUrl;
    }


    public void setSiteUrl(String siteUrl) {
        this.siteUrl = siteUrl;
    }


    public String getIssuesUrl() {
        return issuesUrl;
    }


    public void setIssuesUrl(String issuesUrl) {
        this.issuesUrl = issuesUrl;
    }


    public String getVcsReadUrl() {
        return vcsReadUrl;
    }


    public void setVcsReadUrl(String vcsReadUrl) {
        this.vcsReadUrl = vcsReadUrl;
    }


    public String getVcsWriteUrl() {
        return vcsWriteUrl;
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


}
