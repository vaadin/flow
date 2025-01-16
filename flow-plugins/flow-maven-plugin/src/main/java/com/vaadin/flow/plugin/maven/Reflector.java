/*
 * Copyright 2000-2025 Vaadin Ltd.
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

package com.vaadin.flow.plugin.maven;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;

import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.scanner.ReflectionsClassFinder;
import com.vaadin.flow.utils.FlowFileUtils;

/**
 * Helper class to deal with classloading of Flow plugin mojos.
 */
public final class Reflector {

    public static final String INCLUDE_FROM_COMPILE_DEPS_REGEX = ".*(/|\\\\)(portlet-api|javax\\.servlet-api)-.+jar$";
    private static final Set<String> DEPENDENCIES_GROUP_EXCLUSIONS = Set.of(
            "org.apache.maven", "org.codehaus.plexus", "org.slf4j",
            "org.eclipse.sisu");
    // Dependency required by the plugin but not provided by Flow at runtime
    private static final Set<String> REQUIRED_PLUGIN_DEPENDENCIES = Set.of(
            "org.reflections:reflections:jar",
            "org.zeroturnaround:zt-exec:jar");

    private final URLClassLoader isolatedClassLoader;
    private List<String> dependenciesIncompatibility;
    private Object classFinder;

    /**
     * Creates a new reflector instance for the given classloader.
     *
     * @param isolatedClassLoader
     *            class loader to be used to create mojo instances.
     */
    public Reflector(URLClassLoader isolatedClassLoader) {
        this.isolatedClassLoader = isolatedClassLoader;
    }

    private Reflector(URLClassLoader isolatedClassLoader, Object classFinder,
            List<String> dependenciesIncompatibility) {
        this.isolatedClassLoader = isolatedClassLoader;
        this.classFinder = classFinder;
        this.dependenciesIncompatibility = dependenciesIncompatibility;
    }

    /**
     * Gets a {@link Reflector} instance usable with the caller class loader.
     * <p>
     * </p>
     * Reflector instances are cached in Maven plugin context, but instances
     * might be associated to the plugin class loader, thus not working with
     * classes loaded by the isolated class loader. This method returns the
     * input object if it is compatible with the class loader, otherwise it
     * creates a copy referencing the same isolated class loader and
     * {@link ClassFinder}.
     *
     * @param reflector
     *            the {@link Reflector} instance.
     * @return a {@link Reflector} instance compatible with the current class
     *         loader.
     * @throws IllegalArgumentException
     *             if the input object is not a {@link Reflector} instance or if
     *             it is not possible to make a copy for it due to class
     *             definition incompatibilities.
     */
    @SuppressWarnings("unchecked")
    static Reflector adapt(Object reflector) {
        if (reflector instanceof Reflector sameClassLoader) {
            return sameClassLoader;
        } else if (Reflector.class.getName()
                .equals(reflector.getClass().getName())) {
            Class<?> reflectorClass = reflector.getClass();
            try {
                URLClassLoader classLoader = (URLClassLoader) ReflectTools
                        .getJavaFieldValue(reflector,
                                findField(reflectorClass,
                                        "isolatedClassLoader"),
                                URLClassLoader.class);
                List<String> dependenciesIncompatibility = (List<String>) ReflectTools
                        .getJavaFieldValue(reflector, findField(reflectorClass,
                                "dependenciesIncompatibility"));
                Object classFinder = ReflectTools.getJavaFieldValue(reflector,
                        findField(reflectorClass, "classFinder"));
                return new Reflector(classLoader, classFinder,
                        dependenciesIncompatibility);
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "Object of type " + reflector.getClass().getName()
                                + " is not a compatible Reflector",
                        e);
            }
        }
        throw new IllegalArgumentException(
                "Object of type " + reflector.getClass().getName()
                        + " is not a compatible Reflector");
    }

    /**
     * Gets the isolated class loader.
     *
     * @return the isolated class loader.
     */
    public URLClassLoader getIsolatedClassLoader() {
        return isolatedClassLoader;
    }

    /**
     * Loads the class with the given name from the isolated classloader.
     *
     * @param className
     *            the name of the class to load.
     * @return the class object.
     * @throws ClassNotFoundException
     *             if the class was not found.
     */
    public Class<?> loadClass(String className) throws ClassNotFoundException {
        return isolatedClassLoader.loadClass(className);
    }

    /**
     * Get a resource from the classpath of the isolated class loader.
     *
     * @param name
     *            class literal
     * @return the resource
     */
    public URL getResource(String name) {
        return isolatedClassLoader.getResource(name);
    }

    /**
     * Creates a copy of the given Flow mojo, loading classes the isolated
     * classloader.
     * <p>
     * </p>
     * Loads the given mojo class from the isolated class loader and then
     * creates a new instance for it and fills all field copying values from the
     * original mojo. The input mojo must have a public no-args constructor.
     * Mojo fields must reference types that can be safely loaded be the
     * isolated class loader, such as JDK or Maven core API. It also creates and
     * injects a {@link ClassFinder}, based on the isolated class loader.
     *
     * @param sourceMojo
     *            The mojo for which to create the instance from the isolated
     *            class loader.
     * @return an instance of the mojo loaded from the isolated class loader.
     * @throws Exception
     *             if the mojo instance cannot be created.
     */
    public Mojo createMojo(FlowModeAbstractMojo sourceMojo) throws Exception {
        Class<?> targetMojoClass = loadClass(sourceMojo.getClass().getName());
        Object targetMojo = targetMojoClass.getConstructor().newInstance();
        copyFields(sourceMojo, targetMojo);
        Field classFinderField = findField(targetMojoClass,
                FlowModeAbstractMojo.CLASSFINDER_FIELD_NAME);
        ReflectTools.setJavaFieldValue(targetMojo, classFinderField,
                getOrCreateClassFinder());
        return (Mojo) targetMojo;
    }

    /**
     * Gets a new {@link Reflector} instance for the current Mojo execution.
     * <p>
     * </p>
     * An isolated class loader is created based on project and plugin
     * dependencies, with the first ones having precedence over the seconds. The
     * maven.api class realm is used as parent classloader, allowing usage of
     * Maven core classes in the mojo.
     *
     * @param project
     *            the maven project.
     * @param mojoExecution
     *            the current mojo execution.
     * @return a Reflector instance for the current maven execution.
     */
    public static Reflector of(MavenProject project,
            MojoExecution mojoExecution) {
        List<String> dependenciesIncompatibility = new ArrayList<>();
        URLClassLoader classLoader = createIsolatedClassLoader(project,
                mojoExecution, dependenciesIncompatibility);
        Reflector reflector = new Reflector(classLoader);
        reflector.dependenciesIncompatibility = dependenciesIncompatibility;
        return reflector;
    }

    void logIncompatibilities(Consumer<String> logger) {
        if (dependenciesIncompatibility != null
                && !dependenciesIncompatibility.isEmpty()) {
            logger.accept(
                    """
                            Found dependencies defined with different versions in project and Vaadin maven plugin.
                            Project dependencies are used, but plugin execution could fail if the versions are incompatible.
                            In case of build failure please analyze the project dependencies and update versions or configure exclusions for potential offending transitive dependencies.
                            You can use 'mvn dependency:tree -Dincludes=groupId:artifactId' to detect where the dependency is defined in the project.

                            """
                            + String.join(System.lineSeparator(),
                                    dependenciesIncompatibility));
        }
    }

    private synchronized Object getOrCreateClassFinder() throws Exception {
        if (classFinder == null) {
            Class<?> classFinderImplClass = loadClass(
                    ReflectionsClassFinder.class.getName());
            classFinder = classFinderImplClass
                    .getConstructor(ClassLoader.class, URL[].class).newInstance(
                            isolatedClassLoader, isolatedClassLoader.getURLs());
        }
        return classFinder;
    }

    private static URLClassLoader createIsolatedClassLoader(
            MavenProject project, MojoExecution mojoExecution,
            List<String> dependenciesIncompatibility) {
        List<URL> urls = new ArrayList<>();
        String outputDirectory = project.getBuild().getOutputDirectory();
        if (outputDirectory != null) {
            urls.add(FlowFileUtils.convertToUrl(new File(outputDirectory)));
        }

        Function<Artifact, String> keyMapper = artifact -> artifact.getGroupId()
                + ":" + artifact.getArtifactId() + ":" + artifact.getType()
                + ((artifact.getClassifier() != null)
                        ? ":" + artifact.getClassifier()
                        : "");

        Map<String, Artifact> projectDependencies = new HashMap<>(project
                .getArtifacts().stream()
                // Exclude all maven artifacts to prevent class loading clash
                // with maven.api class realm
                .filter(artifact -> !DEPENDENCIES_GROUP_EXCLUSIONS
                        .contains(artifact.getGroupId()))
                .filter(artifact -> artifact.getFile() != null
                        && artifact.getArtifactHandler().isAddedToClasspath()
                        && (Artifact.SCOPE_COMPILE.equals(artifact.getScope())
                                || Artifact.SCOPE_RUNTIME
                                        .equals(artifact.getScope())
                                || Artifact.SCOPE_SYSTEM
                                        .equals(artifact.getScope())
                                || (Artifact.SCOPE_PROVIDED
                                        .equals(artifact.getScope())
                                        && artifact.getFile().getPath().matches(
                                                INCLUDE_FROM_COMPILE_DEPS_REGEX))))
                .collect(Collectors.toMap(keyMapper, Function.identity())));

        if (mojoExecution != null) {

            List<Artifact> pluginDependencies = mojoExecution
                    .getMojoDescriptor().getPluginDescriptor().getArtifacts()
                    .stream()
                    // Exclude all maven artifacts to prevent class loading
                    // clash with maven.api class realm
                    .filter(artifact -> !DEPENDENCIES_GROUP_EXCLUSIONS
                            .contains(artifact.getGroupId()))
                    .toList();

            // Exclude project artifact that are also defined as mandatory
            // plugin dependencies. The version provided by the plugin will be
            // used to prevent failures during maven build.
            pluginDependencies.stream().map(keyMapper)
                    .filter(REQUIRED_PLUGIN_DEPENDENCIES::contains)
                    .forEach(projectDependencies::remove);

            // Preserve required plugin dependency that are not provided by Flow
            // -1: dependency defined on both plugin and project, with different
            // version
            // 0: dependency defined on both plugin and project, with same
            // version
            // 1: dependency defined by the plugin only
            Map<Integer, List<Artifact>> potentialDuplicates = pluginDependencies
                    .stream().collect(Collectors.groupingBy(pluginArtifact -> {
                        Artifact projectArtifact = projectDependencies
                                .get(keyMapper.apply(pluginArtifact));
                        if (projectArtifact == null) {
                            return 1;
                        } else if (projectArtifact.getId()
                                .equals(pluginArtifact.getId())) {
                            return 0;
                        }
                        return -1;
                    }));
            // Log potential plugin and project dependency versions
            // incompatibilities.
            if (potentialDuplicates.containsKey(-1)) {
                potentialDuplicates.get(-1).stream().map(pluginArtifact -> {
                    String key = keyMapper.apply(pluginArtifact);
                    return String.format(
                            "%s: project version [%s], plugin version [%s]",
                            key, projectDependencies.get(key).getBaseVersion(),
                            pluginArtifact.getBaseVersion());
                }).forEach(dependenciesIncompatibility::add);
            }

            // Add dependencies defined only by the plugin
            if (potentialDuplicates.containsKey(1)) {
                potentialDuplicates.get(1)
                        .forEach(artifact -> projectDependencies
                                .put(keyMapper.apply(artifact), artifact));
            }
        }

        projectDependencies.values().stream()
                .map(artifact -> FlowFileUtils.convertToUrl(artifact.getFile()))
                .forEach(urls::add);
        ClassLoader mavenApiClassLoader;
        if (mojoExecution != null) {
            ClassRealm pluginClassRealm = mojoExecution.getMojoDescriptor()
                    .getPluginDescriptor().getClassRealm();
            try {
                mavenApiClassLoader = pluginClassRealm.getWorld()
                        .getRealm("maven.api");
            } catch (NoSuchRealmException e) {
                throw new RuntimeException(e);
            }
        } else {
            mavenApiClassLoader = Mojo.class.getClassLoader();
            if (mavenApiClassLoader instanceof ClassRealm classRealm) {
                try {
                    mavenApiClassLoader = classRealm.getWorld()
                            .getRealm("maven.api");
                } catch (NoSuchRealmException e) {
                    // Should never happen. In case, ignore the error and use
                    // class loader from the Maven class
                }
            }
        }
        return new CombinedClassLoader(urls.toArray(new URL[0]),
                mavenApiClassLoader);
    }

    // Tries to load class from the give class loader and fallbacks
    // to Platform class loader in case of failure.
    private static class CombinedClassLoader extends URLClassLoader {
        private final ClassLoader delegate;

        private CombinedClassLoader(URL[] urls, ClassLoader delegate) {
            super(urls, null);
            this.delegate = delegate;
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            try {
                return super.loadClass(name);
            } catch (ClassNotFoundException e) {
                // ignore and continue with delegate class loader
            }
            if (delegate != null) {
                try {
                    return delegate.loadClass(name);
                } catch (ClassNotFoundException e) {
                    // ignore and continue with platform class loader
                }
            }
            return ClassLoader.getPlatformClassLoader().loadClass(name);
        }

        @Override
        public URL getResource(String name) {
            URL url = super.getResource(name);
            if (url == null && delegate != null) {
                url = delegate.getResource(name);
            }
            if (url == null) {
                url = ClassLoader.getPlatformClassLoader().getResource(name);
            }
            return url;
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            Enumeration<URL> resources = super.getResources(name);
            if (!resources.hasMoreElements() && delegate != null) {
                resources = delegate.getResources(name);
            }
            if (!resources.hasMoreElements()) {
                resources = ClassLoader.getPlatformClassLoader()
                        .getResources(name);
            }
            return resources;
        }
    }

    private void copyFields(FlowModeAbstractMojo sourceMojo, Object targetMojo)
            throws IllegalAccessException, NoSuchFieldException {
        Class<?> sourceClass = sourceMojo.getClass();
        Class<?> targetClass = targetMojo.getClass();
        while (sourceClass != null && sourceClass != Object.class) {
            for (Field sourceField : sourceClass.getDeclaredFields()) {
                copyField(sourceMojo, targetMojo, sourceField, targetClass);
            }
            targetClass = targetClass.getSuperclass();
            sourceClass = sourceClass.getSuperclass();
        }
    }

    private static void copyField(FlowModeAbstractMojo sourceMojo,
            Object targetMojo, Field sourceField, Class<?> targetClass)
            throws IllegalAccessException, NoSuchFieldException {
        if (Modifier.isStatic(sourceField.getModifiers())) {
            return;
        }
        sourceField.setAccessible(true);
        Object value = sourceField.get(sourceMojo);
        if (value == null) {
            return;
        }
        Field targetField;
        try {
            targetField = targetClass.getDeclaredField(sourceField.getName());
        } catch (NoSuchFieldException ex) {
            // Should never happen, since the class definition should be
            // the same
            String message = "Field " + sourceField.getName() + " defined in "
                    + sourceField.getDeclaringClass().getName()
                    + " is missing in " + targetClass.getName();
            sourceMojo.logError(message, ex);
            throw ex;
        }

        Class<?> targetFieldType = targetField.getType();
        if (!targetFieldType.isAssignableFrom(sourceField.getType())) {
            String message = "Field " + targetFieldType.getName() + " in class "
                    + targetClass.getName() + " of type "
                    + targetFieldType.getName()
                    + " is loaded from different class loaders."
                    + " Source class loader: "
                    + sourceField.getType().getClassLoader()
                    + ", Target class loader: "
                    + targetFieldType.getClassLoader()
                    + ". This is likely a bug in the Vaadin Maven plugin."
                    + " Please, report the error on the issue tracker.";
            sourceMojo.logError(message);
            throw new NoSuchFieldException(message);
        }
        targetField.setAccessible(true);
        targetField.set(targetMojo, value);
    }

    private static Field findField(Class<?> clazz, String fieldName)
            throws NoSuchFieldException {
        while (clazz != null && !clazz.equals(Object.class)) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }

}