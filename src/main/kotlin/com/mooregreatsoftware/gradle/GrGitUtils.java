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
package com.mooregreatsoftware.gradle;

import lombok.val;
import org.ajoberstar.grgit.Grgit;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.gradle.api.Project;

import java.util.concurrent.atomic.AtomicReference;

public final class GrGitUtils {

    /**
     * Retrieves the Grigit instance for the root project (cached in the "ext" properties). It's created if this is the
     * first request.
     *
     * @param project the project to get the Grgit instance for. If not the root project, the root project is used.
     * @return null if this project is not have a git repository
     */
    @SuppressWarnings({"unchecked", "deprecation"})
    public static @Nullable Grgit grgit(Project project) {
        String key = Grgit.class.getName();
        val rootProject = project.getRootProject();
        val ext = rootProject.getExtensions().getExtraProperties();
        if (ext.has(key)) {
            return ((AtomicReference<Grgit>)ext.get(key)).get();
        }
        else {
            AtomicReference<Grgit> grgitRef;
            try {
                val grgit = Grgit.open(rootProject.getProjectDir());
                grgitRef = new AtomicReference<>(grgit);
            }
            catch (Exception exp) {
                grgitRef = new AtomicReference<>();
            }

            ext.set(key, grgitRef);
            return grgitRef.get();
        }
    }

}
