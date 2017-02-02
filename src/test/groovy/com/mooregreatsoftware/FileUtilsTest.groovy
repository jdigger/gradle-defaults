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
package com.mooregreatsoftware

import spock.lang.Specification

import java.nio.file.Files

import static com.mooregreatsoftware.FileUtils.findBreadthFirst
import static com.mooregreatsoftware.TestUtils.createFile

@SuppressWarnings("GroovyPointlessBoolean")
class FileUtilsTest extends Specification {

    def "findBreadthFirst"() {
        def dirPath = Files.createTempDirectory("gradle-defaults-fileutils-test")

        createFile(dirPath.resolve("A/B/afile"))
        createFile(dirPath.resolve("A/A/bfile"))
        createFile(dirPath.resolve("A/C/bfile"))
        createFile(dirPath.resolve("A/A/A/afile"))

        expect:
        findBreadthFirst(dirPath, { path -> path.fileName.toString() == "afile" }).get() == dirPath.resolve("A/B/afile")

        findBreadthFirst(dirPath, { path -> path.fileName.toString() == "xfile" }).isPresent() == false
    }


}
