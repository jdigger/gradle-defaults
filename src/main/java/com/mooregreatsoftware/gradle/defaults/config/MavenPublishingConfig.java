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

import com.mooregreatsoftware.gradle.defaults.DefaultsExtension;
import com.mooregreatsoftware.gradle.defaults.xml.NodeBuilder;
import groovy.util.Node;
import lombok.val;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.gradle.api.Project;
import org.gradle.api.XmlProvider;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.internal.publication.MavenPomInternal;
import org.gradle.api.publish.maven.internal.publisher.MavenProjectIdentity;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;

import java.util.stream.Collectors;

import static com.mooregreatsoftware.gradle.defaults.Utils.isNotEmpty;
import static com.mooregreatsoftware.gradle.defaults.xml.XmlUtils.createNode;
import static com.mooregreatsoftware.gradle.defaults.xml.XmlUtils.n;
import static java.util.Arrays.asList;

@SuppressWarnings({"WeakerAccess", "RedundantTypeArguments"})
public class MavenPublishingConfig extends AbstractConfigWithExtension {
    public static final String PUBLICATION_NAME = "main";


    public MavenPublishingConfig(Project project, DefaultsExtension extension) {
        super(project, extension);
    }


    public static MavenPublication mainPublication(Project project) {
        val mavenPublishPlugin = project.getPlugins().findPlugin(MavenPublishPlugin.class);
        if (mavenPublishPlugin == null)
            project.getPlugins().apply(MavenPublishPlugin.class);

        val publishing = project.getConvention().getByType(PublishingExtension.class);

        val mainPub = (MavenPublication)publishing.getPublications().findByName(PUBLICATION_NAME);

        if (mainPub == null) {
            return createMainPublication(project);
        }
        return mainPub;
    }


    private static MavenPublication createMainPublication(Project project) {
        val publishing = project.getConvention().getByType(PublishingExtension.class);
        val pub = publishing.getPublications().create(PUBLICATION_NAME, MavenPublication.class);

        publishing.getRepositories().mavenLocal();

        project.afterEvaluate(prj -> groupid(project, (MavenPomInternal)pub.getPom()));

        configPom(project, pub);

        return pub;
    }


    public void config() {
        plugins().apply(MavenPublishPlugin.class);

        // initialize the "main" publication if it hasn't already
        mainPublication(project);
    }


    private static void configPom(Project project, MavenPublication pub) {
        pub.pom(pom -> pom.withXml(xmlProvider -> configPom(project, xmlProvider)));
    }


    private static void configPom(Project project, XmlProvider xmlProvider) {
        val extension = defaultsExtension(project);
        if (extension == null) return;

        val rootNode = xmlProvider.asNode();

        name(project, rootNode);
        description(project, rootNode);
        siteUrl(extension, rootNode);
        organization(extension, rootNode);
        licenses(extension, rootNode);
        developers(extension, rootNode);
        contributors(extension, rootNode);
        scm(extension, rootNode);
    }


    private static void groupid(Project project, MavenPomInternal pom) {
        final MavenProjectIdentity projectIdentity = pom.getProjectIdentity();
        if (!isNotEmpty(projectIdentity.getGroupId())) {
            if (!isNotEmpty(project.getGroup().toString()))
                throw new IllegalStateException("There is no group set on the project");

            projectIdentity.setGroupId(project.getGroup().toString());
        }
    }


    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    private static void organization(DefaultsExtension extension, Node rootNode) {
        val orgName = extension.getOrgName();
        if (orgName != null) {
            val children = asList(n("name", orgName));
            val orgUrl = extension.getOrgUrl();
            if (orgUrl != null) children.add(n("url", orgUrl));
            createNode(rootNode, "organization", children);
        }
    }


    private static void siteUrl(DefaultsExtension extension, Node rootNode) {
        rootNode.appendNode("url", extension.getSiteUrl());
    }


    private static void description(Project project, Node rootNode) {
        if (isNotEmpty(project.getDescription())) {
            rootNode.appendNode("description", project.getDescription());
        }
    }


    private static void name(Project project, Node rootNode) {
        rootNode.appendNode("name", project.getName());
    }


    private static void scm(DefaultsExtension extension, Node rootNode) {
        createNode(rootNode, "scm", asList(
            n("connection", "scm:git:" + extension.getVcsReadUrl()),
            n("developerConnection", "scm:git:" + extension.getVcsWriteUrl()),
            n("url", extension.getSiteUrl()))
        );
    }


    private static void developers(DefaultsExtension extension, Node rootNode) {
        val developers = extension.getDevelopers();
        if (developers != null && !developers.isEmpty()) {
            val devNodes = developers.stream().
                map(developer -> n("developer", asList(
                    n("id", developer.getId()),
                    n("name", developer.getName()),
                    n("email", developer.getEmail()))
                )).
                collect(Collectors.<@NonNull NodeBuilder>toList());
            createNode(rootNode, "developers", devNodes);
        }
    }


    private static void contributors(DefaultsExtension extension, Node rootNode) {
        val contributors = extension.getContributors();
        if (contributors != null && !contributors.isEmpty()) {
            val contribNodes = contributors.stream().
                map(m -> n("contributor", asList(
                    // TODO ensure that the @NonNull is true...
                    n("name", (@NonNull String)m.get("name")),
                    n("email", (@NonNull String)m.get("email")))
                )).
                collect(Collectors.<@NonNull NodeBuilder>toList());
            createNode(rootNode, "contributors", contribNodes);
        }
    }


    private static void licenses(DefaultsExtension extension, Node rootNode) {
        createNode(rootNode, "licenses",
            n("license", asList(
                n("name", extension.getLicenseName()),
                n("url", extension.getLicenseUrl()))
            )
        );
    }


    private static @Nullable DefaultsExtension defaultsExtension(Project project) {
        if (project == null) return null;
        val extension = project.getExtensions().findByType(DefaultsExtension.class);
        if (extension != null) return extension;
        return defaultsExtension(project.getParent());
    }

}
