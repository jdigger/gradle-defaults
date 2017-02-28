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
package com.mooregreatsoftware;

import lombok.val;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Optional;
import java.util.function.Function;

@SuppressWarnings("Convert2MethodRef")
public final class FileUtils {

    private FileUtils() {
    }


    /**
     * Search the path tree breadth-first, returning the first path that meets "matcher."
     *
     * @param sourcePath where to start the search
     * @param matcher    the predicate to match
     * @return empty() if nothing matches
     */
    public static Optional<Path> findBreadthFirst(Path sourcePath, Function<Path, Boolean> matcher) throws IOException {
        if (Files.notExists(sourcePath)) return Optional.empty();

        val dirQueue = new ArrayDeque<Path>();
        dirQueue.add(sourcePath);

        while (!dirQueue.isEmpty()) {
            try (val pathStream = Files.list(dirQueue.removeFirst())) {
                val foundPath = pathStream.
                    peek(path -> {
                        if (Files.isDirectory(path)) dirQueue.add(path);
                    }).
                    filter(path -> matcher.apply(path)).findAny();
                if (foundPath.isPresent()) return foundPath;
            }
        }

        return Optional.empty();
    }

}
