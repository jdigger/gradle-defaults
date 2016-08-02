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
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;

@SuppressWarnings("RedundantCast")
public class XmlUtils {

    public static Node findByAttribute(NodeList nodes, String attrName, String attrValue, Supplier<Node> nodeCreator) {
        @SuppressWarnings("unchecked") val stream = (Stream<@NonNull Node>)nodes.stream();
        return stream.
            filter(n -> attributeMatches(n, attrName, attrValue)).
            findAny().
            orElseGet(nodeCreator);
    }


    public static Node getOrCreate(Node parent, String elementName, Supplier<Node> nodeCreator) {
        @SuppressWarnings("unchecked") val stream = (Stream<@NonNull Node>)parent.children().stream();
        return stream.
            filter(n -> n.name().equals(elementName)).
            findAny().
            orElseGet(nodeCreator);
    }


    private static boolean attributeMatches(Node node, String attrName, String attrValue) {
        val attr = (String)node.attributes().get(attrName);
        return attr != null && attr.equalsIgnoreCase(attrValue);
    }


    public static NodeBuilder n(String name, Map<String, Comparable> attrs) {
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


    public static NodeBuilder n(String name, @Nullable Map<String, Comparable> attrs, NodeBuilder child) {
        return new NodeBuilder(name, attrs, null, singletonList(child));
    }


    public static NodeBuilder n(String name, @Nullable Map<String, Comparable> attrs, List<NodeBuilder> children) {
        return new NodeBuilder(name, attrs, null, children);
    }


    public static NodeBuilder n(String name, NodeBuilder child) {
        return new NodeBuilder(name, emptyMap(), null, singletonList(child));
    }


    public static Node createNode(Node parent, String nodeName, NodeBuilder child) {
        return createNode(parent, nodeName, null, null, child);
    }


    public static Node createNode(Node parent, String nodeName, @Nullable Map<String, Comparable> attrs, NodeBuilder child) {
        return createNode(parent, nodeName, attrs, null, child);
    }


    public static Node createNode(Node parent, String nodeName, @Nullable Map<String, Comparable> attrs, @Nullable String textVal, NodeBuilder child) {
        return createNode(parent, nodeName, attrs, textVal, singletonList(child));
    }


    public static Node createNode(Node parent, String nodeName, List<NodeBuilder> children) {
        return createNode(parent, nodeName, null, null, children);
    }


    public static Node createNode(Node parent, String nodeName, @Nullable Map<String, ? extends Comparable> attrs, List<NodeBuilder> children) {
        return createNode(parent, nodeName, attrs, null, children);
    }


    public static Node createNode(Node parent, String nodeName, @Nullable Map<String, ? extends Comparable> attrs, @Nullable String textVal, List<NodeBuilder> children) {
        if (parent == null) throw new IllegalArgumentException("parent == null");
        if (nodeName == null) throw new IllegalArgumentException("nodeName == null");
        if (children == null) throw new IllegalArgumentException("children == null");
        val node = (textVal == null) ?
            parent.appendNode(nodeName, (@NonNull Map)attrs) :
            parent.appendNode(nodeName, (@NonNull Map)attrs, (@NonNull String)textVal);
        children.forEach(child ->
            createNode(node, child.name(), child.attrs(), child.textVal(), child.children())
        );
        return node;
    }


    /**
     * Alias for creating a {@link Collections#singletonMap(Object, Object)}
     */
    public static Map<String, Comparable> m(String key, String value) {
        return Collections.singletonMap(key, value);
    }

}
