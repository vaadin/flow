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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.internal.ExportsWebComponent;
import com.vaadin.flow.internal.ReflectTools;

/**
 * Writes web components generated from
 * {@link com.vaadin.flow.component.WebComponentExporter} implementation classes
 * to a target directory.
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
     *            set of {@link ExportsWebComponent}
     *            classes
     * @param outputDirectory
     *            target directory for the generated web component module files
     * @param compatibilityMode
     *            {@code true} to generated html modules, {@code false} to
     *            generate JavaScript modules
     * @return generated files
     * @throws java.lang.NullPointerException
     *             if {@code exportedClasses} or {@code outputDirectory} is null
     * @throws java.lang.IllegalArgumentException
     *             if {@code outputDirectory} is not a directory
     */
    private static Set<File> writeWebComponentsToDirectory( // NOSONAR
            Set<Class<? extends ExportsWebComponent<? extends Component>>> exporterClasses,
            File outputDirectory, boolean compatibilityMode) {
        // this method is used via reflection by DirectoryWriter
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
                        outputDirectory, compatibilityMode))
                .collect(Collectors.toSet());
    }

    private static Stream<Class<? extends ExportsWebComponent<? extends Component>>> filterConcreteExporters(
            Set<Class<? extends ExportsWebComponent<? extends Component>>> exporterClasses) {
        return exporterClasses.stream()
                .filter(clazz -> ExportsWebComponent.class
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
            Class<? extends ExportsWebComponent<? extends Component>> clazz,
            File outputDirectory, boolean compatibilityMode) {
        String tag = getTag(clazz);

        String fileName = compatibilityMode ? tag + ".html" : tag + ".js";
        Path generatedFile = outputDirectory.toPath().resolve(fileName);
        try {
            FileUtils.forceMkdir(generatedFile.getParent().toFile());
            Files.write(generatedFile,
                    Collections.singletonList(
                            generateModule(clazz, compatibilityMode)),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(String.format(
                    "Failed to create web component module file '%s'",
                    generatedFile), e);
        }
        return generatedFile.toFile();
    }

    private static String generateModule(
            Class<? extends ExportsWebComponent<? extends Component>> exporterClass,
            boolean compatibilityMode) {
        return WebComponentGenerator.generateModule(exporterClass, "../",
                compatibilityMode);
    }

    private static String getTag(
            Class<? extends ExportsWebComponent<? extends Component>> exporterClass) {
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
    public static final class DirectoryWriter implements Serializable {
        private static final String WRITE_MODULES_METHOD = "writeWebComponentsToDirectory";

        /**
         * Calls
         * {@link #writeWebComponentsToDirectory(java.util.Set, java.io.File, boolean)}
         * via reflection on the supplied {@code writer}. The {@code writer} and
         * {@code exporterClasses} must be loaded with the same class loader.
         *
         * @param writerClass
         *            {@code WebComponentModulesWriter} class
         * @param exporterClasses
         *            set of
         *            {@link ExportsWebComponent}
         *            classes, loaded with the same class loader as
         *            {@code writer}
         * @param outputDirectory
         *            target directory for the generated web component module
         *            files
         * @param compatibilityMode
         *            {@code true} to generated html modules, {@code false} to *
         *            generate JavaScript modules
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
         *             {@link #writeWebComponentsToDirectory(java.util.Set, java.io.File, boolean)}
         * @throws java.lang.RuntimeException
         *             if reflective method invocation fails
         * @see #writeWebComponentsToDirectory(java.util.Set, java.io.File,
         *      boolean)
         */
        @SuppressWarnings("unchecked")
        public static Set<File> generateWebComponentsToDirectory(
                Class<?> writerClass, Set<Class<?>> exporterClasses,
                File outputDirectory, boolean compatibilityMode) {
            Objects.requireNonNull(writerClass,
                    "Parameter 'writerClassSupplier' must not null");
            Objects.requireNonNull(exporterClasses,
                    "Parameter 'exporterClassSupplier' must not be null");
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
                        exporterClasses, outputDirectory, compatibilityMode));
                writeMethod.setAccessible(accessible);
                return files;
            } catch (IllegalAccessException | InvocationTargetException
                    | NullPointerException e) {
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
