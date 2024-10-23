/*
 * Copyright 2000-2024 Vaadin Ltd.
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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;

import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.scanner.ReflectionsClassFinder;
import com.vaadin.flow.utils.FlowFileUtils;

/**
 * Helper class to deal with classloading of Flow plugin tasks.
 */
public final class Reflector {

    public static final String INCLUDE_FROM_COMPILE_DEPS_REGEX = ".*(/|\\\\)(portlet-api|javax\\.servlet-api)-.+jar$";

    private final URLClassLoader taskClassLoader;
    private Map.Entry<Class<?>, Object> classFinderTuple;

    /**
     * Creates a new reflector instance for the given classloader.
     *
     * @param taskClassLoader
     *            class loader to be used to create task instances.
     */
    public Reflector(URLClassLoader taskClassLoader) {
        this.taskClassLoader = taskClassLoader;
    }

    /**
     * Gets the task class loader.
     *
     * @return the task class loader.
     */
    public URLClassLoader getTaskClassLoader() {
        return taskClassLoader;
    }

    /**
     * Loads the class with the given name from the task classloader.
     *
     * @param className
     *            the name of the class to load.
     * @return the class object.
     * @throws ClassNotFoundException
     *             if the class was not found.
     */
    public Class<?> loadClass(String className) throws ClassNotFoundException {
        return taskClassLoader.loadClass(className);
    }

    /**
     * Get a resource from the classpath.
     *
     * @param name
     *            class literal
     * @return the resource
     */
    public URL getResource(String name) {
        return taskClassLoader.getResource(name);
    }

    /**
     * Creates task instances for the given Flow mojo, loading classes the task
     * classloader.
     * <p>
     * </p>
     * Expects Flow mojos extending {@link FlowModeAbstractMojo} to have a
     * companion task class, named replacing the {@literal Mojo} suffix with
     * {@literal Task} (e.g. {@literal BuildFrontendMojo} and
     * {@literal BuildFrontendTask}) and having the same fields as the mojo
     * class. Task class is loaded from the task class loader provided to the
     * reflector. After the task is instantiated, all mojos fields are copied to
     * the task.
     * <p>
     * </p>
     * The lists of parameters types and values must not contain the argument
     * defined by the {@link FlowModeAbstractTask} class, but only additional
     * type for the specific subclass constructor.
     *
     * @param mojo
     *            The mojo for which to create the task.
     * @param params
     *            additional task constructor parameter types.
     * @param args
     *            additional task constructor parameter values.
     * @return an instance of the task class related to the given mojo.
     * @throws Exception
     *             if the task instance cannot be created.
     */
    public Mojo createTask(FlowModeAbstractMojo mojo, List<Class<?>> params,
            List<Object> args) throws Exception {

        Class<?> baseTaskClass = toTaskClass(FlowModeAbstractMojo.class);

        Class<?> taskClass;
        try {
            taskClass = mojo.taskClass(this);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(
                    "Cannot find companion task class for "
                            + mojo.getClass().getName(),
                    e);
        }

        if (!baseTaskClass.isAssignableFrom(taskClass)) {
            throw new IllegalArgumentException(mojo.getClass().getName()
                    + " companion task class " + taskClass.getName()
                    + " does not extend " + baseTaskClass.getName());
        }
        Map.Entry<Class<?>, Object> classFinderTuple = getOrCreateClassFinder();
        List<Class<?>> paramTypes = new ArrayList<>(List.of(MavenProject.class,
                classFinderTuple.getKey(), Log.class));
        paramTypes.addAll(params);

        List<Object> values = new ArrayList<>();
        values.add(mojo.project);
        values.add(classFinderTuple.getValue());
        values.add(mojo.getLog());
        values.addAll(args);
        Constructor<?> ctor = taskClass
                .getDeclaredConstructor(paramTypes.toArray(Class[]::new));
        Object task = ctor.newInstance(values.toArray());
        copyFields(mojo, task);
        return (Mojo) task;
    }

    /**
     * Gets a new {@link Reflector} instance for the current Mojo execution.
     * <p>
     * </p>
     * Task class loader is created based on project and plugin dependencies,
     * with the first ones having precedence over the seconds. The maven.api
     * class realm is used as parent classloader, allowing usage of Maven core
     * classes in the tasks.
     *
     * @param project
     *            the maven project.
     * @param mojoExecution
     *            the current mojo execution.
     * @return a Reflector instance for the current maven execution.
     */
    public static Reflector of(MavenProject project,
            MojoExecution mojoExecution) {
        URLClassLoader taskClassLoader = createTaskClassLoader(project,
                mojoExecution);
        return new Reflector(taskClassLoader);
    }

    private synchronized Map.Entry<Class<?>, Object> getOrCreateClassFinder()
            throws Exception {
        if (classFinderTuple == null) {
            Class<?> classFinderImplClass = loadClass(
                    ReflectionsClassFinder.class.getName());
            classFinderTuple = Map.entry(loadClass(ClassFinder.class.getName()),
                    classFinderImplClass
                            .getConstructor(ClassLoader.class, URL[].class)
                            .newInstance(taskClassLoader,
                                    taskClassLoader.getURLs()));
        }
        return classFinderTuple;
    }

    private static URLClassLoader createTaskClassLoader(MavenProject project,
            MojoExecution mojoExecution) {
        List<URL> urls = new ArrayList<>();
        String outputDirectory = project.getBuild().getOutputDirectory();
        if (outputDirectory != null) {
            urls.add(FlowFileUtils.convertToUrl(new File(outputDirectory)));
        }

        Function<Artifact, String> keyMapper = artifact -> artifact.getGroupId()
                + ":" + artifact.getArtifactId();

        Map<String, Artifact> taskDependencies = new HashMap<>(project
                .getArtifacts().stream()
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
            mojoExecution.getMojoDescriptor().getPluginDescriptor()
                    .getArtifacts().stream()
                    .filter(artifact -> !taskDependencies
                            .containsKey(keyMapper.apply(artifact)))
                    .forEach(artifact -> taskDependencies
                            .put(keyMapper.apply(artifact), artifact));
        }

        taskDependencies.values().stream()
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
        return new URLClassLoader(urls.toArray(URL[]::new),
                mavenApiClassLoader);
    }

    private void copyFields(FlowModeAbstractMojo mojo, Object task)
            throws IllegalAccessException {
        Class<?> mojoClass = mojo.getClass();
        Class<?> taskClass = task.getClass();
        while (taskClass != null && taskClass != Object.class) {
            for (Field taskField : taskClass.getDeclaredFields()) {
                if (Modifier.isStatic(taskField.getModifiers())) {
                    continue;
                }

                Object value = null;
                boolean found = false;
                try {
                    Field mojoField = findField(mojoClass, taskField.getName());
                    if (mojoField.getType()
                            .isAssignableFrom(taskField.getType())) {
                        mojoField.setAccessible(true);
                        value = mojoField.get(mojo);
                        found = true;
                    }
                } catch (NoSuchFieldException ex) {
                    String message = "Field " + taskField.getName()
                            + " defined in " + taskClass.getName()
                            + " is missing in " + mojoClass.getName();
                    mojo.getLog().debug(message);
                }
                if (found) {
                    taskField.setAccessible(true);
                    taskField.set(task, value);
                }
            }
            taskClass = taskClass.getSuperclass();
        }
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

    private Class<?> toTaskClass(Class<?> mojoClass)
            throws ClassNotFoundException {
        String taskClass = mojoClass.getName().replace("Mojo", "Task");
        return loadClass(taskClass);
    }

}
