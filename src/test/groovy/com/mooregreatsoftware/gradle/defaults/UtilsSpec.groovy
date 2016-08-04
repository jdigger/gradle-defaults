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
package com.mooregreatsoftware.gradle.defaults

import spock.lang.Specification

import static com.mooregreatsoftware.gradle.defaults.UtilsKt.jdkVersion

class UtilsSpec extends Specification {

    def "IsJava8Compatible"() {
        final origVersion = System.getProperty("java.version")

        when:
        System.setProperty("java.version", "1.8.0_60")

        then:
        jdkVersion() == new JdkVersion(1, 8, 0, 60)

        when:
        System.setProperty("java.version", "1.8.0_31")

        then:
        jdkVersion() == new JdkVersion(1, 8, 0, 31)

        cleanup:
        System.setProperty("java.version", origVersion)
    }

}
