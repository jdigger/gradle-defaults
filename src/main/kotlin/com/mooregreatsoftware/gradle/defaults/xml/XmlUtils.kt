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
package com.mooregreatsoftware.gradle.defaults.xml

import groovy.util.Node
import groovy.util.NodeList
import java.util.Collections.emptyMap

fun NodeList.findByAttribute(attrName: String, attrValue: String, nodeCreator: () -> Node): Node {
    @Suppress("UNCHECKED_CAST")
    return (this as Iterable<Node>).find { it.attributeMatches(attrName, attrValue) } ?: nodeCreator()
}


fun Node.getOrCreate(elementName: String, nodeCreator: () -> Node): Node {
    @Suppress("UNCHECKED_CAST")
    return (this.children() as Iterable<Node>).find { it.name() == elementName } ?: nodeCreator()
}


private fun Node.attributeMatches(attrName: String, attrValue: String): Boolean {
    val attr = this.attributes()[attrName] as String?
    return attr != null && attr.equals(attrValue, ignoreCase = true)
}

private fun noNodes(): List<NodeBuilder> = listOf()

fun n(name: String, attrs: Map<String, String>) = NodeBuilder(name, attrs, null, noNodes())
fun n(name: String, textVal: String): NodeBuilder = NodeBuilder(name, mapOf(), textVal, listOf())
fun n(name: String, children: Iterable<NodeBuilder>) = NodeBuilder(name, emptyMap(), null, children)
fun n(name: String) = NodeBuilder(name, emptyMap(), null, noNodes())
fun n(name: String, attrs: Map<String, String>?, child: NodeBuilder) = NodeBuilder(name, attrs, null, listOf(child))
fun n(name: String, attrs: Map<String, String>?, children: Iterable<NodeBuilder>) = NodeBuilder(name, attrs, null, children)
fun n(name: String, child: NodeBuilder) = NodeBuilder(name, emptyMap(), null, listOf(child))

fun Node.appendChild(nodeName: String) = this.appendChildren(nodeName, null, listOf())
fun Node.appendChild(nodeName: String, attrs: Map<String, String>) = this.appendChildren(nodeName, attrs, listOf())
fun Node.appendChild(nodeName: String, child: NodeBuilder) = this.appendChild(nodeName, null, child)
fun Node.appendChild(nodeName: String, attrs: Map<String, String>?, child: NodeBuilder) = this.appendChildren(nodeName, attrs, null, child)

fun Node.appendChildren(nodeName: String, attrs: Map<String, String>?, textVal: String?, child: NodeBuilder) = this.appendChildren(nodeName, attrs, textVal, listOf(child))
fun Node.appendChildren(nodeName: String, children: Iterable<NodeBuilder>) = this.appendChildren(nodeName, null, null, children)
fun Node.appendChildren(nodeName: String, attrs: Map<String, String>?, children: Iterable<NodeBuilder>) = this.appendChildren(nodeName, attrs, null, children)
fun Node.appendChildren(nodeName: String, attrs: Map<String, String>?, textVal: String?, children: Iterable<NodeBuilder>): Node {
    val node = when (textVal) {
        null -> this.appendNode(nodeName, attrs as Map<*, *>?)
        else -> this.appendNode(nodeName, attrs as Map<*, *>?, textVal)
    }
    children.forEach { node.appendChildren(it.name, it.attrs, it.textVal, it.children) }
    return node
}
