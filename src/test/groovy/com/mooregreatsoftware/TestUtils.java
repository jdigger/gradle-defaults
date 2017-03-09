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

import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TestUtils {
    public static Path testLibsPath() throws IOException {
        return topLevelDir().resolve("test-libs");
    }


    @SuppressWarnings("WeakerAccess")
    public static Path topLevelDir() throws IOException {
        return getTopLevelDir(new File(".").getCanonicalFile().toPath());
    }


    public static Path createFile(Path filePath) throws IOException {
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, new byte[0]);
        return filePath;
    }


    // **********************************************************************
    //
    // PRIVATE METHODS
    //
    // **********************************************************************


    private static Path getTopLevelDir(@Nullable Path dir) throws IOException {
        if (dir == null) throw new IllegalArgumentException("dir == null");
        return (isTopLevelDir(dir)) ? dir : getTopLevelDir(dir.getParent());
    }


    private static boolean isTopLevelDir(@Nullable Path dir) throws IOException {
        if (dir == null) throw new IllegalArgumentException("dir == null");
        return Files.walk(dir, 1).anyMatch(path -> ".travis.yml".equals(path.getFileName().toString()));
    }

}
