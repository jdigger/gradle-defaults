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

import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.regex.Pattern

/**
 * Returns true if the given string is non-null and has at least one non-whitespace character; false otherwise.
 */
fun isNotEmpty(str: String?): Boolean {
    return !(str == null || str.trim { it <= ' ' }.isEmpty())
}

val maxIntStr = Integer.MAX_VALUE.toString()

/**
 * Delete a directory, akin to "rm -rf".
 */
@Throws(IOException::class)
fun deleteDir(path: Path) {
    Files.walkFileTree(path, object : SimpleFileVisitor<Path>() {
        @Throws(IOException::class)
        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
            Files.delete(file)
            return FileVisitResult.CONTINUE
        }


        @Throws(IOException::class)
        override fun postVisitDirectory(dir: Path, e: IOException?): FileVisitResult {
            if (e == null) {
                Files.delete(dir)
                return FileVisitResult.CONTINUE
            }
            else {
                // directory iteration failed
                throw e
            }
        }
    })
}

fun <T> Iterable<T>.hasItems() = when (this) {
    is Collection -> this.isNotEmpty()
    else -> this.iterator().hasNext()
}


fun jdkVersion(): JdkVersion {
    val javaVer = System.getProperty("java.version")
    val matcher = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)_(\\d+)$").matcher(javaVer)
    if (matcher.matches()) {
        val major = Integer.parseInt(matcher.group(1) as String)
        val minor = Integer.parseInt(matcher.group(2) as String)
        val patch = Integer.parseInt(matcher.group(3) as String)
        val update = Integer.parseInt(matcher.group(4) as String)

        return JdkVersion(major, minor, patch, update)
    }
    else {
        throw IllegalArgumentException("Could not parse \"" + javaVer + "\"")
    }
}


val isBuggyJavac: Boolean
    get() {
        val jdkVersion = jdkVersion()
        return jdkVersion.minor == 8 && jdkVersion.update < 45
    }


/**
 * Compares two [Map]s of [Comparable] items. If one is null, that sorts lower. If one's size is
 * smaller, that sorts lower. Otherwise the keys are compared. (See [.compareSets].)
 * If the keys are the same, the first difference in the values found when searching the sorted keys is used for
 * the comparision.
 *
 * @see Comparable
 */
fun <K : Comparable<K>, V : Comparable<V>> compareMaps(m1: Map<K, V>?,
                                                       m2: Map<K, V>?): Int {
    if (m1 == null) {
        return if (m2 == null) 0 else -1
    }
    else if (m2 == null) return 1

    if (m1.size == m2.size) {
        if (m1.isEmpty()) return 0
        val m1Keys = m1.keys
        val m2Keys = m2.keys
        val keysComp = compareSets(m1Keys, m2Keys)
        if (keysComp == 0) {
            val keyIter = m1Keys.sorted().iterator()
            while (keyIter.hasNext()) {
                val key = keyIter.next()
                val m1Val = m1[key] as V
                val m2Val = m2[key] as V
                val valComp = m1Val.compareTo(m2Val)
                if (valComp != 0) return valComp
            }
            return 0
        }
        else
            return keysComp
    }
    else
        return if (m1.size > m2.size) 1 else -1
}


/**
 * Compares two [Set]s of [Comparable] items. If one is null, that sorts lower. If one's size is
 * smaller, that sorts lower. Otherwise the first non-equal value after they are sorted is used for the comparision.

 * @see Comparable
 */
fun <U : Comparable<U>> compareSets(s1: Set<U>?, s2: Set<U>?): Int {
    if (s1 == null) {
        return if (s2 == null) 0 else -1
    }
    else if (s2 == null) return 1

    if (s1.size == s2.size) {
        if (s1.isEmpty()) return 0
        val s1SortedIter = s1.sorted().iterator()
        val s2SortedIter = s2.sorted().iterator()
        while (s1SortedIter.hasNext()) { // both iter's hasNext() is guaranteed to be the same since same size
            val s1Val = s1SortedIter.next()
            val s2Val = s2SortedIter.next()
            val compVal = s1Val.compareTo(s2Val)
            if (compVal != 0) return compVal
        }
        return 0
    }
    else
        return if (s1.size > s2.size) 1 else -1
}


data class JdkVersion(val major: Int, val minor: Int, val patch: Int, val update: Int)
