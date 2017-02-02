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
package com.mooregreatsoftware.gradle.scala;

import com.mooregreatsoftware.gradle.lang.AbstractLanguagePlugin;

import static org.gradle.api.plugins.scala.ScalaPlugin.SCALA_DOC_TASK_NAME;

public class ExtScalaPlugin extends AbstractLanguagePlugin {
    public static final String PLUGIN_ID = "com.mooregreatsoftware.scala";


    @Override
    protected String pluginId() {
        return PLUGIN_ID;
    }


    @Override
    protected String basePluginId() {
        return "scala";
    }


    @Override
    protected String docTaskName() {
        return SCALA_DOC_TASK_NAME;
    }

}
