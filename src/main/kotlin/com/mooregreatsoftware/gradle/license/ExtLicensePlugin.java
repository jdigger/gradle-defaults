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
package com.mooregreatsoftware.gradle.license;

import com.mooregreatsoftware.gradle.defaults.DefaultsExtensionKt;
import groovy.lang.GroovyObject;
import lombok.val;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.internal.plugins.PluginApplicationException;
import org.gradle.api.plugins.Convention;
import org.gradle.api.plugins.ExtraPropertiesExtension;

import java.lang.reflect.InvocationTargetException;

import static org.apache.commons.beanutils.BeanUtils.setProperty;

@SuppressWarnings("SameParameterValue")
public class ExtLicensePlugin implements Plugin<Project> {
    public static final String PLUGIN_ID = "com.mooregreatsoftware.license";


    @Override
    public void apply(Project project) {
        project.getPlugins().apply("com.github.hierynomus.license");
        val licenseExt = project.getConvention().getByName("license");
        try {
            setProperty(licenseExt, "header", project.getRootProject().file("gradle/HEADER"));
            setProperty(licenseExt, "strictCheck", true);
            setProperty(licenseExt, "isUseDefaultMappings", true);
            setMapping(licenseExt, "groovy", "SLASHSTAR_STYLE");
            setMapping(licenseExt, "java", "SLASHSTAR_STYLE");
            setMapping(licenseExt, "scala", "SLASHSTAR_STYLE");
            setMapping(licenseExt, "kt", "SLASHSTAR_STYLE");
            setMapping(licenseExt, "css", "SLASHSTAR_STYLE");
        }
        catch (IllegalAccessException | InvocationTargetException e) {
            throw new PluginApplicationException(PLUGIN_ID, e);
        }


        project.afterEvaluate(p ->
            configLicenseExtension(DefaultsExtensionKt.defaultsExtension(project).getCopyrightYears(), licenseExt)
        );
    }


    private void configLicenseExtension(@Nullable String copyrightYears, Object licenseExt) {
        addExclude(licenseExt, "**/*.properties");

        if (copyrightYears != null) ext(licenseExt).set("year", copyrightYears);
    }


    private static void setMapping(Object licenseExt, String extension, String styleName) {
        try {
            val mappingMethod = licenseExt.getClass().getMethod("mapping", String.class, String.class);
            mappingMethod.invoke(licenseExt, extension, styleName);
        }
        catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new PluginApplicationException(PLUGIN_ID, e);
        }
    }


    private static void addExclude(Object licenseExt, String exclusion) {
        try {
            licenseExt.getClass().getMethod("exclude", String.class).invoke(licenseExt, exclusion);
        }
        catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new PluginApplicationException(PLUGIN_ID, e);
        }
    }


    private static ExtraPropertiesExtension ext(Object licenseExt) {
        val metaClass = ((GroovyObject)licenseExt).getMetaClass();
        val conExtPropsProp = metaClass.getMetaProperty("extensions");
        val convExtensions = (Convention)conExtPropsProp.getProperty(licenseExt);
        return convExtensions.getExtraProperties();
    }

}
