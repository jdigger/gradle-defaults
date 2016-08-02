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

import lombok.Value;
import lombok.val;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.gradle.api.Project;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

@SuppressWarnings({"WeakerAccess", "RedundantCast"})
public class Utils {

    /**
     * Convert the String into an Optional that only has value if the input was both non-null and non-empty.
     */
    public static Optional<String> opt(@Nullable String val) {
        return Optional.ofNullable(val).filter(s -> !s.isEmpty());
    }


    /**
     * Returns true if the given string is non-null and has at least one non-whitespace character; false otherwise.
     */
    public static boolean isNotEmpty(@Nullable String str) {
        return !(str == null || str.trim().isEmpty());
    }


    /**
     * Gets a value from the project's "extra properties", creating and/or transforming it.
     * <p>
     * If the key does not currently exist in "extra properties", the <code>creator</code> is called to create a new
     * instance.
     * <p>
     * Regardless of if the value was found or newly created, the <code>transformer</code> is always called upon it
     * and the result put back into "extra properties" before being returned from this method. Therefore it is
     * important that <em><code>transformer</code> is idempotent</em>.
     *
     * @param project     the project containing the "extra properties"
     * @param keyName     the property name
     * @param transformer the IDEMPOTENT value transformer
     * @param creator     the supplier of new value instances
     * @param <T>         the type of the value
     * @see #setFromExt(Project, String, Supplier)
     */
    @SuppressWarnings("unchecked")
    public static <T> T fromExt(Project project, String keyName, Function<T, T> transformer, Supplier<T> creator) {
        val ext = project.getExtensions().getExtraProperties();
        val value = (@NonNull T)transformer.apply(ext.has(keyName) ? (T)ext.get(keyName) : creator.get());
        ext.set(keyName, value);
        return value;
    }


    /**
     * Gets a {@link Set} of values from the project's "extra properties", ensuring that the values in
     * <code>itemsToAddSupplier</code> are a part of it.
     *
     * @param project            the project containing the "extra properties"
     * @param keyName            the property name
     * @param itemsToAddSupplier the {@link Supplier} of values to make sure are in the {@link Set}
     * @param <T>                the type of the values
     * @see #fromExt(Project, String, Function, Supplier)
     */
    @SuppressWarnings("Convert2MethodRef")
    public static <T> Set<T> setFromExt(Project project, String keyName, Supplier<Collection<T>> itemsToAddSupplier) {
        return fromExt(project, keyName, set -> {
            set.addAll(itemsToAddSupplier.get());
            return set;
        }, () -> new HashSet<T>());
    }


    /**
     * Gets a {@link Set} of values from the project's "extra properties", ensuring that the values in
     * <code>itemsToAddSupplier</code> are a part of it.
     *
     * @param project    the project containing the "extra properties"
     * @param keyName    the property name
     * @param itemsToAdd the values to make sure are in the {@link Set}
     * @param <T>        the type of the values
     * @see #fromExt(Project, String, Function, Supplier)
     */
    @SuppressWarnings("Convert2MethodRef")
    public static <T> Set<T> setFromExt(Project project, String keyName, Collection<T> itemsToAdd) {
        return fromExt(project, keyName, set -> {
            set.addAll(itemsToAdd);
            return set;
        }, () -> new HashSet<T>());
    }


    /**
     * Delete a directory, akin to "rm -rf".
     */
    public static void deleteDir(Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }


            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
                if (e == null) {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
                else {
                    // directory iteration failed
                    throw e;
                }
            }
        });
    }


    public static JdkVersion jdkVersion() {
        val javaVer = System.getProperty("java.version");
        val matcher = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)_(\\d+)$").matcher(javaVer);
        if (matcher.matches()) {
            val major = Integer.parseInt((@NonNull String)matcher.group(1));
            val minor = Integer.parseInt((@NonNull String)matcher.group(2));
            val patch = Integer.parseInt((@NonNull String)matcher.group(3));
            val update = Integer.parseInt((@NonNull String)matcher.group(4));

            return new JdkVersion(major, minor, patch, update);
        }
        else {
            throw new IllegalArgumentException("Could not parse \"" + javaVer + "\"");
        }
    }


    public static boolean isBuggyJavac() {
        val jdkVersion = jdkVersion();
        return jdkVersion.minor() == 8 && jdkVersion.update() < 45;
    }


    /**
     * Compares two {@link Map}s of {@link Comparable} items. If one is null, that sorts lower. If one's size is
     * smaller, that sorts lower. Otherwise the keys are compared. (See {@link #compareSets(Set, Set)}.)
     * If the keys are the same, the first difference in the values found when searching the sorted keys is used for
     * the comparision.
     *
     * @see Comparable
     */
    public static <K extends Comparable<K>, V extends Comparable<V>> int compareMaps(@Nullable Map<K, V> m1,
                                                                                     @Nullable Map<K, V> m2) {
        if (m1 == null) {
            return m2 == null ? 0 : -1;
        }
        else if (m2 == null) return 1;

        if (m1.size() == m2.size()) {
            if (m1.isEmpty()) return 0;
            val m1Keys = m1.keySet();
            val m2Keys = m2.keySet();
            val keysComp = compareSets(m1Keys, m2Keys);
            if (keysComp == 0) {
                val keyIter = m1Keys.stream().sorted().iterator();
                while (keyIter.hasNext()) {
                    val key = keyIter.next();
                    val m1Val = (@NonNull V)m1.get(key);
                    val m2Val = (@NonNull V)m2.get(key);
                    val valComp = m1Val.compareTo(m2Val);
                    if (valComp != 0) return valComp;
                }
                return 0;
            }
            else return keysComp;
        }
        else return m1.size() > m2.size() ? 1 : -1;
    }


    /**
     * Compares two {@link Set}s of {@link Comparable} items. If one is null, that sorts lower. If one's size is
     * smaller, that sorts lower. Otherwise the first non-equal value after they are sorted is used for the comparision.
     *
     * @see Comparable
     */
    public static <U extends Comparable<U>> int compareSets(@Nullable Set<U> s1, @Nullable Set<U> s2) {
        if (s1 == null) {
            return s2 == null ? 0 : -1;
        }
        else if (s2 == null) return 1;

        if (s1.size() == s2.size()) {
            if (s1.isEmpty()) return 0;
            val s1SortedIter = s1.stream().sorted().iterator();
            val s2SortedIter = s2.stream().sorted().iterator();
            while (s1SortedIter.hasNext()) { // both iter's hasNext() is guaranteed to be the same since same size
                val s1Val = s1SortedIter.next();
                val s2Val = s2SortedIter.next();
                val compVal = s1Val.compareTo(s2Val);
                if (compVal != 0) return compVal;
            }
            return 0;
        }
        else return s1.size() > s2.size() ? 1 : -1;
    }


    @Value
    public static class JdkVersion {
        int major;
        int minor;
        int patch;
        int update;
    }

}
