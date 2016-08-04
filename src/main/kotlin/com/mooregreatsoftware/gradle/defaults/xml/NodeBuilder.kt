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
package com.mooregreatsoftware.gradle.defaults.xml

import com.mooregreatsoftware.gradle.defaults.compareMaps
import java.util.Comparator.nullsFirst
import java.util.Objects.compare

data class NodeBuilder(val name: String, val attrs: Map<String, String>?,
                       val textVal: String?, val children: List<NodeBuilder>) : Comparable<NodeBuilder> {

    override fun toString(): String {
        return super.toString()
    }

    override fun compareTo(other: NodeBuilder): Int {
        if (this === other) return 0

        val nameComp = this.name.compareTo(other.name, ignoreCase = true)
        if (nameComp != 0) return nameComp

        val textValComp = compare<String>(textVal, other.textVal, nullsFirst({ obj, str -> obj.compareTo(str, ignoreCase = true) }))
        if (textValComp != 0) return textValComp

        return compareMaps(attrs, other.attrs)
    }

}
