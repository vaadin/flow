/*
 * Copyright 2000-2021 Vaadin Ltd.
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
     * @param compatibilityMode
     *            {@code true} to generated html modules, {@code false} to
     *            generate JavaScript modules
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
            boolean compatibilityMode, String themeName) {
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

        return WebComponentExporterUtils.getFactories(exporterClasses).stream()
                .map(factory -> writeWebComponentToDirectory(factory,
                        outputDirectory, compatibilityMode, themeName))
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
            boolean compatibilityMode, String themeName) {
        String tag = getTag(factory);

        String fileName = compatibilityMode ? tag + ".html" : tag + ".js";
        Path generatedFile = outputDirectory.toPath().resolve(fileName);
        try {
            FileUtils.forceMkdir(generatedFile.getParent().toFile());
            Files.write(generatedFile,
                    Collections.singletonList(generateModule(factory,
                            compatibilityMode, themeName)),
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
            boolean compatibilityMode, String themeName) {
        return WebComponentGenerator.generateModule(factory, "../",
                compatibilityMode, themeName);
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

        /**
         * Calls
         * {@link #writeWebComponentsToDirectory(java.util.Set, java.io.File, boolean, java.lang.String)}
         * via reflection on the supplied {@code writer}. The {@code writer} and
         * {@code exporterClasses} must be loaded with the same class loader.
         *
         * @param exporterClasses
         *            set of
         *            {@link WebComponentExporter}/{@link WebComponentExporterFactory}
         *            classes, loaded with the same class loader as
         *            {@code writer}
         * @param outputDirectory
         *            target directory for the generated web component module
         *            files
         * @param compatibilityMode
         *            {@code true} to generated html modules, {@code false} to *
         *            generate JavaScript modules
         * @param themeName
         *            the theme defined using {@link Theme} or {@code null} if
         *            not defined
         * @return generated files
         * @throws java.lang.NullPointerException
         *             if {@code exporterClassSupplier}, or {@code
         *             outputDirectory} is {@code null}
         * @throws java.lang.IllegalArgumentException
         *             if {@link WebComponentModulesWriter} and
         *             {@code exporterClasses} do not share a class loader
         * @see #writeWebComponentsToDirectory(java.util.Set, java.io.File,
         *      boolean, java.lang.String)
         */
        public static Set<File> generateWebComponentsToDirectory(
                Set<Class<?>> exporterClasses, File outputDirectory,
                boolean compatibilityMode, String themeName) {
            Objects.requireNonNull(exporterClasses,
                    "Parameter 'exporterClasses' must not be null");
            Objects.requireNonNull(outputDirectory,
                    "Parameter 'outputDirectory' must not be null");

            /*
             * We'll treat null class loader as "no shared parent" instead of
             * bootstrap classloader - otherwise we'd have no way of ensuring
             * that the class loaders share parentage.
             */
            ClassLoader writerClassLoader = WebComponentModulesWriter.class
                    .getClassLoader();
            for (Class<?> exporterClass : exporterClasses) {
                ClassLoader exporterClassLoader = exporterClass
                        .getClassLoader();
                if (!ReflectTools.findClosestCommonClassLoaderAncestor(
                        writerClassLoader, exporterClassLoader).isPresent()) {
                    String writerClassLoaderName = writerClassLoader == null
                            ? "null"
                            : writerClassLoader.getClass().getName();

                    String exporterClassLoaderName = exporterClassLoader == null
                            ? "null"
                            : exporterClassLoader.getClass().getName();

                    throw new IllegalArgumentException(String.format(
                            "Supplied writer '%s' and "
                                    + "supplied exporter '%s' have different "
                                    + "class loaders, '%s' and '%s', "
                                    + "respectively. Writer and exporters "
                                    + "must share a class loader.",
                            WebComponentModulesWriter.class.getName(),
                            exporterClass.getName(), writerClassLoaderName,
                            exporterClassLoaderName));
                }
            }

            return WebComponentModulesWriter.writeWebComponentsToDirectory(
                    exporterClasses, outputDirectory, compatibilityMode,
                    themeName);
        }

    }
}
