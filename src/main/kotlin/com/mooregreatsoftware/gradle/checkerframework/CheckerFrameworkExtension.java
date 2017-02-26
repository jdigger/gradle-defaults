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
package com.mooregreatsoftware.gradle.checkerframework;

import lombok.Data;

/**
 * Configuration options for {@link CheckerFrameworkPlugin}
 */
@Data
@SuppressWarnings("WeakerAccess")
public class CheckerFrameworkExtension {
    /**
     * The version of the Checker Framework to use.
     */
    private String version = DEFAULT_CHECKER_VERSION;


    /**
     * The name to register this under as a Gradle extension.
     */
    public static final String NAME = "checkerFramework";

    // tag::default_version[]
    public static final String DEFAULT_CHECKER_VERSION = "2.1.8";
    // end::default_version[]

}
