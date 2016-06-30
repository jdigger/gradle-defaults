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
package com.mooregreatsoftware.gradle.defaults.config;

import groovy.lang.GroovyObject;
import groovy.lang.MetaClass;
import groovy.lang.MetaProperty;
import nl.javadude.gradle.plugins.license.License;
import nl.javadude.gradle.plugins.license.LicenseExtension;
import org.gradle.api.Project;
import org.gradle.api.plugins.Convention;
import org.gradle.api.plugins.ExtraPropertiesExtension;

import java.util.function.Supplier;

@SuppressWarnings("WeakerAccess")
public class LicenseConfig extends AbstractConfig {
    public LicenseConfig(Project project) {
        super(project);
    }


    public void config(Supplier<String> copyrightYears) {
        plugins().apply("license");
        final LicenseExtension licenseExt = project.getConvention().getByType(LicenseExtension.class);
        licenseExt.setHeader(project.getRootProject().file("gradle/HEADER"));
        licenseExt.setStrictCheck(true);
        licenseExt.setUseDefaultMappings(false);
        licenseExt.mapping("groovy", "SLASHSTAR_STYLE");
        licenseExt.mapping("java", "SLASHSTAR_STYLE");

        project.afterEvaluate(prj -> {
            @SuppressWarnings("RedundantCast") final MetaClass metaClass = ((GroovyObject)licenseExt).getMetaClass();
            final MetaProperty conExtPropsProp = metaClass.getMetaProperty("extensions");
            final Convention convExtensions = (Convention)conExtPropsProp.getProperty(licenseExt);
            final ExtraPropertiesExtension extensions = convExtensions.getExtraProperties();
            extensions.set("year", copyrightYears.get());
        });

        tasks().whenTaskAdded(task -> {
            if (task instanceof License) {
                ((License)task).exclude("**/*.properties");
            }
        });
    }
}
