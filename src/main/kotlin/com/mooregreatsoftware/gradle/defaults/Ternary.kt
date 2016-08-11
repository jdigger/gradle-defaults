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
package com.mooregreatsoftware.gradle.defaults

enum class Ternary {
    TRUE, FALSE, MAYBE
}

fun Any?.asTernary(): Ternary {
    fun boolToTern(bool: Boolean) = if (bool) Ternary.TRUE else Ternary.FALSE
    return when (this) {
        null -> Ternary.MAYBE
        is Ternary -> this
        is Boolean -> boolToTern(this)
        is String -> boolToTern(java.lang.Boolean.valueOf(this))
        else -> throw IllegalArgumentException("Don't know how to parse \"$this\"")
    }
}
