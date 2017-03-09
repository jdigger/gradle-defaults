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
package com.mooregreatsoftware.gradle.util;

import lombok.val;
import org.ajoberstar.grgit.Grgit;
import org.apache.commons.lang.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.jgit.lib.StoredConfig;
import org.gradle.api.Project;

import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("RedundantCast")
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


    @SuppressWarnings("WeakerAccess")
    public static final String DEFAULT_USER_EMAIL = "unknown@unknown";

    private static final String EMAIL_CACHE_KEY = "com.mooregreatsoftware.defaults.useremail";


    /**
     * The user's email from the git repository's configuration. If it can't be retrieved (including if there isn't
     * a git repository) then {@link #DEFAULT_USER_EMAIL} is used.
     */
    public static String userEmail(Project project) {
        val rootExt = project.getRootProject().getExtensions().getExtraProperties();

        if (rootExt.has(EMAIL_CACHE_KEY)) {
            return (String)rootExt.get(EMAIL_CACHE_KEY);
        }
        else {
            val userEmail = detectUserEmail(project);
            rootExt.set(EMAIL_CACHE_KEY, userEmail);
            return userEmail;
        }
    }


    private static String detectUserEmail(Project project) {
        val grgit = grgit(project);
        if (grgit == null) {
            project.getLogger().warn("There is no git repository to detect the user's email address from," +
                " so using \"{}\" instead", DEFAULT_USER_EMAIL);
            return DEFAULT_USER_EMAIL;
        }
        val storedConfig = grgit.getRepository().getJgit().getRepository().getConfig();
        val userEmail = email(storedConfig);
        if (StringUtils.isBlank(userEmail)) {
            project.getLogger().warn("The git repository's \"user.email\" configuration is null, " +
                "so using \"{}\" instead", DEFAULT_USER_EMAIL);
            return DEFAULT_USER_EMAIL;
        }
        else {
            return (@NonNull String)userEmail;
        }
    }


    private static @Nullable String email(StoredConfig storedConfig) {
        return storedConfig.getString("user", null, "email");
    }

}
