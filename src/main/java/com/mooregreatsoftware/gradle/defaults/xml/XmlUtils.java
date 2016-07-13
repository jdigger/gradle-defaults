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
package com.mooregreatsoftware.gradle.defaults.xml;

import groovy.util.Node;
import groovy.util.NodeList;
import lombok.val;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;

public class XmlUtils {

    public static Node findByAttribute(NodeList nodes, String attrName, String attrValue, Supplier<Node> nodeCreator) {
        @SuppressWarnings("unchecked") final Stream<Node> stream = nodes.stream();
        return stream.
            filter(n -> attributeMatches(n, attrName, attrValue)).
            findAny().
            orElseGet(nodeCreator);
    }


    public static Node getOrCreate(Node parent, String elementName, Supplier<Node> nodeCreator) {
        @SuppressWarnings("unchecked") final Stream<Node> stream = parent.children().stream();
        return stream.
            filter(n -> n.name().equals(elementName)).
            findAny().
            orElseGet(nodeCreator);
    }


    private static boolean attributeMatches(Node node, String attrName, String attrValue) {
        val attr = (String)node.attributes().get(attrName);
        return attr != null && attr.equalsIgnoreCase(attrValue);
    }


    public static NodeBuilder n(String name, Map attrs) {
        return new NodeBuilder(name, attrs, null, noNodes());
    }


    public static NodeBuilder n(String name, String textVal) {
        return new NodeBuilder(name, emptyMap(), textVal, noNodes());
    }


    private static List<NodeBuilder> noNodes() {
        return Collections.emptyList();
    }


    public static NodeBuilder n(String name, List<NodeBuilder> children) {
        return new NodeBuilder(name, emptyMap(), null, children);
    }


    public static NodeBuilder n(String name) {
        return new NodeBuilder(name, emptyMap(), null, noNodes());
    }


    public static NodeBuilder n(String name, Map attrs, NodeBuilder child) {
        return new NodeBuilder(name, attrs, null, singletonList(child));
    }


    public static NodeBuilder n(String name, Map attrs, List<NodeBuilder> children) {
        return new NodeBuilder(name, attrs, null, children);
    }


    public static NodeBuilder n(String name, NodeBuilder child) {
        return new NodeBuilder(name, emptyMap(), null, singletonList(child));
    }


    public static Node createNode(Node parent, String nodeName, NodeBuilder child) {
        return createNode(parent, nodeName, null, null, child);
    }


    public static Node createNode(Node parent, String nodeName, Map attrs, NodeBuilder child) {
        return createNode(parent, nodeName, attrs, null, child);
    }


    public static Node createNode(Node parent, String nodeName, Map attrs, String textVal, NodeBuilder child) {
        return createNode(parent, nodeName, attrs, textVal, singletonList(child));
    }


    public static Node createNode(Node parent, String nodeName, List<NodeBuilder> children) {
        return createNode(parent, nodeName, null, null, children);
    }


    public static Node createNode(Node parent, String nodeName, Map attrs, List<NodeBuilder> children) {
        return createNode(parent, nodeName, attrs, null, children);
    }


    public static Node createNode(Node parent, String nodeName, Map attrs, String textVal, List<NodeBuilder> children) {
        if (parent == null) throw new IllegalArgumentException("parent == null");
        if (nodeName == null) throw new IllegalArgumentException("nodeName == null");
        if (children == null) throw new IllegalArgumentException("children == null");
        val node = textVal == null ?
            parent.appendNode(nodeName, attrs) :
            parent.appendNode(nodeName, attrs, textVal);
        children.forEach(child ->
            createNode(node, child.name(), child.attrs(), child.textVal(), child.children())
        );
        return node;
    }


    /**
     * Alias for creating a {@link Collections#singletonMap(Object, Object)}
     */
    public static Map<String, String> m(String key, String value) {
        return Collections.singletonMap(key, value);
    }

}
