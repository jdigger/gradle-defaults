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
import com.mooregreatsoftware.gradle.defaults.DefaultsExtensionKt;
import com.mooregreatsoftware.gradle.maven.MavenPublishPublications;
import com.mooregreatsoftware.gradle.util.LangUtils;
import lombok.val;
import org.apache.commons.beanutils.BeanUtilsBean;
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
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.mooregreatsoftware.gradle.util.ProjectUtilsKt.getCustomProperty;
import static com.mooregreatsoftware.gradle.util.ProjectUtilsKt.hasCustomProperty;
import static com.mooregreatsoftware.gradle.util.UtilsKt.stream;

/**
 * Applies the BintrayPlugin from JFrog and sets up reasonable defaults that are coordinated with other
 * "known" plugins (such as for POM.xml data).
 */
// TODO Remove dependency on defaults extension
@SuppressWarnings({"RedundantCast", "RedundantTypeArguments", "Convert2MethodRef"})
public class ExtBintrayPlugin implements Plugin<Project> {
    private static final Logger LOG = LoggerFactory.getLogger(ExtBintrayPlugin.class);

    public static final String PLUGIN_ID = "com.mooregreatsoftware.bintray";

    public static final String BINTRAY_PKG_KEY = "com.mooregreatsoftware.property.bintray.pkg";
    public static final String BINTRAY_REPO_KEY = "com.mooregreatsoftware.property.bintray.repo";
    public static final String BINTRAY_LABELS_KEY = "com.mooregreatsoftware.property.bintray.labels";

    public static final String ORG_ID_KEY = "com.mooregreatsoftware.property.orgId";
    public static final String SITE_URL_KEY = "com.mooregreatsoftware.property.siteUrl";
    public static final String ISSUES_URL_KEY = "com.mooregreatsoftware.property.issuesUrl";

    public static final String LICENSE_KEY = "com.mooregreatsoftware.property.license.key";

    public static final String VCS_READ_URL_KEY = "com.mooregreatsoftware.property.vcs.readUrl";


    @Override
    public void apply(Project project) {
        DefaultsExtensionKt.defaultsExtension(project);
        LOG.info("Applying com.jfrog.bintray to {}", project);
        project.getPlugins().apply("com.jfrog.bintray");

        project.afterEvaluate(proj -> init(proj));
    }


    // **********************************************************************
    //
    // PRIVATE METHODS
    //
    // **********************************************************************


    private void setPackageRepo(Project project, Object packageConfigExt) {
        val bintrayRepo = getProperty(packageConfigExt, "repo");
        if (nullOrBlank(bintrayRepo)) {
            if (hasCustomProperty(project, BINTRAY_REPO_KEY)) {
                setProperty(packageConfigExt, "repo", (@NonNull String)getCustomProperty(project, BINTRAY_REPO_KEY));
            }
            else {
                throw new GradleException("Need to set defaults { bintrayRepo = ... }");
            }
        }
        else {
            if (hasCustomProperty(project, BINTRAY_REPO_KEY)) {
                throw new GradleException("Both defaults { bintrayRepo = ... } and bintray { pkg { repo = ... } } have been set.");
            }
        }
    }


    private void setPackageName(Project project, Object packageConfigExt) {
        val packageName = getProperty(packageConfigExt, "name");
        val hasCustomBintrayPkg = hasCustomProperty(project, BINTRAY_PKG_KEY);
        if (nullOrBlank(packageName)) {
            if (hasCustomBintrayPkg)
                setProperty(packageConfigExt, "name", (@NonNull String)getCustomProperty(project, BINTRAY_PKG_KEY));
            else
                setProperty(packageConfigExt, "name", project.getName());
        }
        else {
            if (hasCustomBintrayPkg) {
                throw new GradleException("Both defaults { bintrayPkg = ... } and bintray { pkg { name = ... } } have been set.");
            }
        }
    }


    private static boolean nullOrBlank(@Nullable Object value) {
        return value == null || ((value instanceof String) && ((String)value).trim().isEmpty());
    }


    private Map bintrayAttributes(Project project) {
        val bintrayAttributes = new HashMap<String, Object>();
        val labels = defaultLabels(project);
        final @Nullable Collection<String> bintrayLabels = labels == null ? null : Arrays.asList(labels);
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

        LOG.info("bintrayAttributes: " + bintrayAttributes);
        return bintrayAttributes;
    }


    private String pluginIdToBintrayAttributeValue(Project project, String pluginId) {
        return pluginId + ":" + project.getGroup() + ":" + project.getName();
    }


    /**
     * Initialize the plugin. Guaranteed to run after the project has been evaluated.
     */
    private void init(Project project) {
        val bintray = (@NonNull Object)bintrayExtension(project);

        setCredentials(project);
        setProperty(bintray, "publish", true);

        setArtifacts(project, bintray);

        val pkgConfig = (@NonNull Object)getProperty(bintray, "pkg");

        val desc = project.getDescription();
        if (desc != null) {
            setProperty(pkgConfig, "desc", desc);
        }

        setPackageRepo(project, pkgConfig);
        setPackageName(project, pkgConfig);

        setDefaultValueIfPropEmpty(pkgConfig, "websiteUrl", () -> defaultWebsiteUrl(project));
        setDefaultValueIfPropEmpty(pkgConfig, "issueTrackerUrl", () -> defaultIssueTrackerUrl(project, pkgConfig));
        setDefaultValueIfPropEmpty(pkgConfig, "vcsUrl", () -> defaultVcsUrl(project, pkgConfig));
        setDefaultValueIfPropEmpty(pkgConfig, "licenses", () -> defaultLicenses(project));
        setDefaultValueIfPropEmpty(pkgConfig, "labels", () -> defaultLabels(project));

        setProperty(pkgConfig, "publicDownloadNumbers", true);

        val versionConfig = (@NonNull Object)getProperty(pkgConfig, "version");
        setProperty(versionConfig, "vcsTag", "v" + project.getVersion());
        setProperty(versionConfig, "attributes", bintrayAttributes(project));

        val gpg = (@NonNull Object)getProperty(versionConfig, "gpg");
        if (project.hasProperty("gpgPassphrase")) {
            setProperty(gpg, "sign", true);
            setProperty(gpg, "passphrase", ((String)project.property("gpgPassphrase")));
        }
        else {
            LOG.info("\"gpgPassphrase\" not set on the project, so not signing the Bintray upload");
        }
    }


    private String defaultWebsiteUrl(Project project) {
        return defaultValue(project, "websiteUrl", SITE_URL_KEY,
            () -> "https://github.com/" + orgId(project) + "/" + project.getName());
    }


    private String defaultIssueTrackerUrl(Project project, Object config) {
        return defaultValue(project, "issuesTrackerUrl", ISSUES_URL_KEY,
            () -> getProperty(config, "websiteUrl") + "/issues");
    }


    private String defaultVcsUrl(Project project, Object config) {
        return defaultValue(project, "vcsUrl", VCS_READ_URL_KEY,
            () -> getProperty(config, "websiteUrl") + ".git");
    }


    private String[] defaultLicenses(Project project) {
        return defaultValue(project, "licenses", LICENSE_KEY,
            () -> new String[]{"Apache-2.0"});
    }


    private String @Nullable [] defaultLabels(Project project) {
        val hasCustomProperty = hasCustomProperty(project, BINTRAY_LABELS_KEY);
        if (hasCustomProperty) return toStringArray(getCustomProperty(project, BINTRAY_LABELS_KEY));
        return null;
    }


    @SuppressWarnings("unchecked")
    private <T> T defaultValue(Project project, String propertyName, String defaultKey, Supplier<T> defaultComputer) {
        val hasCustomProperty = hasCustomProperty(project, defaultKey);
        if (hasCustomProperty) return (@NonNull T)getCustomProperty(project, defaultKey);
        val computedValue = defaultComputer.get();
        LOG.info("Computed the {} to be: {}", propertyName, (@NonNull Object)computedValue);
        return computedValue;
    }


    private String orgId(Project project) {
        val hasCustomProperty = hasCustomProperty(project, ORG_ID_KEY);
        if (hasCustomProperty) return (@NonNull String)getCustomProperty(project, ORG_ID_KEY);
        throw new IllegalStateException("\"orgId\" is not set for on \"" + project.getName() + "\"");
    }


    private static @Nullable Object getProperty(Object object, String name) {
        try {
            return BeanUtilsBean.getInstance().getPropertyUtils().getSimpleProperty(object, name);
        }
        catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw LangUtils.softened(e);
        }
    }


    private static void setProperty(Object bean, String propName, @Nullable Object value) {
        try {
            BeanUtilsBean.getInstance().getPropertyUtils().setSimpleProperty(bean, propName, value);
        }
        catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw LangUtils.softened(e);
        }
    }


    @SuppressWarnings("unchecked")
    private static <S> void setDefaultValueIfPropEmpty(Object config, String propName, Supplier<@Nullable S> defaultSupplier) {
        val currentValue = getProperty(config, propName);
        if (nullOrBlank(currentValue)) {
            val defaultValue = defaultSupplier.get();
            setProperty(config, propName, defaultValue);
        }
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


    private static void setArtifacts(Project project, Object bintrayExt) {
        if (noArtifactsDefined(bintrayExt)) {
            MavenPublishPublications.mainPublication(project);
            setProperty(bintrayExt, "publications", new String[]{MavenPublishPublications.PUBLICATION_NAME});
        }
    }


    private static boolean noArtifactsDefined(Object bintrayExt) {
        return getProperty(bintrayExt, "publications") == null &&
            getProperty(bintrayExt, "configurations") == null &&
            getProperty(bintrayExt, "filesSpec") == null;
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


    public static @Nullable Object bintrayExtension(Project project) {
        return project.getConvention().findByName("bintray");
    }

}
