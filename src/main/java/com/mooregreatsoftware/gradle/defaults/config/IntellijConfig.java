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

import com.mooregreatsoftware.gradle.defaults.xml.XmlUtils;
import groovy.util.Node;
import groovy.util.NodeList;
import lombok.val;
import org.gradle.api.Project;
import org.gradle.api.XmlProvider;
import org.gradle.plugins.ide.idea.model.IdeaModel;
import org.gradle.plugins.ide.idea.model.IdeaProject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static com.mooregreatsoftware.gradle.defaults.xml.XmlUtils.createNode;
import static com.mooregreatsoftware.gradle.defaults.xml.XmlUtils.m;
import static com.mooregreatsoftware.gradle.defaults.xml.XmlUtils.n;
import static java.util.Arrays.asList;

@SuppressWarnings("WeakerAccess")
public class IntellijConfig extends AbstractConfig {
    private final Supplier<String> compatibilityVersionSupplier;


    protected IntellijConfig(Project project, Supplier<String> compatibilityVersionSupplier) {
        super(project);
        this.compatibilityVersionSupplier = compatibilityVersionSupplier;
    }


    public static IntellijConfig create(Project project, Supplier<String> compatibilityVersionSupplier) {
        return new IntellijConfig(project, compatibilityVersionSupplier).config();
    }


    protected IntellijConfig config() {
        plugins().apply("idea");

        // everything below this is only at the (top-level) "Project" level, not the "Module" level
        if (!isRootProject(project)) return this;

        info("Configuring the 'idea' plugin");

        val ideaProject = ideaProject();
        ideaProject.setVcs("Git");

        ideaProject.getIpr().withXml(this::customizeProjectXml);

        project.afterEvaluate(prj -> configLanguageVersion());

        return this;
    }


    private void configLanguageVersion() {
        val ideaProject = ideaProject();
        val compatibilityVersion = getLanguageVersion();
        ideaProject.setJdkName(compatibilityVersion);
        ideaProject.setLanguageLevel(compatibilityVersion);
    }


    public IdeaProject ideaProject() {
        return ideaModel().getProject();
    }


    public static IdeaProject ideaProject(Project project) {
        return ideaModel(project).getProject();
    }


    public String getLanguageVersion() {
        return compatibilityVersionSupplier.get();
    }


    private void customizeProjectXml(XmlProvider provider) {
        val rootNode = provider.asNode();

        addGradleHome(rootNode);

        setupCodeStyle(rootNode);
    }


    private static void setupCodeStyle(Node rootNode) {
        val codeStyleNode = codeStyleNode(rootNode);
        codeStyleNode.setValue(new ArrayList<>());// remove any previous children

        codeStyleNode.appendNode("option", nv("USE_PER_PROJECT_SETTINGS", "true"));

        @SuppressWarnings("OptionalGetWithoutIsPresent") final Node perProjSettings =
            (Node)createNode(codeStyleNode, "option", m("name", "PER_PROJECT_SETTINGS"), n("value")).
                children().stream().findFirst().get();

        asList(
            n("option", m("name", "OTHER_INDENT_OPTIONS"),
                n("value", asList(
                    n("option", nv("INDENT_SIZE", "4")),
                    n("option", nv("CONTINUATION_INDENT_SIZE", "4")),
                    n("option", nv("TAB_SIZE", "4")),
                    n("option", nv("USE_TAB_CHARACTER", "false")),
                    n("option", nv("SMART_TABS", "false")),
                    n("option", nv("LABEL_INDENT_SIZE", "0")),
                    n("option", nv("LABEL_INDENT_ABSOLUTE", "false")),
                    n("option", nv("USE_RELATIVE_INDENTS", "false")))
                )
            ),
            n("option", nv("CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND", "9999")),
            n("option", nv("NAMES_COUNT_TO_USE_IMPORT_ON_DEMAND", "9999")),
            n("XML",
                n("option", nv("XML_LEGACY_SETTINGS_IMPORTED", "true"))
            ),
            n("GroovyCodeStyleSettings", asList(
                n("option", nv("CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND", "9999")),
                n("option", nv("NAMES_COUNT_TO_USE_IMPORT_ON_DEMAND", "9999"))
            ))
        ).forEach(child -> createNode(perProjSettings, child.name(), child.attrs(), child.children()));

        asList("Groovy", "JAVA", "Scala").forEach(lang ->
            createNode(perProjSettings, "codeStyleSettings", m("language", lang), asList(
                n("option", nv("BLANK_LINES_AROUND_METHOD", "2")),
                n("option", nv("ELSE_ON_NEW_LINE", "true")),
                n("option", nv("CATCH_ON_NEW_LINE", "true")),
                n("option", nv("FINALLY_ON_NEW_LINE", "false")),
                n("option", nv("SPACE_AFTER_TYPE_CAST", "false")),
                n("option", nv("INDENT_SIZE", "2")),
                n("option", nv("TAB_SIZE", "4")),
                n("option", nv("CONTINUATION_INDENT_SIZE", "4")),
                n("indentOptions",
                    n("option", nv("CONTINUATION_INDENT_SIZE", "4"))
                )
            )));
    }


    public static Node codeStyleNode(Node rootNode) {
        val componentNodes = componentNodes(rootNode);

        return XmlUtils.findByAttribute(componentNodes, "name", "ProjectCodeStyleSettingsManager",
            () -> rootNode.appendNode("component", m("name", "ProjectCodeStyleSettingsManager"))
        );
    }


    public static NodeList componentNodes(Node rootNode) {
        return (NodeList)rootNode.get("component");
    }


    private void addGradleHome(Node rootNode) {
        createNode(rootNode, "component", m("name", "GradleSettings"),
            n("option", nv("SDK_HOME", gradle().getGradleHomeDir().toString()))
        );
    }


    public IdeaModel ideaModel() {
        return ideaModel(project);
    }


    public static IdeaModel ideaModel(Project project) {
        return project.getExtensions().findByType(IdeaModel.class);
    }


    public static Map<String, String> nv(String name, String value) {
        val map = new HashMap<String, String>();
        map.put("name", name);
        map.put("value", value);
        return map;
    }

}
