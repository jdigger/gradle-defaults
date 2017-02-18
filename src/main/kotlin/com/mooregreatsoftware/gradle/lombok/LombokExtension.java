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
package com.mooregreatsoftware.gradle.lombok;

import lombok.Data;

/**
 * Configuration options for {@link LombokPlugin}
 */
@Data
public class LombokExtension {
    private String version;

    static final String NAME = "lombok";

    public static final String DEFAULT_LOMBOK_VERSION = "1.16.12";


    public LombokExtension() {
        this.version = DEFAULT_LOMBOK_VERSION;
    }

}
