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
package com.mooregreatsoftware.gradle;

import org.gradle.api.Project;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.logging.LogLevel;
import org.slf4j.ILoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;

import static com.mooregreatsoftware.gradle.util.LangUtils.tryRun;

public class Projects {

    /**
     * Evaluate the given project.
     *
     * @return the given project
     */
    public static Project evaluate(Project project) {
        return ((ProjectInternal)project).evaluate();
    }


    /**
     * Change the LogLevel used by the static instance of LoggerFactory.
     * <p>
     * Note that this affects a static factory, so changes will be reflected by anything that uses the same classloader.
     */
    public static void setLogLevel(LogLevel logLevel) {
        final ILoggerFactory loggerFactory = StaticLoggerBinder.getSingleton().getLoggerFactory();

        tryRun(() -> {
            final Class<? extends ILoggerFactory> loggerFactoryClass = loggerFactory.getClass();
            loggerFactoryClass.getMethod("setLevel", LogLevel.class).invoke(loggerFactory, logLevel);
            final Object eventListener = loggerFactoryClass.getMethod("getOutputEventListener").invoke(loggerFactory);
            if (eventListener == null) throw new IllegalStateException("eventListener == null for " + loggerFactory);
            eventListener.getClass().getMethod("configure", LogLevel.class).invoke(eventListener, logLevel);
        });
    }

}
