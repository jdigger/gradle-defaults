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
import org.gradle.api.Project;
import org.gradle.api.publish.Publication;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;

import java.util.List;
import java.util.stream.Collectors;

import static com.mooregreatsoftware.gradle.defaults.xml.XmlUtils.createNode;
import static com.mooregreatsoftware.gradle.defaults.xml.XmlUtils.n;
import static java.util.Arrays.asList;

@SuppressWarnings("WeakerAccess")
public class MavenPublishingConfig extends AbstractConfigWithExtension {
    public static final String PUBLICATION_NAME = "main";


    public MavenPublishingConfig(Project project, DefaultsExtension extension) {
        super(project, extension);
    }


    public void config() {
        plugins().apply("maven-publish");

        PublishingExtension publishing = project.getConvention().getByType(PublishingExtension.class);

        Publication mainPub = publishing.getPublications().findByName(PUBLICATION_NAME);
        if (mainPub == null) {
            project.afterEvaluate(prj -> {
                final MavenPublication pub = publishing.getPublications().create(PUBLICATION_NAME, MavenPublication.class);
                publishing.getRepositories().mavenLocal();
                plugins().withId("java", plugin -> {
                    pub.from(project.getComponents().getByName("java"));
                    pub.artifact(tasks().getByName("sourcesJar"));
                    pub.artifact(tasks().getByName("javadocJar"));
                });

                plugins().withId("groovy", plugin -> pub.artifact(tasks().getByName("groovydocJar")));
                plugins().withId("scala", plugin -> pub.artifact(tasks().getByName("scaladocJar")));

                pub.pom(pom ->
                    pom.withXml(xmlProvider -> {
                            if (project.getGroup() == null || project.getGroup().toString().trim().isEmpty())
                                throw new IllegalStateException("There is no group set on the project");

                            Node rootNode = xmlProvider.asNode();
                            rootNode.appendNode("name", name());
                            rootNode.appendNode("description", project.getDescription());
                            rootNode.appendNode("url", extension.getSiteUrl());
                            if (extension.getOrgName() != null) {
                                createNode(rootNode, "organization", asList(
                                    n("name", extension.getOrgName()),
                                    n("url", extension.getOrgUrl()))
                                );
                            }

                            createNode(rootNode, "licenses",
                                n("license", asList(
                                    n("name", extension.getLicenseName()),
                                    n("url", extension.getLicenseUrl()))
                                )
                            );

                            if (extension.getDevelopers() != null && !extension.getDevelopers().isEmpty()) {
                                List<NodeBuilder> devNodes = extension.getDevelopers().stream().
                                    map(m -> n("developer", asList(
                                        n("id", (String)m.get("id")),
                                        n("name", (String)m.get("name")),
                                        n("email", (String)m.get("email")))
                                    )).
                                    collect(Collectors.toList());
                                createNode(rootNode, "developers", devNodes);
                            }

                            if (extension.getContributors() != null && !extension.getContributors().isEmpty()) {
                                List<NodeBuilder> devNodes = extension.getContributors().stream().
                                    map(m -> n("contributor", asList(
                                        n("id", (String)m.get("id")),
                                        n("name", (String)m.get("name")),
                                        n("email", (String)m.get("email")))
                                    )).
                                    collect(Collectors.toList());
                                createNode(rootNode, "developers", devNodes);
                            }

                            createNode(rootNode, "scm", asList(
                                n("connection", "scm:git:" + extension.getVcsReadUrl()),
                                n("developerConnection", "scm:git:" + extension.getVcsWriteUrl()),
                                n("url", extension.getSiteUrl()))
                            );
                        }
                    )
                );
            });
        }
    }

}
