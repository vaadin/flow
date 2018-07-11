/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A superclass for serialization testing. The test scans all the classpath and
 * tries to serialize every single class (except ones from whitelist) in the
 * classpath. Subclasses may adjust the whitelist by overriding
 * {@link #getExcludedPatterns()}, {@link #getBasePackages()},
 * {@link #getJarPattern()}
 */

public abstract class ClassesSerializableTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @SuppressWarnings("WeakerAccess")
    protected Stream<String> getExcludedPatterns() {
        return Stream.of(
                "com\\.vaadin\\.flow\\.data\\.validator\\.BeanValidator\\$LazyFactoryInitializer",
                "com\\.vaadin\\.flow\\.internal\\.BeanUtil\\$LazyValidationAvailability",
                ".*\\.slf4j\\..*", ".*\\.testbench\\..*", ".*\\.testutil\\..*",
                ".*\\.demo\\..*", "com\\.vaadin\\..*Util(s)?(\\$\\w+)?$", // Various
                                                                          // utils
                                                                          // with
                                                                          // inner
                                                                          // classes

                "com\\.vaadin\\.flow\\.data\\.provider\\.InMemoryDataProviderHelpers",
                "com\\.vaadin\\.flow\\.dom\\.ElementConstants",
                "com\\.vaadin\\.flow\\.component\\.board\\.internal\\.FunctionCaller",
                "com\\.vaadin\\.flow\\.component\\.grid\\.ColumnGroupHelpers",
                "com\\.vaadin\\.flow\\.component\\.textfield\\.SlotHelpers",
                "com\\.vaadin\\.flow\\.component\\.orderedlayout\\.FlexConstants",
                "com\\.vaadin\\.flow\\.component\\.polymertemplate\\.DefaultTemplateParser",
                "com\\.vaadin\\.flow\\.component\\.PropertyDescriptors(\\$.*)?",
                "com\\.vaadin\\.flow\\.internal\\.JsonSerializer",
                "com\\.vaadin\\.flow\\.internal\\.JsonCodec",
                "com\\.vaadin\\.flow\\.internal\\.UsageStatistics(\\$.*)?",
                "com\\.vaadin\\.flow\\.internal\\.nodefeature\\.NodeFeatureRegistry",
                "com\\.vaadin\\.flow\\.internal\\.nodefeature\\.NodeFeatures",
                "com\\.vaadin\\.flow\\.internal\\.CustomElementNameValidator",
                "com\\.vaadin\\.flow\\.router\\.HighlightActions",
                "com\\.vaadin\\.flow\\.router\\.HighlightConditions",
                "com\\.vaadin\\.flow\\.router\\.ParameterDeserializer",
                "com\\.vaadin\\.flow\\.router\\.NavigationStateBuilder",
                "com\\.vaadin\\.flow\\.router\\.RouteNotFoundError$LazyInit",
                "com\\.vaadin\\.flow\\.internal\\.JavaScriptSemantics",
                "com\\.vaadin\\.flow\\.internal\\.nodefeature\\.NodeProperties",
                "com\\.vaadin\\.flow\\.internal\\.AnnotationReader",
                "com\\.vaadin\\.flow\\.server\\.communication\\.ServerRpcHandler\\$LazyInvocationHandlers",
                "com\\.vaadin\\.flow\\.server\\.VaadinServletRequest",
                "com\\.vaadin\\.flow\\.server\\.VaadinServletResponse",
                "com\\.vaadin\\.flow\\.server\\.startup\\.AnnotationValidator",
                "com\\.vaadin\\.flow\\.server\\.startup\\.ServletDeployer",
                "com\\.vaadin\\.flow\\.server\\.communication.JSR356WebsocketInitializer(\\$.*)?",
                "com\\.vaadin\\.flow\\.server\\.BootstrapHandler(\\$.*)?",
                "com\\.vaadin\\.flow\\.server\\.BootstrapPageResponse",
                "com\\.vaadin\\.flow\\.server\\.InlineTargets",
                "com\\.vaadin\\.flow\\.server\\.communication\\.PushHandler(\\$.*)?",
                "com\\.vaadin\\.flow\\.server\\.communication\\.PushRequestHandler(\\$.*)?",
                "com\\.vaadin\\.flow\\.templatemodel\\.PathLookup",
                "com\\.vaadin\\.flow\\.server\\.startup\\.ErrorNavigationTargetInitializer",
                "com\\.vaadin\\.flow\\.server\\.startup\\.ServletVerifier",
                "com\\.vaadin\\.flow\\.server\\.startup\\.RouteRegistryInitializer",
                "com\\.vaadin\\.flow\\.server\\.VaadinResponse",
                "com\\.vaadin\\.flow\\.component\\.Key",
                "com\\.vaadin\\.flow\\.server\\.VaadinRequest",
                "com\\.vaadin\\.flow\\.router\\.RouteNotFoundError\\$LazyInit",
                "com\\.vaadin\\.flow\\.component\\.polymertemplate\\.TemplateDataAnalyzer\\$.*",
                "com\\.vaadin\\.flow\\.component\\.HtmlComponent", // De-facto
                                                                   // abstract
                                                                   // class
                "com\\.vaadin\\.flow\\.component\\.HtmlContainer", // De-facto
                                                                   // abstract
                                                                   // class
                "com\\.vaadin\\.flow\\.component\\.polymertemplate\\.TemplateInitializer(\\$.*)?",
                "com\\.vaadin\\.flow\\.component\\.polymertemplate\\.TemplateParser(\\$.*)?",
                "com\\.vaadin\\.flow\\.dom\\.impl\\.ThemeListImpl\\$ThemeListIterator",
                "com\\.vaadin\\.flow\\.templatemodel\\.PropertyMapBuilder(\\$.*)?",
                "com\\.vaadin\\.flow\\.internal\\.ReflectionCache",
                "com\\.vaadin\\.flow\\.component\\.internal\\.ComponentMetaData(\\$.*)?",
                "com\\.vaadin\\.flow\\.component\\.polymertemplate\\.TemplateDataAnalyzer",
                "com\\.vaadin\\.flow\\.dom\\.ElementFactory",
                "com\\.vaadin\\.flow\\.dom\\.NodeVisitor",
                "com\\.vaadin\\.flow\\.internal\\.nodefeature\\.NodeList(\\$.*)?",
                "com\\.vaadin\\.flow\\.templatemodel\\.PropertyFilter",
                "com\\.vaadin\\.flow\\.internal\\.ReflectTools(\\$.*)?",
                "com\\.vaadin\\.flow\\.server\\.FutureAccess",
                "com\\.vaadin\\.flow\\.internal\\.nodefeature\\.ElementPropertyMap\\$PutResult",

                // Various test classes
                ".*\\.test(s)?\\..*", ".*Test.*",
                "com\\.vaadin\\.flow\\.server\\.MockVaadinServletService",
                "com\\.vaadin\\.flow\\.server\\.MockServletServiceSessionSetup",
                "com\\.vaadin\\.flow\\.server\\.MockServletConfig",
                "com\\.vaadin\\.flow\\.server\\.MockServletContext",
                "com\\.vaadin\\.flow\\.templatemodel\\.Bean",
                "com\\.vaadin\\.flow\\.internal\\.HasCurrentService",
                "com\\.vaadin\\.flow\\.component\\.ValueChangeMonitor",
                "com\\.vaadin\\.flow\\.templatemodel\\.BeanContainingBeans(\\$.*)?");
    }

    /**
     * Performs actual serialization/deserialization
     *
     * @param <T>
     *            the type of the instance
     * @param instance
     *            the instance
     * @return the copy of the source object
     * @throws Throwable
     *             if something goes wrong.
     */
    @SuppressWarnings({ "UnusedReturnValue", "WeakerAccess" })
    public <T> T serializeAndDeserialize(T instance) throws Throwable {
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bs);
        out.writeObject(instance);
        byte[] data = bs.toByteArray();
        ObjectInputStream in = new ObjectInputStream(
                new ByteArrayInputStream(data));

        @SuppressWarnings("unchecked")
        T readObject = (T) in.readObject();

        return readObject;
    }

    /**
     * The method is called right after a class instantiation and might be
     * overriden by subclasses to reset thread local values (ex. current UI).
     *
     * @see #setupThreadLocals
     */
    @SuppressWarnings("WeakerAccess")
    protected void resetThreadLocals() {
    }

    /**
     * The method is called right a class instantiation and might be overriden
     * by subclasses to install some necessary thread local values (ex. current
     * UI).
     *
     * @see #resetThreadLocals
     */
    @SuppressWarnings("WeakerAccess")
    protected void setupThreadLocals() {
    }

    private static boolean isFunctionalType(Type type) {
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
    private static List<String> getRawClasspathEntries() {
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

    /**
     * Tests that all the relevant classes and interfaces under
     * {@link #getBasePackages} implement Serializable.
     *
     * @throws Throwable
     *             serialization goes wrong
     */
    @Test
    public void classesSerializable() throws Throwable {
        List<String> rawClasspathEntries = getRawClasspathEntries();

        List<String> classes = new ArrayList<>();
        List<Pattern> excludes = getExcludedPatterns().map(Pattern::compile)
                .collect(Collectors.toList());
        for (String location : rawClasspathEntries) {
            classes.addAll(findServerClasses(location, excludes));
        }

        ArrayList<Field> nonSerializableFunctionFields = new ArrayList<>();

        List<Class<?>> nonSerializableClasses = new ArrayList<>();
        for (String className : classes) {
            Class<?> cls;
            try {
                cls = Class.forName(className);
            } catch (ClassNotFoundException e) {
                logger.debug("Ignoring {} since it's not found", className);
                continue;
            }
            // Don't add classes that have a @Ignore annotation on the class
            if (isTestClass(cls)) {
                continue;
            }

            // report fields that use lambda types that won't be serializable
            // (also in synthetic classes)
            Stream.of(cls.getDeclaredFields())
                    .filter(field -> isFunctionalType(field.getGenericType()))
                    .filter(field -> !Modifier.isStatic(field.getModifiers()))
                    .forEach(nonSerializableFunctionFields::add);

            // skip annotations and synthetic classes
            if (cls.isAnnotation() || cls.isSynthetic()) {
                continue;
            }

            if (!cls.isInterface()
                    && !Modifier.isAbstract(cls.getModifiers())) {
                serializeAndDeserialize(cls);
            }

            // report non-serializable classes and interfaces
            if (!Serializable.class.isAssignableFrom(cls)) {
                nonSerializableClasses.add(cls);
                // TODO easier to read when testing
                // System.err.println(cls);
            }
        }

        // useful failure message including all non-serializable classes and
        // interfaces
        if (!nonSerializableClasses.isEmpty()) {
            failSerializableClasses(nonSerializableClasses);
        }

        if (!nonSerializableFunctionFields.isEmpty()) {
            failSerializableFields(nonSerializableFunctionFields);
        }
    }

    private void serializeAndDeserialize(Class<?> clazz) {
        try {
            Optional<Constructor<?>> defaultCtor = Stream
                    .of(clazz.getDeclaredConstructors())
                    .filter(ctor -> ctor.getParameterCount() == 0).findFirst();
            if (!defaultCtor.isPresent()) {
                return;
            }
            defaultCtor.get().setAccessible(true);
            setupThreadLocals();
            Object instance;
            try {
                instance = defaultCtor.get().newInstance();
            } finally {
                resetThreadLocals();
            }
            serializeAndDeserialize(instance);
        } catch (Throwable e) {
            throw new AssertionError(clazz.getName(), e);
        }
    }

    private void failSerializableFields(
            List<Field> nonSerializableFunctionFields) {
        String nonSerializableString = nonSerializableFunctionFields.stream()
                .map(field -> String.format("%s.%s",
                        field.getDeclaringClass().getName(), field.getName()))
                .collect(Collectors.joining(", "));

        fail("Fields with functional types that are not serializable: "
                + nonSerializableString);
    }

    private void failSerializableClasses(
            List<Class<?>> nonSerializableClasses) {
        StringBuilder nonSerializableString = new StringBuilder();
        for (Class<?> c : nonSerializableClasses) {
            nonSerializableString.append(",\n").append(c.getName());
            if (c.isAnonymousClass()) {
                nonSerializableString.append("(super: ")
                        .append(c.getSuperclass().getName())
                        .append(", interfaces: ");
                for (Class<?> i : c.getInterfaces()) {
                    nonSerializableString.append(i.getName()).append(",");
                }
                nonSerializableString.append(")");
            }
        }
        fail("Serializable not implemented by the following classes and interfaces: "
                + nonSerializableString);

    }

    private boolean isTestClass(Class<?> cls) {
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
     * matching {@link #getExcludedPatterns()} are filtered out.
     */
    private List<String> findServerClasses(String classpathEntry,
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
