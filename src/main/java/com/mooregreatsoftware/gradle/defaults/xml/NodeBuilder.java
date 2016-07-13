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

import com.mooregreatsoftware.gradle.defaults.Utils;
import lombok.Value;
import lombok.val;

import java.util.List;
import java.util.Map;

import static java.util.Comparator.nullsFirst;
import static java.util.Objects.compare;

@Value
public class NodeBuilder implements Comparable {
    String name;
    Map<String, Comparable> attrs;
    String textVal;
    List<NodeBuilder> children;


    @Override
    @SuppressWarnings("unchecked")
    public int compareTo(Object o) {
        if (this == o) return 0;

        val other = (NodeBuilder)o;

        val nameComp = this.name.compareToIgnoreCase((other.name()));
        if (nameComp != 0) return nameComp;

        val textValComp = compare(textVal, other.textVal(), nullsFirst(String::compareToIgnoreCase));
        if (textValComp != 0) return textValComp;

        return Utils.compareMaps(attrs, other.attrs());
    }

}
