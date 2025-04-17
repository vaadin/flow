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

package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.WebComponentExporterFactory;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.webcomponent.WebComponentModulesWriter;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.ThemeDefinition;

/**
 * Generates embeddable web component files in npm mode, hiding the complexity
 * caused by using a different class loader.
 *
 * Uses {@link com.vaadin.flow.server.webcomponent.WebComponentModulesWriter} to
 * generate web component modules files from
 * {@link com.vaadin.flow.component.WebComponentExporter} or
 * {@link WebComponentExporterFactory} implementations found by
 * {@link ClassFinder}.
 *
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd.
 * @since 2.0
 */
public class FrontendWebComponentGenerator implements Serializable {
    private final ClassFinder finder;

    /**
     * Creates a new instances and stores the {@code finder} to be used for
     * locating
     * {@link com.vaadin.flow.server.webcomponent.WebComponentModulesWriter} and
     * {@link com.vaadin.flow.component.WebComponentExporter}/{@link WebComponentExporterFactory}
     * classes.
     *
     * @param finder
     *            {@link com.vaadin.flow.server.frontend.scanner.ClassFinder}
     *            implementation
     */
    public FrontendWebComponentGenerator(ClassFinder finder) {
        this.finder = finder;
    }

    /**
     * Collects
     * {@link com.vaadin.flow.server.webcomponent.WebComponentModulesWriter}
     * class and classes that extend
     * {@link com.vaadin.flow.component.WebComponentExporter}/{@link WebComponentExporterFactory}
     * using {@code
     * finder}. Generates web component modules and places the into the {@code
     * outputDirectory}.
     *
     * @param outputDirectory
     *            target directory for the web component module files
     * @param theme
     *            the theme defined using {@link Theme} or {@code null} if not
     *            defined
     * @return generated files
     * @throws java.lang.IllegalStateException
     *             if {@code finder} cannot locate required classes
     */
    public Set<File> generateWebComponents(File outputDirectory,
            ThemeDefinition theme) {
        try {
            final Class<?> writerClass = finder
                    .loadClass(WebComponentModulesWriter.class.getName());
            Set<Class<?>> exporterRelatedClasses = new HashSet<>();
            finder.getSubTypesOf(WebComponentExporter.class.getName())
                    .forEach(exporterRelatedClasses::add);
            finder.getSubTypesOf(WebComponentExporterFactory.class.getName())
                    .forEach(exporterRelatedClasses::add);
            final String themeName = theme == null ? "" : theme.getName();
            return WebComponentModulesWriter.DirectoryWriter
                    .generateWebComponentsToDirectory(writerClass,
                            exporterRelatedClasses, outputDirectory, themeName);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "Unable to locate a required class using custom class "
                            + "loader",
                    e);
        }
    }
}
