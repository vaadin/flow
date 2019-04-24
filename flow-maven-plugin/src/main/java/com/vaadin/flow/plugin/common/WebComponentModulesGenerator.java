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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.server.webcomponent.WebComponentExporterTagExtractor;
import com.vaadin.flow.server.webcomponent.WebComponentGenerator;

/**
 * @author Vaadin Ltd
 * @since
 */
public class WebComponentModulesGenerator extends ClassPathIntrospector {

    private static final String GENERATE_MODULE_METHOD = "generateModule";

    private static final String ABSENT_METHOD_ERROR = String.format(
            "There is no method '%s' in the class '%s', consider updating flow-server dependency",
            GENERATE_MODULE_METHOD, WebComponentExporter.class.getName());

    private static final Logger LOGGER = LoggerFactory
            .getLogger(WebComponentModulesGenerator.class);

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
     * class in the given {@code outputFolder}.
     *
     * @param clazz
     *         web component exporter class
     * @param outputFolder
     *         folder into which the generate file is written
     * @return the generated module content
     */
    public File generateModuleFile(
            Class<? extends WebComponentExporter<? extends Component>> clazz,
            File outputFolder) {
        String tag = getTag(clazz);

        Path generatedFile = outputFolder.toPath().resolve(tag + ".html");
        if (generatedFile.toFile().exists()) {
            LOGGER.debug("File '{}' already exists in the '{}' directory."
                    + "It might be a previously generated web component " +
                            "module file "
                    + "or it's an imported dependency. The file will be overwritten.",
                    generatedFile.getFileName(), outputFolder.getPath());
        }
        try {
            Files.write(generatedFile,
                    Collections.singletonList(generateModule(clazz)),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(String.format(
                    "Failed to create web component module file '%s'",
                    generatedFile), e);
        }
        return generatedFile.toFile();
    }

    private String generateModule(
            Class<? extends WebComponentExporter<? extends Component>> exporterClass) {
        Class<?> generatorClass = loadClassInProjectClassLoader(
                WebComponentGenerator.class.getName());
        Method generateMethod = Stream.of(generatorClass.getDeclaredMethods())
                .filter(method -> method.getParameters().length == 3
                        && method.getName().equals(GENERATE_MODULE_METHOD)
                        && Class.class.isAssignableFrom(method.getParameterTypes()[0]))
                .findFirst().orElseThrow(
                        () -> new IllegalStateException(ABSENT_METHOD_ERROR));

        try {
            return (String) generateMethod.invoke(null, exporterClass, "../",
                    true);
        } catch (IllegalAccessException | IllegalArgumentException
                | InvocationTargetException exception) {
            throw new RuntimeException(String.format(
                    "Unable to invoke '%s::%s' for the exporter type '%s'",
                    WebComponentGenerator.class.getName(),
                    GENERATE_MODULE_METHOD, exporterClass.getName()),
                    exception);
        }
    }

    private String getTag(Class<? extends WebComponentExporter<?
            extends Component>> exporterClass) {
        Class<?> extractorClass =
                loadClassInProjectClassLoader(WebComponentExporterTagExtractor.class.getName());

        Method tagMethod = Stream.of(extractorClass.getDeclaredMethods())
                .filter(method ->
                        method.getReturnType() == String.class
                                && method.getParameterCount() == 1)
                .findFirst().orElseThrow(() -> new RuntimeException(
                        String.format("Failed to find tag extraction method " +
                                        "from '%s'",
                                WebComponentExporterTagExtractor.class.getName())));

        try {
            return (String) tagMethod.invoke(extractorClass.newInstance(), exporterClass);
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new RuntimeException(String.format("Unable to extract tag " +
                            "from '%s' using '%s'", exporterClass.getName(),
                    WebComponentExporterTagExtractor.class.getName()), e);
        }
    }
}
