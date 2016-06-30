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

import java.util.Optional;

@SuppressWarnings("WeakerAccess")
public class Utils {

    /**
     * Convert the String into an Optional that only has value if the input was both non-null and non-empty.
     */
    public static Optional<String> opt(String val) {
        return Optional.ofNullable(val).filter(s -> !s.isEmpty());
    }

}
