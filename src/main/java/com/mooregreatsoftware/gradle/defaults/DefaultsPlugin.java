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
package com.mooregreatsoftware.gradle.defaults;

import com.mooregreatsoftware.gradle.defaults.config.BintrayConfig;
import com.mooregreatsoftware.gradle.defaults.config.GhPagesConfig;
import com.mooregreatsoftware.gradle.defaults.config.GroovyConfig;
import com.mooregreatsoftware.gradle.defaults.config.IntellijConfig;
import com.mooregreatsoftware.gradle.defaults.config.JavaConfig;
import com.mooregreatsoftware.gradle.defaults.config.LicenseConfig;
import com.mooregreatsoftware.gradle.defaults.config.LombokConfiguration;
import com.mooregreatsoftware.gradle.defaults.config.MavenPublishingConfig;
import com.mooregreatsoftware.gradle.defaults.config.ReleaseConfig;
import com.mooregreatsoftware.gradle.defaults.config.ScalaConfig;
import lombok.val;
import org.ajoberstar.grgit.Grgit;
import org.eclipse.jgit.api.Git;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.publish.plugins.PublishingPlugin;

import java.io.IOException;
import java.util.Objects;

@SuppressWarnings({"GrMethodMayBeStatic", "WeakerAccess"})
public class DefaultsPlugin implements Plugin<Project> {

    public static final String DEFAULT_USER_EMAIL = "unknown@unknown";
    private static final String EMAIL_CACHE_KEY = "com.mooregreatsoftware.defaults.useremail";


    public void apply(Project project) {
        if (!project.equals(project.getRootProject())) {
            project.getLogger().warn(this.getClass().getName() + " can only be applied to the root project");
            return;
        }

        val grgit = createGrgit(project);

        val extension = project.getExtensions().create("defaults", DefaultsExtension.class, project);

        project.getPlugins().apply("org.ajoberstar.organize-imports");

        if (grgit != null) {
            new GhPagesConfig(project).config(extension::getVcsWriteUrl);
            new ReleaseConfig(project, grgit).config();
        }

        project.allprojects(prj -> {
            prj.getRepositories().jcenter();
            new IntellijConfig(prj).config(extension::getCompatibilityVersion);
            new JavaConfig(prj).config(extension::getCompatibilityVersion);
            new GroovyConfig(prj).config(extension::getCompatibilityVersion);
            new ScalaConfig(prj).config(extension::getCompatibilityVersion);
            new LicenseConfig(prj).config(extension::getCopyrightYears);
            new MavenPublishingConfig(prj, extension).config();
            new BintrayConfig(prj, extension).config();
            new LombokConfiguration(prj).config(extension::getLobokVersion);
            addOrderingRules(prj);
        });
    }


    @SuppressWarnings("deprecation")
    private static Grgit createGrgit(Project project) {
        try {
            return Grgit.open(project.file("."));
        }
        catch (Exception e) {
            return null;
        }
    }


    private static void addOrderingRules(final Project project) {
        project.getPlugins().withType(BasePlugin.class, plugin -> {
            allTasksShouldRunAfterClean(project);
            publishingTasksShouldRunAfterBuild(project);
        });
    }


    private static void allTasksShouldRunAfterClean(Project project) {
        val clean = project.getTasks().getByName(BasePlugin.CLEAN_TASK_NAME);
        project.getTasks().forEach(task -> {
            if (!task.equals(clean)) {
                task.shouldRunAfter(clean);
            }
        });
    }


    private static void publishingTasksShouldRunAfterBuild(Project project) {
        val build = project.getTasks().getByName(BasePlugin.BUILD_GROUP);
        project.getTasks().forEach(task -> {
            if (Objects.equals(task.getGroup(), PublishingPlugin.PUBLISH_TASK_GROUP)) {
                task.shouldRunAfter(build);
            }
        });
    }


    public static String userEmail(Project project) {
        val rootExt = project.getRootProject().getExtensions().getExtraProperties();
        if (rootExt.has(EMAIL_CACHE_KEY)) {
            return (String)rootExt.get(EMAIL_CACHE_KEY);
        }

        val userEmail = detectUserEmail(project);
        rootExt.set(EMAIL_CACHE_KEY, userEmail);
        return userEmail;
    }


    private static String detectUserEmail(Project project) {
        try {
            val rootDir = project.getRootDir();
            val git = Git.open(rootDir);
            val repository = git.getRepository();
            val config = repository.getConfig();
            val userEmail = config.getString("user", null, "email");
            if (userEmail == null || userEmail.trim().isEmpty()) {
                project.getLogger().warn("The git repository's \"user.email\" configuration is null, " +
                    "so using \"" + DEFAULT_USER_EMAIL + "\" instead");
                return DEFAULT_USER_EMAIL;
            }
            return userEmail;
        }
        catch (IOException e) {
            project.getLogger().warn("Could not detect the user's email address from the " +
                "git repository's \"user.email\" configuration, so using \"" + DEFAULT_USER_EMAIL + "\" instead");
            return DEFAULT_USER_EMAIL;
        }
    }

}
