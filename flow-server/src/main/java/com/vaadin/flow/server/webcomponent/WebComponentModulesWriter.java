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
package com.vaadin.flow.server.webcomponent;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.WebComponentExporter;

/**
 * Writes web components generated from
 * {@link com.vaadin.flow.component.WebComponentExporter} implementation classes
 * to a target directory.
 * 
 * @author Vaadin Ltd
 * @since
 */
public final class WebComponentModulesWriter implements Serializable {
    // commented out since URLClassLoader does not have access to org/slf4j/Logger
//    private static Logger LOGGER =
//            LoggerFactory.getLogger(WebComponentModulesWriter.class);

    private WebComponentModulesWriter() {
    }

    /**
     * Generates web component modules using
     * {@link com.vaadin.flow.server.webcomponent.WebComponentGenerator} and
     * writes the generated modules to {@code outputDirectory}. The name of the
     * file is {@code [web component's tag].js}.
     * 
     * @param exporterClasses
     *            set of {@link com.vaadin.flow.component.WebComponentExporter}
     *            classes
     * @param outputDirectory
     *            target directory for the generated web component module files
     * @param bowerMode
     *            {@code true} to generated html modules, {@code false} to
     *            generate JavaScript modules
     * @return generated files
     * @throws java.lang.NullPointerException
     *             if {@code exportedClasses} or {@code outputDirectory} is null
     * @throws java.lang.IllegalArgumentException
     *             if {@code outputDirectory} is not a directory
     */
    public static Set<File> writeWebComponentsToDirectory(
            Set<Class<? extends WebComponentExporter<? extends Component>>> exporterClasses,
            File outputDirectory, boolean bowerMode) {
        Objects.requireNonNull(exporterClasses,
                "Parameter 'exporterClasses' must not be null");
        Objects.requireNonNull(outputDirectory,
                "Parameter 'outputDirectory' must not be null");

        if (!outputDirectory.isDirectory()) {
            throw new IllegalArgumentException(String.format("Path provided "
                    + "by parameter 'outputDirectory' (%s) is not a directory",
                    outputDirectory.getPath()));
        }

        return filterConcreteExporters(exporterClasses)
                .map(clazz -> writeWebComponentToDirectory(clazz,
                        outputDirectory, bowerMode))
                .collect(Collectors.toSet());
    }

    @SuppressWarnings("unchecked")
    private static Stream<Class<? extends WebComponentExporter<? extends Component>>> filterConcreteExporters(
            Set<Class<? extends WebComponentExporter<? extends Component>>> exporterClasses) {
        return exporterClasses.stream()
                .filter(clazz -> WebComponentExporter.class
                        .isAssignableFrom(clazz) && !clazz.isInterface()
                        && !Modifier.isAbstract(clazz.getModifiers()));
    }

    /**
     * Generate a file with web component html/JS content for given exporter
     * class in the given {@code outputFolder}.
     *
     * @param clazz
     *            web component exporter class
     * @param outputDirectory
     *            folder into which the generate file is written
     * @return the generated module content
     */
    private static File writeWebComponentToDirectory(
            Class<? extends WebComponentExporter<? extends Component>> clazz,
            File outputDirectory, boolean bowerMode) {
        String tag = getTag(clazz);

        String fileName = bowerMode ? tag + ".html" : tag + ".js";
        Path generatedFile = outputDirectory.toPath().resolve(fileName);
        // commented out since URLClassLoader does not have access to org/slf4j/Logger
//        if (generatedFile.toFile().exists()) {
//            LOGGER.debug("File '{}' already exists in the '{}' directory."
//                    + "It might be a previously generated web component "
//                    + "module file "
//                    + "or it's an imported dependency. The file will be overwritten.",
//                    generatedFile.getFileName(), outputDirectory.getPath());
//        }
        try {
            FileUtils.forceMkdir(generatedFile.getParent().toFile());
            Files.write(generatedFile,
                    Collections.singletonList(generateModule(clazz, bowerMode)),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(String.format(
                    "Failed to create web component module file '%s'",
                    generatedFile), e);
        }
        return generatedFile.toFile();
    }

    private static String generateModule(
            Class<? extends WebComponentExporter<? extends Component>> exporterClass,
            boolean bowerMode) {
        return WebComponentGenerator.generateModule(exporterClass, "../",
                bowerMode);
    }

    private static String getTag(
            Class<? extends WebComponentExporter<? extends Component>> exporterClass) {
        WebComponentExporterTagExtractor exporterTagExtractor = new WebComponentExporterTagExtractor();
        return exporterTagExtractor.apply(exporterClass);
    }

    /**
     * Enables the usage of given {@link WebComponentModulesWriter} class via
     * reflection. This is to simplify the usage of the {@code
     * WebComponentModulesWriter} when the writer and received
     * {@link com.vaadin.flow.component.WebComponentExporter} classes are loaded
     * by a different class loader than the code using the writer.
     */
    public static class ReflectionUsage implements Serializable {
        private static final String WRITE_MODULES_METHOD = "writeWebComponentsToDirectory";

        /**
         * Calls
         * {@link #writeWebComponentsToDirectory(java.util.Set, java.io.File, boolean)}
         * via reflection on the given {@code writer}. The {@code writer} and
         * {@code exporterClasses} must be loaded with the same class loader.
         * 
         * @param writer
         *            {@code WebComponentModulesWriter} class
         * @param exporterClasses
         *            set of
         *            {@link com.vaadin.flow.component.WebComponentExporter}
         *            classes, loaded with the same class loader as
         *            {@code writer}
         * @param outputDirectory
         *            target directory for the generated web component module
         *            files
         * @param bowerMode
         *            {@code true} to generated html modules, {@code false} to *
         *            generate JavaScript modules
         * @return generated files
         * @throws java.lang.NullPointerException
         *             if {@code writer}, {@code
         * exporterClasses}, or {@code outputDirectory} is null
         * @throws java.lang.IllegalArgumentException
         *             if {@code writer} is not
         *             {@code WebComponentModulesWriter} class
         * @throws java.lang.IllegalStateException
         *             if the received {@code writer} does not have method
         *             {@link #writeWebComponentsToDirectory(java.util.Set, java.io.File, boolean)}
         * @throws java.lang.RuntimeException
         *             if method invocation fails
         * @see #writeWebComponentsToDirectory(java.util.Set, java.io.File,
         *      boolean)
         */
        @SuppressWarnings("unchecked")
        public static Set<File> reflectiveWriteWebComponentsToDirectory(
                Class<?> writer, Set<Class<?>> exporterClasses,
                File outputDirectory, boolean bowerMode) {
            Objects.requireNonNull(writer, "Parameter 'writer' must not null");
            Objects.requireNonNull(exporterClasses,
                    "Parameter " + "'exporterClasses' must not be null");
            Objects.requireNonNull(outputDirectory,
                    "Parameter " + "'outputDirectory' must not be null");
            if (!WebComponentModulesWriter.class.getName()
                    .equals(writer.getName())) {
                throw new IllegalArgumentException(
                        "Argument 'writer' should " + "be a class of '"
                                + WebComponentModulesWriter.class.getName()
                                + "' but it is '" + writer.getName() + "'");
            }
            Method writeMethod = getMethod(writer, WRITE_MODULES_METHOD)
                    .orElseThrow(() -> new IllegalStateException(String.format(
                            "Could not locate locate method '%s' on the "
                                    + "received writer '%s'.",
                            WRITE_MODULES_METHOD, writer.getName())));
            try {
                return ((Set<File>) writeMethod.invoke(null, exporterClasses,
                        outputDirectory, bowerMode));
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(
                        "Could not write exported web component module!", e);
            }
        }

        private static Optional<Method> getMethod(Class<?> writerClass,
                String methodName) {
            return Stream.of(writerClass.getDeclaredMethods())
                    .filter(method -> method.getName().equals(methodName))
                    .findFirst();
        }
    }
}
