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
package com.mooregreatsoftware.gradle.maven;

import com.mooregreatsoftware.gradle.defaults.DefaultsExtension;
import com.mooregreatsoftware.gradle.util.xml.NodeBuilder;
import com.mooregreatsoftware.gradle.util.ProjectUtilsKt;
import groovy.util.Node;
import javaslang.collection.HashSet;
import javaslang.collection.List;
import lombok.val;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.gradle.api.Project;
import org.gradle.api.XmlProvider;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.internal.publication.MavenPomInternal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.mooregreatsoftware.gradle.defaults.DefaultsExtensionKt.defaultsExtension;
import static com.mooregreatsoftware.gradle.util.xml.XmlUtilsKt.appendChild;
import static com.mooregreatsoftware.gradle.util.xml.XmlUtilsKt.appendChildren;
import static com.mooregreatsoftware.gradle.util.xml.XmlUtilsKt.n;
import static org.apache.commons.lang.StringUtils.isNotEmpty;

public class MavenPublishPublications {
    private static final Logger LOG = LoggerFactory.getLogger(MavenPublishPublications.class);
    public static final String PUBLICATION_NAME = "main";

    private static final String ORG_GRADLE_MAVEN_PUBLISH = "org.gradle.maven-publish";


    /**
     * Gets (creating, if needed) the "main" publication.
     */
    public static MavenPublication mainPublication(Project project) {
        val pub = findMainPublication(project);
        return (pub == null) ? createMainPublication(project) : pub;
    }

    // **********************************************************************
    //
    // PRIVATE METHODS
    //
    // **********************************************************************


    private static @Nullable MavenPublication findMainPublication(Project project) {
        ensureMavenPublishPlugin(project);

        val publishing = project.getConvention().getByType(PublishingExtension.class);
        val publications = publishing.getPublications();

        return (MavenPublication)publications.findByName(PUBLICATION_NAME);
    }


    /**
     * Makes sure the standard Maven-Publish plugin has been applied.
     */
    private static void ensureMavenPublishPlugin(Project project) {
        project.getPlugins().apply(ORG_GRADLE_MAVEN_PUBLISH);
    }


    private static MavenPublication createMainPublication(Project project) {
        val publishing = project.getConvention().getByType(PublishingExtension.class);
        val pub = publishing.getPublications().create(PUBLICATION_NAME, MavenPublication.class);

        publishing.getRepositories().mavenLocal();

        project.afterEvaluate(prj -> groupid(project, (MavenPomInternal)pub.getPom()));

        configPom(project, pub);

        return pub;
    }


    private static void configPom(Project project, MavenPublication pub) {
        pub.pom(pom -> pom.withXml(xmlProvider -> configPom(project, xmlProvider)));
    }


    private static void configPom(Project project, XmlProvider xmlProvider) {
        val extension = defaultsExtension(project);

        val node = xmlProvider.asNode();
        name(node, project);
        description(node, project);
        siteUrl(node, extension);
        organization(node, extension);
        licenses(node, extension);
        developers(node, extension);
        contributors(node, extension);
        scm(node, extension);
        dependencies(node, project);
    }


    private static void groupid(Project project, MavenPomInternal pom) {
        val projectIdentity = pom.getProjectIdentity();
        if (!isNotEmpty(projectIdentity.getGroupId())) {
            if (!isNotEmpty(project.getGroup().toString())) {
                val topPackageName = ProjectUtilsKt.detectTopPackageName(project.getConvention());
                if (topPackageName == null) {
                    throw new IllegalStateException("There is no group set on the project, and could not auto-detect one");
                }
                LOG.warn("Auto-detecting the group id as \"" + topPackageName +
                    "\", but should be explicitly set on the project.\nIn build.gradle, add:\n   group = \"" +
                    topPackageName + "\"");
                project.setGroup(topPackageName);
            }
            projectIdentity.setGroupId(project.getGroup().toString());
        }
    }


    private static void organization(Node node, DefaultsExtension extension) {
        val orgname = extension.getOrgName();
        if (orgname != null) {
            List<NodeBuilder> children = List.of(n("name", orgname));
            val orgUrl = extension.getOrgUrl();
            if (orgUrl != null)
                children = children.append(n("url", orgUrl));
            appendChildren(node, "organization", children);
        }
    }


    private static void siteUrl(Node node, DefaultsExtension extension) {
        node.appendNode("url", extension.getSiteUrl());
    }


    private static void description(Node node, Project project) {
        if (isNotEmpty(project.getDescription())) {
            node.appendNode("description", project.getDescription());
        }
    }


    private static void name(Node node, Project project) {
        node.appendNode("name", project.getName());
    }


    private static void scm(Node node, DefaultsExtension extension) {
        appendChildren(node, "scm", List.of(
            n("connection", "scm:git:" + extension.getVcsReadUrl()),
            n("developerConnection", "scm:git:" + extension.getVcsWriteUrl()),
            n("url", extension.getSiteUrl())));
    }


    private static void dependencies(Node node, Project project) {
        final Configuration runtime = project.getConfigurations().findByName("runtime");
        if (runtime != null) {
            final DependencySet allDependencies = runtime.getAllDependencies();
            if (!allDependencies.isEmpty()) {
                final Node dependenciesNode = node.appendNode("dependencies");
                for (Dependency dependency : allDependencies) {
                    if (dependency.getGroup() != null) {
                        final Node dependencyNode = dependenciesNode.appendNode("dependency");
                        dependencyNode.appendNode("groupId", dependency.getGroup());
                        dependencyNode.appendNode("artifactId", dependency.getName());
                        dependencyNode.appendNode("version", dependency.getVersion());
                    }
                    else {
                        LOG.warn("There is a \"null\" dependency (likely pointing directly to a file): ignoring it for the POM generation for {}", project);
                    }
                }
            }
            else {
                LOG.info("There are no dependencies in the \"runtime\" configuration for {}", project);
            }
        }
        else {
            LOG.info("There is no \"runtime\" configuration, so not adding any dependencies to the generated POM for {}", project);
        }
    }


    private static void developers(Node node, DefaultsExtension extension) {
        val developers = extension.getDevelopers();
        if (developers != null && !developers.isEmpty()) {
            appendChildren(node, "developers",
                HashSet.ofAll(developers).map(developer ->
                    n("developer", List.of(
                        n("id", developer.getId()),
                        n("name", developer.getName()),
                        n("email", developer.getEmail()))
                    )
                )
            );
        }
    }


    private static void contributors(Node node, DefaultsExtension extension) {
        val contributors = extension.getContributors();
        if (contributors != null && !contributors.isEmpty()) {
            appendChildren(node, "contributors",
                HashSet.ofAll(contributors).map(m ->
                    n("contributor", List.of(
                        // TODO ensure that the @NonNull is true...
                        n("name", (@NonNull String)m.get("name")),
                        n("email", (@NonNull String)m.get("email")))
                    )
                )
            );
        }
    }


    private static void licenses(Node node, DefaultsExtension extension) {
        appendChild(node, "licenses",
            n("license", List.of(
                n("name", extension.getLicenseName()),
                n("url", extension.getLicenseUrl()))
            )
        );
    }

}
