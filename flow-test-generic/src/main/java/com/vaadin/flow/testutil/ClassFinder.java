/*
 * Copyright 2000-2020 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.testutil;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertNotNull;

public abstract class ClassFinder {

    private final Logger logger = LoggerFactory.getLogger(ClassFinder.class);

    protected boolean isTestClassPath(String classPath) {
        File file = new File(classPath);
        return "test-classes".equals(file.getName());
    }

    protected static boolean isFunctionalType(Type type) {
        return type.getTypeName().contains("java.util.function");
    }

    /**
     * Lists all class path entries by splitting the class path string.
     * <p>
     * Adapted from ClassPathExplorer.getRawClasspathEntries(), but without
     * filtering.
     *
     * @return List of class path segment strings
     */
    protected static List<String> getRawClasspathEntries() {
        // try to keep the order of the classpath

        String pathSep = System.getProperty("path.separator");
        String classpath = System.getProperty("java.class.path");

        if (classpath.startsWith("\"")) {
            classpath = classpath.substring(1);
        }
        if (classpath.endsWith("\"")) {
            classpath = classpath.substring(0, classpath.length() - 1);
        }

        String[] split = classpath.split(pathSep);
        return Arrays.asList(split);
    }

    /**
     * Lists class names (based on .class files) in a directory (a package path
     * root).
     *
     * @param parentPackage
     *            parent package name or null at root of hierarchy, used by
     *            recursion
     * @param parent
     *            File representing the directory to scan
     * @return collection of fully qualified class names in the directory
     */
    private static Collection<String> findClassesInDirectory(
            String parentPackage, File parent) {
        if (parent.isHidden()
                || parent.getPath().contains(File.separator + ".")) {
            return Collections.emptyList();
        }

        if (parentPackage == null) {
            parentPackage = "";
        } else {
            parentPackage += ".";
        }

        Collection<String> classNames = new ArrayList<>();

        // add all directories recursively
        File[] files = parent.listFiles();
        assertNotNull(files);
        for (File child : files) {
            if (child.isDirectory()) {
                classNames.addAll(findClassesInDirectory(
                        parentPackage + child.getName(), child));
            } else if (child.getName().endsWith(".class")) {
                classNames.add(parentPackage.replace(File.separatorChar, '.')
                        + child.getName().replaceAll("\\.class", ""));
            }
        }

        return classNames;
    }

    /**
     * JARs that will be scanned for classes to test, in addition to classpath
     * directories.
     *
     * @return the compiled pattern
     */
    @SuppressWarnings("WeakerAccess")
    protected Pattern getJarPattern() {
        return Pattern.compile("(.*vaadin.*)|(.*flow.*)\\.jar");
    }

    @SuppressWarnings("WeakerAccess")
    protected Stream<String> getBasePackages() {
        return Stream.of("com.vaadin");
    }

    protected boolean isTestClass(Class<?> cls) {
        if (cls.getEnclosingClass() != null
                && isTestClass(cls.getEnclosingClass())) {
            return true;
        }

        // Test classes with a @Test annotation on some method
        for (Method method : cls.getMethods()) {
            if (method.isAnnotationPresent(Test.class)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Finds the server side classes/interfaces under a class path entry -
     * either a directory or a JAR that matches {@link #getJarPattern()}.
     * <p>
     * Only classes under {@link #getBasePackages} are considered, and those
     * matching {@code excludes} are filtered out.
     */
    protected List<String> findServerClasses(String classpathEntry,
            Collection<Pattern> excludes) throws IOException {
        Collection<String> classes;

        File file = new File(classpathEntry);
        if (file.isDirectory()) {
            classes = findClassesInDirectory(null, file);
        } else if (getJarPattern().matcher(file.getName()).matches()) {
            classes = findClassesInJar(file);
        } else {
            logger.debug("Ignoring " + classpathEntry);
            return Collections.emptyList();
        }
        return classes.stream()
                .filter(className -> getBasePackages().anyMatch(
                        basePackage -> className.startsWith(basePackage + ".")))
                .filter(className -> excludes.stream()
                        .noneMatch(p -> p.matcher(className).matches()))
                .collect(Collectors.toList());
    }

    /**
     * Lists class names (based on .class files) in a JAR file.
     *
     * @param file
     *            a valid JAR file
     * @return collection of fully qualified class names in the JAR
     */
    private Collection<String> findClassesInJar(File file) throws IOException {
        Collection<String> classes = new ArrayList<>();

        try (JarFile jar = new JarFile(file)) {
            Enumeration<JarEntry> e = jar.entries();
            while (e.hasMoreElements()) {
                JarEntry entry = e.nextElement();
                if (entry.getName().endsWith(".class")) {
                    String nameWithoutExtension = entry.getName()
                            .replaceAll("\\.class", "");
                    String className = nameWithoutExtension.replace('/', '.');
                    classes.add(className);
                }
            }
        }
        return classes;
    }
}
