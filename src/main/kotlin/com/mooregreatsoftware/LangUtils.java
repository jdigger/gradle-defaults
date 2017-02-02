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
package com.mooregreatsoftware;

import java.util.concurrent.Callable;

public final class LangUtils {

    /**
     * Remove checked-ness from the exception. The same exception is returned
     * (checked or unchecked), but this removes the compiler's checks.
     */
    @SuppressWarnings("RedundantTypeArguments")
    public static <T extends Throwable> RuntimeException softened(T exp) {
        return LangUtils.<RuntimeException>uncheck(exp);
    }


    @SuppressWarnings({"unchecked", "ConstantConditions"})
    private static <T extends Throwable> T uncheck(Throwable throwable) throws T {
        throw (T)throwable;
    }


    /**
     * A functional interface for something that returns nothing but may throw a checked exception.
     */
    @FunctionalInterface
    public interface CheckedRunnable {
        void run() throws Exception;
    }


    /**
     * Tries to run the given function, but will "soften" any exception that is thrown.
     *
     * @see #softened(Throwable)
     */
    public static void tryRun(CheckedRunnable checkedRunnable) {
        try {
            checkedRunnable.run();
        }
        catch (Exception e) {
            throw softened(e);
        }
    }


    /**
     * Tries to run the given function and return its value, but will "soften" any exception that is thrown.
     *
     * @see #softened(Throwable)
     */
    public static <T> T tryGet(Callable<T> callable) {
        try {
            return callable.call();
        }
        catch (Exception e) {
            throw softened(e);
        }
    }

}
