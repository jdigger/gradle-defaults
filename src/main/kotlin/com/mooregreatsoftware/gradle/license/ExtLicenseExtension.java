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

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.Instant;
import java.time.ZoneId;

/**
 * Configuration options for {@link ExtLicensePlugin}
 */
public class ExtLicenseExtension {
    private final @Nullable ExtLicenseExtension parent;

    private @MonotonicNonNull String copyrightYears;

    static final String NAME = "extLicense";


    /**
     * Creates a new (root) ExtLicenseExtension
     */
    public ExtLicenseExtension() {
        this(null);
    }


    /**
     * Creates a new ExtLicenseExtension
     *
     * @param parent the parent to get values from if not set directly on this instance; null if this is the root
     */
    public ExtLicenseExtension(@Nullable ExtLicenseExtension parent) {
        this.parent = parent;
    }


    public String getCopyrightYears() {
        if (this.copyrightYears != null) return this.copyrightYears;

        if (parent != null) {
            return parent.getCopyrightYears();
        }
        else {
            // TODO Compute from Git history
            return String.valueOf(Instant.now().atZone(ZoneId.systemDefault()).getYear());
        }
    }


    public void setCopyrightYears(String copyrightYears) {
        if (copyrightYears == null) return;
        this.copyrightYears = copyrightYears;
    }


    @Override
    public String toString() {
        return "ExtLicenseExtension{" +
            "parent=" + parent +
            ", copyrightYears='" + copyrightYears + '\'' +
            '}';
    }

}
