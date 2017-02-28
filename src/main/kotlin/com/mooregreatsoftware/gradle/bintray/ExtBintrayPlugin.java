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
package com.mooregreatsoftware.gradle.bintray;

import com.google.common.io.Files;
import com.jfrog.bintray.gradle.BintrayExtension;
import com.jfrog.bintray.gradle.BintrayPlugin;
import com.mooregreatsoftware.gradle.defaults.ProjectUtilsKt;
import com.mooregreatsoftware.gradle.defaults.ReadableDefaultsExtension;
import com.mooregreatsoftware.gradle.defaults.ReadableDefaultsExtensionKt;
import com.mooregreatsoftware.gradle.maven.MavenPublishPublications;
import lombok.val;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static com.mooregreatsoftware.gradle.defaults.UtilsKt.stream;

/**
 * Applies the {@link BintrayPlugin} from JFrog and sets up reasonable defaults that are coordinated with other
 * "known" plugins (such as for POM.xml data).
 */
@SuppressWarnings({"RedundantCast", "RedundantTypeArguments", "Convert2MethodRef"})
public class ExtBintrayPlugin implements Plugin<Project> {
    private static final Logger LOG = LoggerFactory.getLogger(ExtBintrayPlugin.class);


    @Override
    @SuppressWarnings("argument.type.incompatible")
    public void apply(Project project) {
        project.getPlugins().apply(BintrayPlugin.class);

        val readExtensionFuture = ReadableDefaultsExtensionKt.readableDefaultsExtension(project);
        ProjectUtilsKt.postEvalCreate(project, () -> init(project, readExtensionFuture));
    }


    // **********************************************************************
    //
    // PRIVATE METHODS
    //
    // **********************************************************************


    private void setPackageRepo(BintrayExtension.PackageConfig packageConfig, @Nullable ReadableDefaultsExtension readExtension) {
        String bintrayRepo = packageConfig.getRepo();
        if (nullOrBlank(bintrayRepo)) {
            if (readExtension != null) {
                bintrayRepo = readExtension.getBintrayRepo();
                if (nullOrBlank(bintrayRepo)) {
                    throw new GradleException("Need to set defaults { bintrayRepo = ... }");
                }

                packageConfig.setRepo((@NonNull String)bintrayRepo);
            }
            else {
                throw new GradleException("Need to set bintray { pkg { repo = ... } }");
            }
        }
        else {
            if (readExtension != null) {
                bintrayRepo = readExtension.getBintrayRepo();
                if (!nullOrBlank(bintrayRepo)) {
                    throw new GradleException("Both defaults { bintrayRepo = ... } and bintray { pkg { repo = ... } } have been set.");
                }
            }
        }
    }


    private void setPackageName(Project project, BintrayExtension.PackageConfig packageConfig, ReadableDefaultsExtension readExtension) {
        String packageName = packageConfig.getName();
        if (nullOrBlank(packageName)) {
            packageName = readExtension.getBintrayPkg();
            packageConfig.setName(nullOrBlank(packageName) ? project.getName() : (@NonNull String)packageName);
        }
        else {
            packageName = readExtension.getBintrayPkg();
            if (!nullOrBlank(packageName)) {
                throw new GradleException("Both defaults { bintrayPkg = ... } and bintray { pkg { name = ... } } have been set.");
            }
        }
    }


    private static boolean nullOrBlank(@Nullable String str) {
        return str == null || str.trim().isEmpty();
    }


    private Map bintrayAttributes(Project project, @Nullable ReadableDefaultsExtension readExtension) {
        val bintrayAttributes = new HashMap<String, Object>();
        if (readExtension != null) {
            final Set<String> bintrayLabels = readExtension.getBintrayLabels();
            if (bintrayLabels != null && bintrayLabels.contains("gradle")) {
                LOG.info("bintrayLabels does includes \'gradle\' so generating \'gradle-plugins\' attribute");
                val filesTree = gradlePluginPropertyFiles(project);
                val pluginIds = filesToPluginIds(filesTree);
                val pluginIdBintrayAttributeValues = new ArrayList<String>();

                for (String it : pluginIds) {
                    String attributeValue = pluginIdToBintrayAttributeValue(project, it);
                    pluginIdBintrayAttributeValues.add(attributeValue);
                }

                bintrayAttributes.put("gradle-plugins", pluginIdBintrayAttributeValues);
            }
            else {
                LOG.info("bintrayLabels does not include \'gradle\' so not generating \'gradle-plugins\' attribute");
            }
        }

        LOG.info("bintrayAttributes: " + bintrayAttributes);
        return bintrayAttributes;
    }


    private String pluginIdToBintrayAttributeValue(Project project, String pluginId) {
        return pluginId + ":" + project.getGroup() + ":" + project.getName();
    }


    /**
     * Initialize the plugin. Guaranteed to run after the project has been evaluated.
     */
    private String init(Project project, Future<@Nullable ReadableDefaultsExtension> readExtensionFuture) {
        val readExtension = readableDefaultsExtension(readExtensionFuture);

        val bintray = (@NonNull BintrayExtension)bintrayExtension(project);

        setCredentials(project);
        bintray.setPublish(true);

        setArtifacts(project, bintray);

        val pkgConfig = bintray.getPkg();

        val desc = project.getDescription();
        if (desc != null) {
            pkgConfig.setDesc(desc);
        }

        if (readExtension != null) {
            setPackageRepo(pkgConfig, readExtension);
            setPackageName(project, pkgConfig, readExtension);

            if (nullOrBlank(pkgConfig.getWebsiteUrl()))
                pkgConfig.setWebsiteUrl(readExtension.getSiteUrl());
            if (nullOrBlank(pkgConfig.getIssueTrackerUrl()))
                pkgConfig.setIssueTrackerUrl(readExtension.getIssuesUrl());
            if (nullOrBlank(pkgConfig.getVcsUrl()))
                pkgConfig.setVcsUrl(readExtension.getVcsReadUrl());
            if (pkgConfig.getLicenses() == null)
                pkgConfig.setLicenses(readExtension.getLicenseKey());

            pkgConfig.setLabels(toStringArray(readExtension.getBintrayLabels()));
        }

        pkgConfig.setPublicDownloadNumbers(true);

        val versionConfig = pkgConfig.getVersion();
        versionConfig.setVcsTag("v" + project.getVersion());
        versionConfig.setAttributes(bintrayAttributes(project, readExtension));

        val gpg = versionConfig.getGpg();
        if (project.hasProperty("gpgPassphrase")) {
            gpg.setSign(true);
            gpg.setPassphrase((String)project.property("gpgPassphrase"));
        }
        else {
            LOG.info("\"gpgPassphrase\" not set on the project, so not signing the Bintray upload");
        }

        return "";
    }


    private static void setCredentials(Project project) {
        val gradle = project.getGradle();
        val taskGraph = gradle.getTaskGraph();
        taskGraph.addTaskExecutionGraphListener(it ->
            it.getAllTasks().
                stream().
                filter(task -> task.getName().equals("bintrayUpload")).
                forEach(bintrayUploadTask -> setUserAndKeyOnUploadTask(bintrayUploadTask))
        );
    }


    private static void setUserAndKeyOnUploadTask(Task bintrayUploadTask) {
        val project = bintrayUploadTask.getProject();
        if (!project.hasProperty("bintrayUser") || !project.hasProperty("bintrayKey")) {
            throw new GradleException("You need to set the \"bintrayUser\" and \"bintrayKey\" properties on the project to upload to BinTray");
        }

        bintrayUploadTask.setProperty("user", project.property("bintrayUser"));
        bintrayUploadTask.setProperty("apiKey", project.property("bintrayKey"));
    }


    private static void setArtifacts(Project project, BintrayExtension bintray) {
        if (noArtifactsDefined(bintray)) {
            MavenPublishPublications.mainPublication(project);
            bintray.setPublications(MavenPublishPublications.PUBLICATION_NAME);
        }
    }


    private static boolean noArtifactsDefined(BintrayExtension bintray) {
        return bintray.getPublications() == null && bintray.getConfigurations() == null && bintray.getFilesSpec() == null;
    }


    private static @Nullable ReadableDefaultsExtension readableDefaultsExtension(Future<@Nullable ReadableDefaultsExtension> readExtensionFuture) {
        try {
            return (ReadableDefaultsExtension)readExtensionFuture.get(50L, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.info("ReadableDefaultsExtension has not been set");
            return null;
        }
    }


    private static String[] toStringArray(@Nullable Set<String> labels) {
        return (labels == null) ? new String[0] : labels.toArray(new String[labels.size()]);
    }


    private static List<String> filesToPluginIds(ConfigurableFileTree filesTree) {
        return stream(filesTree).map(ExtBintrayPlugin::filenameNoExtension).collect(Collectors.<String>toList());
    }


    private static String filenameNoExtension(File file) {
        return Files.getNameWithoutExtension(file.getName());
    }


    private static ConfigurableFileTree gradlePluginPropertyFiles(Project project) {
        val filetreeProps = new HashMap<String, String>();
        filetreeProps.put("dir", "src/main/resources/META-INF/gradle-plugins");
        filetreeProps.put("include", "*.properties");
        return project.fileTree(filetreeProps);
    }


    public static @Nullable BintrayExtension bintrayExtension(Project project) {
        return project.getConvention().findByType(BintrayExtension.class);
    }

}
