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
import groovy.util.Node;
import lombok.val;
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

@SuppressWarnings("WeakerAccess")
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

        project.afterEvaluate(prj -> {
            groupid(project, (MavenPomInternal)pub.getPom());
        });

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


    private static void organization(DefaultsExtension extension, Node rootNode) {
        if (extension.getOrgName() != null) {
            createNode(rootNode, "organization", asList(
                n("name", extension.getOrgName()),
                n("url", extension.getOrgUrl()))
            );
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
        if (extension.getDevelopers() != null && !extension.getDevelopers().isEmpty()) {
            val devNodes = extension.getDevelopers().stream().
                map(m -> n("developer", asList(
                    n("id", (String)m.get("id")),
                    n("name", (String)m.get("name")),
                    n("email", (String)m.get("email")))
                )).
                collect(Collectors.toList());
            createNode(rootNode, "developers", devNodes);
        }
    }


    private static void contributors(DefaultsExtension extension, Node rootNode) {
        if (extension.getDevelopers() != null && !extension.getDevelopers().isEmpty()) {
            val devNodes = extension.getDevelopers().stream().
                map(m -> n("contributor", asList(
                    n("name", (String)m.get("name")),
                    n("email", (String)m.get("email")))
                )).
                collect(Collectors.toList());
            createNode(rootNode, "contributors", devNodes);
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


    private static DefaultsExtension defaultsExtension(Project project) {
        if (project == null) return null;
        val extension = project.getExtensions().findByType(DefaultsExtension.class);
        if (extension != null) return extension;
        return defaultsExtension(project.getParent());
    }

}
