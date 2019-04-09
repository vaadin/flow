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
package com.vaadin.flow.plugin.common;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.webcomponent.WebComponentConfiguration;
import com.vaadin.flow.server.webcomponent.WebComponentGenerator;

/**
 * @author Vaadin Ltd
 */
public class WebComponentModulesGenerator extends ClassPathIntrospector {

    private static final String GENERATE_MODULE_METHOD = "generateModule";
    private static final String GET_TAG_METHOD = "getTag";
    private static final String CREATE_METHOD = "create";

    private static final String ABSENT_METHOD_ERROR = String.format(
            "There is no method '%s' in the class '%s', consider updating flow-server dependency",
            GENERATE_MODULE_METHOD, WebComponentExporter.class.getName());

    /**
     * Prepares the class to find web component exporters from the project
     * classes specified.
     *
     * @param introspector
     *         another introspector whose reflection tools will be reused to
     *         find the web component exporters
     */
    public WebComponentModulesGenerator(ClassPathIntrospector introspector) {
        super(introspector);
    }

    /**
     * Discovers all non-abstract web component exporters in the classpath.
     *
     * @return stream of all non abstract web component exporters found in the
     *         class path
     */
    @SuppressWarnings("unchecked")
    public Stream<Class<? extends WebComponentExporter<? extends Component>>> getExporters() {
        return getSubtypes(WebComponentExporter.class)
                .filter(clazz -> !clazz.isInterface()
                        && !Modifier.isAbstract(clazz.getModifiers()))
                .map(clazz -> (Class<? extends WebComponentExporter<? extends Component>>) clazz);
    }

    /**
     * Generate a file with web component html/JS content for given exporter
     * class in the given {@code outputFolder}
     *
     * @param clazz
     *         web component exporter class
     * @return the generated module content
     */
    public File generateModuleFile(
            Class<? extends WebComponentExporter<? extends Component>> clazz,
            File outputFolder) {

        Object configuration =
                createWebComponentConfiguration(clazz);

        String fileName = getTag(configuration);
        int index = 1;
        Path generatedFile;
        do {
            generatedFile = outputFolder.toPath().resolve(fileName + ".html");
            index++;
            fileName = getTag(configuration) + index;
        } while (generatedFile.toFile().exists());
        try {
            Files.write(generatedFile,
                    Collections.singletonList(generateModule(configuration)),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(String.format(
                    "Failed to create web component module file '%s'",
                    generatedFile), e);
        }
        return generatedFile.toFile();
    }

    private Object createWebComponentConfiguration(
            Class<? extends WebComponentExporter<? extends Component>> exporterClass) {
        Class<?> factoryClass =
                loadClassInProjectClassLoader(WebComponentExporter.WebComponentConfigurationFactory.class.getName());
        Object factory;
        try {
            factory = factoryClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(String.format("Failed to create %s " +
                            "instance",
                    WebComponentExporter.WebComponentConfigurationFactory.class.getName()),
                    e);
        }

        Method createMethod = Stream.of(factoryClass.getDeclaredMethods())
                .filter(method -> method.getParameterCount() == 1)
                .filter(method -> Class.class.isAssignableFrom(method.getParameterTypes()[0]))
                .findFirst().orElseThrow(() ->
                        new RuntimeException(String.format("Could not find " +
                                        "method '%s::%s(Class< ? extends %s>)!",
                                WebComponentExporter.WebComponentConfigurationFactory.class.getName(),
                                CREATE_METHOD,
                                WebComponentExporter.class.getSimpleName())));
        try {
            return createMethod.invoke(factory, exporterClass);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(String.format("Failed to create %s " +
                            "instance",
                    WebComponentConfiguration.class.getName()), e);
        }
    }

    private String generateModule(
            Object configuration) {
        Class<?> generatorClass = loadClassInProjectClassLoader(
                WebComponentGenerator.class.getName());
        Method generateMethod = Stream.of(generatorClass.getDeclaredMethods())
                .filter(method -> method.getParameters().length == 2
                        && method.getName().equals(GENERATE_MODULE_METHOD))
                .findFirst().orElseThrow(
                        () -> new IllegalStateException(ABSENT_METHOD_ERROR));

        try {
            return (String) generateMethod.invoke(null, configuration, "");
        } catch (IllegalAccessException | IllegalArgumentException
                | InvocationTargetException exception) {
            throw new RuntimeException(String.format(
                    "Unable to invoke '%s::%s' for the exporter type '%s'",
                    WebComponentGenerator.class.getName(),
                    GENERATE_MODULE_METHOD, configuration.getClass().getName()),
                    exception);
        }
    }

    private String getTag(Object configuration) {
        boolean accessible = false;
        Method tagMethod = null;
        try {
            tagMethod = configuration.getClass().getMethod(GET_TAG_METHOD);
            accessible = tagMethod.isAccessible();
            tagMethod.setAccessible(true);
            return (String) tagMethod.invoke(configuration);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(String.format("Unable to invoke " +
                            "'%s::%s' for the configuration type '%s'",
                    WebComponentConfiguration.class.getName(),
                    GET_TAG_METHOD, configuration.getClass().getName()), e);
        } finally {
            if (tagMethod != null) {
                tagMethod.setAccessible(accessible);
            }
        }
    }
}
