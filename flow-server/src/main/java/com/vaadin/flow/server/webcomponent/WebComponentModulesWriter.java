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
package com.vaadin.flow.server.webcomponent;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.WebComponentExporterFactory;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.theme.Theme;

/**
 * Writes web components generated from
 * {@link com.vaadin.flow.component.WebComponentExporter} implementation classes
 * to a target directory.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 2.0
 */
public final class WebComponentModulesWriter implements Serializable {

    private WebComponentModulesWriter() {
    }

    /**
     * Generates web component modules using
     * {@link com.vaadin.flow.server.webcomponent.WebComponentGenerator} and
     * writes the generated modules to {@code outputDirectory}. The name of the
     * file is {@code [web component's tag].js}.
     *
     * @param exporterClasses
     *            set of
     *            {@link WebComponentExporter}/{@link WebComponentExporterFactory}
     *            classes
     * @param outputDirectory
     *            target directory for the generated web component module files
     * @param themeName
     *            the theme defined using {@link Theme} or {@code null} if not
     *            defined
     * @return generated files
     * @throws java.lang.NullPointerException
     *             if {@code exportedClasses} or {@code outputDirectory} is null
     * @throws java.lang.IllegalArgumentException
     *             if {@code outputDirectory} is not a directory
     */
    private static Set<File> writeWebComponentsToDirectory( // NOSONAR
            Set<Class<?>> exporterClasses, File outputDirectory,
            String themeName) {
        // this method is used via reflection by DirectoryWriter
        Objects.requireNonNull(exporterClasses,
                "Parameter 'exporterClasses' must not be null");
        Objects.requireNonNull(outputDirectory,
                "Parameter 'outputDirectory' must not be null");

        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        if (!outputDirectory.isDirectory()) {
            throw new IllegalArgumentException(String.format("Path provided "
                    + "by parameter 'outputDirectory' (%s) is not a directory",
                    outputDirectory.getPath()));
        }

        return WebComponentExporterUtils.getFactories(exporterClasses).stream()
                .map(factory -> writeWebComponentToDirectory(factory,
                        outputDirectory, themeName))
                .collect(Collectors.toSet());
    }

    /**
     * Generate a file with web component html/JS content for given exporter
     * class in the given {@code outputFolder}.
     *
     * @param factory
     *            web component exporter factory
     * @param outputDirectory
     *            folder into which the generate file is written
     * @param themeName
     *            the theme defined using {@link Theme} or {@code null} if not
     *            defined
     * @return the generated module content
     */
    private static File writeWebComponentToDirectory(
            WebComponentExporterFactory<?> factory, File outputDirectory,
            String themeName) {
        String tag = getTag(factory);

        String fileName = tag + ".js";
        Path generatedFile = outputDirectory.toPath().resolve(fileName);
        try {
            FileUtils.forceMkdir(generatedFile.getParent().toFile());
            Files.write(generatedFile,
                    Collections
                            .singletonList(generateModule(factory, themeName)),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(String.format(
                    "Failed to create web component module file '%s'",
                    generatedFile), e);
        }
        return generatedFile.toFile();
    }

    private static String generateModule(
            WebComponentExporterFactory<? extends Component> factory,
            String themeName) {
        return WebComponentGenerator.generateModule(factory, "../", themeName);
    }

    private static String getTag(
            WebComponentExporterFactory<? extends Component> factory) {
        WebComponentExporterTagExtractor exporterTagExtractor = new WebComponentExporterTagExtractor();
        return exporterTagExtractor.apply(factory);
    }

    /**
     * Enables the usage of given {@link WebComponentModulesWriter} class via
     * reflection. This is to simplify the usage of the {@code
     * WebComponentModulesWriter} when the writer and received
     * {@link com.vaadin.flow.component.WebComponentExporter} classes are loaded
     * by a different class loader than the code using the writer.
     */
    public static final class DirectoryWriter implements Serializable {
        private static final String WRITE_MODULES_METHOD = "writeWebComponentsToDirectory";

        /**
         * Calls
         * {@link #writeWebComponentsToDirectory(java.util.Set, java.io.File, java.lang.String)}
         * via reflection on the supplied {@code writer}. The {@code writer} and
         * {@code exporterClasses} must be loaded with the same class loader.
         *
         * @param writerClass
         *            {@code WebComponentModulesWriter} class
         * @param exporterClasses
         *            set of
         *            {@link WebComponentExporter}/{@link WebComponentExporterFactory}
         *            classes, loaded with the same class loader as
         *            {@code writer}
         * @param outputDirectory
         *            target directory for the generated web component module
         *            files
         * @param themeName
         *            the theme defined using {@link Theme} or {@code null} if
         *            not defined
         * @return generated files
         * @throws java.lang.NullPointerException
         *             if {@code writerClassSupplier},
         *             {@code exporterClassSupplier}, or {@code
         *             outputDirectory} is {@code null}
         * @throws java.lang.IllegalArgumentException
         *             if {@code writer} is not
         *             {@code WebComponentModulesWriter} class
         * @throws java.lang.IllegalArgumentException
         *             if {@code writerClass} and {@code exporterClasses} do not
         *             share a class loader
         * @throws java.lang.IllegalStateException
         *             if the received {@code writer} does not have method
         *             {@link #writeWebComponentsToDirectory(java.util.Set, java.io.File, java.lang.String)}
         * @throws java.lang.RuntimeException
         *             if reflective method invocation fails
         * @see #writeWebComponentsToDirectory(java.util.Set, java.io.File,
         *      java.lang.String)
         */
        @SuppressWarnings("unchecked")
        public static Set<File> generateWebComponentsToDirectory(
                Class<?> writerClass, Set<Class<?>> exporterClasses,
                File outputDirectory, String themeName) {
            Objects.requireNonNull(writerClass,
                    "Parameter 'writerClassSupplier' must not null");
            Objects.requireNonNull(exporterClasses,
                    "Parameter 'exporterClasses' must not be null");
            Objects.requireNonNull(outputDirectory,
                    "Parameter 'outputDirectory' must not be null");

            /*
             * We'll treat null class loader as "no shared parent" instead of
             * bootstrap classloader - otherwise we'd have no way of ensuring
             * that the class loaders share parentage.
             */
            for (Class<?> exporterClass : exporterClasses) {
                if (!ReflectTools.findClosestCommonClassLoaderAncestor(
                        writerClass.getClassLoader(),
                        exporterClass.getClassLoader()).isPresent()) {
                    throw new IllegalArgumentException(String.format(
                            "Supplied writer '%s' and "
                                    + "supplied exporter '%s' have different "
                                    + "class loaders, '%s' and '%s', "
                                    + "respectively. Writer and exporters "
                                    + "must share a class loader.",
                            writerClass.getName(), exporterClass.getName(),
                            writerClass.getClassLoader().getClass().getName(),
                            exporterClass.getClassLoader().getClass()
                                    .getName()));
                }
            }

            if (!WebComponentModulesWriter.class.getName()
                    .equals(writerClass.getName())) { // NOSONAR
                throw new IllegalArgumentException(
                        "Argument 'writer' should be a class of '"
                                + WebComponentModulesWriter.class.getName()
                                + "' but it is '" + writerClass.getName()
                                + "'");
            }
            Method writeMethod = getMethod(writerClass, WRITE_MODULES_METHOD)
                    .orElseThrow(() -> new IllegalStateException(String.format(
                            "Could not locate locate method '%s' on the "
                                    + "received writer '%s'.",
                            WRITE_MODULES_METHOD, writerClass.getName())));
            try {
                final boolean accessible = writeMethod.isAccessible();
                writeMethod.setAccessible(true);
                Set<File> files = ((Set<File>) writeMethod.invoke(null,
                        exporterClasses, outputDirectory, themeName));
                writeMethod.setAccessible(accessible);
                return files;
            } catch (IllegalAccessException exception) {
                throw new RuntimeException("Failed to call '"
                        + WRITE_MODULES_METHOD
                        + "' via reflection because Java language access control doesn't allow to call it",
                        exception);
            } catch (InvocationTargetException exception) {
                throw new RuntimeException(
                        "Could not write exported web component module because of exception: "
                                + exception.getCause().getMessage(),
                        exception.getCause());
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
