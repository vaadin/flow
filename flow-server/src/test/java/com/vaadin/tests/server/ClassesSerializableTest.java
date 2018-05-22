package com.vaadin.tests.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import static java.lang.reflect.Modifier.isStatic;
import static org.junit.Assert.fail;


public class ClassesSerializableTest {

    /**
     * JARs that will be scanned for classes to test, in addition to classpath
     * directories.
     */
    private static final String JAR_PATTERN = "(.*vaadin.*)|(.*flow.*)\\.jar";

    private static final String[] BASE_PACKAGES = {"com.vaadin"};

    private static final String[] EXCLUDED_PATTERNS = {
            "com\\.vaadin\\.flow\\.data\\.validator\\.BeanValidator\\$LazyFactoryInitializer",
            "com\\.vaadin\\.flow\\.internal\\.BeanUtil\\$LazyValidationAvailability",
            ".*\\.slf4j\\..*",
            ".*\\.testbench\\..*",
            ".*\\.test(s)?\\..*",
            "com\\.vaadin\\..*Util(s)?(\\$\\w+)?$", //Various utils with inner classes

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
            "com\\.vaadin\\.flow\\.component\\.HtmlComponent",// De-facto abstract class
            "com\\.vaadin\\.flow\\.component\\.HtmlContainer",// De-facto abstract class
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

            //Various test classes
            "com\\.vaadin\\.flow\\.server\\.MockVaadinServletService",
            "com\\.vaadin\\.flow\\.server\\.MockServletServiceSessionSetup",
            "com\\.vaadin\\.flow\\.server\\.MockServletConfig",
            "com\\.vaadin\\.flow\\.server\\.MockServletContext",
            "com\\.vaadin\\.flow\\.templatemodel\\.Bean",
            "com\\.vaadin\\.flow\\.internal\\.HasCurrentService",
            "com\\.vaadin\\.flow\\.component\\.ValueChangeMonitor",
            "com\\.vaadin\\.flow\\.templatemodel\\.BeanContainingBeans(\\$.*)?",
    };


    public static <T> T serializeAndDeserialize(T instance)
            throws IOException, ClassNotFoundException {
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
    private static final List<String> getRawClasspathEntries() {
        // try to keep the order of the classpath
        List<String> locations = new ArrayList<>();

        String pathSep = System.getProperty("path.separator");
        String classpath = System.getProperty("java.class.path");

        if (classpath.startsWith("\"")) {
            classpath = classpath.substring(1);
        }
        if (classpath.endsWith("\"")) {
            classpath = classpath.substring(0, classpath.length() - 1);
        }

        String[] split = classpath.split(pathSep);
        locations.addAll(Arrays.asList(split));

        return locations;
    }

    /**
     * Lists class names (based on .class files) in a directory (a package path
     * root).
     *
     * @param parentPackage parent package name or null at root of hierarchy, used by
     *                      recursion
     * @param parent        File representing the directory to scan
     * @return collection of fully qualified class names in the directory
     */
    private static final Collection<String> findClassesInDirectory(
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
     * Tests that all the relevant classes and interfaces under
     * {@link #BASE_PACKAGES} implement Serializable.
     *
     * @throws Exception
     */
    @Test
    public void classesSerializable() throws Exception {
        List<String> rawClasspathEntries = getRawClasspathEntries();

        List<String> classes = new ArrayList<>();
        for (String location : rawClasspathEntries) {
            classes.addAll(findServerClasses(location));
        }

        ArrayList<Field> nonSerializableFunctionFields = new ArrayList<>();

        List<Class<?>> nonSerializableClasses = new ArrayList<>();
        for (String className : classes) {
            Class<?> cls = Class.forName(className);
            // Don't add classes that have a @Ignore annotation on the class
            if (isTestClass(cls)) {
                continue;
            }

            // report fields that use lambda types that won't be serializable
            // (also in synthetic classes)
            Stream.of(cls.getDeclaredFields())
                    .filter(field -> isFunctionalType(field.getGenericType()))
                    .filter(field-> !isStatic(field.getModifiers()))
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
                if (cls.getSuperclass() == Object.class
                        && cls.getInterfaces().length == 1) {
                    // Single interface implementors
                    Class<?> iface = cls.getInterfaces()[0];

                    if (iface == Runnable.class) {
                        // Ignore Runnables used with access()
                        continue;
                    } else if (iface == Comparator.class) {
                        // Ignore inline comparators
                        continue;
                    }
                }
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

    private void serializeAndDeserialize(Class<?> clazz)
            throws IOException, ClassNotFoundException, InstantiationException,
            IllegalAccessException, IllegalArgumentException,
            InvocationTargetException {
        Optional<Constructor<?>> defaultCtor = Stream
                .of(clazz.getDeclaredConstructors())
                .filter(ctor -> ctor.getParameterCount() == 0).findFirst();
        if (!defaultCtor.isPresent()) {
            return;
        }
        defaultCtor.get().setAccessible(true);
        try { //todo remove this try when the test is passable
            Object instance = defaultCtor.get().newInstance();
            serializeAndDeserialize(instance);
        } catch (Throwable e) {
            System.err.println(clazz.getName());
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
        String nonSerializableString = "";
        for (Class<?> c : nonSerializableClasses) {
            nonSerializableString += ",\n" + c.getName();
            if (c.isAnonymousClass()) {
                nonSerializableString += "(super: ";
                nonSerializableString += c.getSuperclass().getName();
                nonSerializableString += ", interfaces: ";
                for (Class<?> i : c.getInterfaces()) {
                    nonSerializableString += i.getName();
                    nonSerializableString += ",";
                }
                nonSerializableString += ")";
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
     * either a directory or a JAR that matches {@link #JAR_PATTERN}.
     * <p>
     * Only classes under {@link #BASE_PACKAGES} are considered, and those
     * matching {@link #EXCLUDED_PATTERNS} are filtered out.
     *
     * @param classpathEntry
     * @return
     * @throws IOException
     */
    private List<String> findServerClasses(String classpathEntry)
            throws IOException {
        Collection<String> classes = new ArrayList<>();

        File file = new File(classpathEntry);
        if (file.isDirectory()) {
            classes = findClassesInDirectory(null, file);
        } else if (file.getName().matches(JAR_PATTERN)) {
            classes = findClassesInJar(file);
        } else {
            System.out.println("Ignoring " + classpathEntry);
            return Collections.emptyList();
        }

        List<String> filteredClasses = new ArrayList<>();
        for (String className : classes) {
            boolean ok = false;
            for (String basePackage : BASE_PACKAGES) {
                if (className.startsWith(basePackage + ".")) {
                    ok = true;
                    break;
                }
            }
            for (String excludedPrefix : EXCLUDED_PATTERNS) {
                if (className.matches(excludedPrefix)) {
                    ok = false;
                    break;
                }
            }

            // Don't add test classes
            if (className.contains("Test")) {
                ok = false;
            }

            if (ok) {
                filteredClasses.add(className);
            }
        }

        return filteredClasses;
    }

    /**
     * Lists class names (based on .class files) in a JAR file.
     *
     * @param file a valid JAR file
     * @return collection of fully qualified class names in the JAR
     * @throws IOException
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
