/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
                            exporterRelatedClasses, outputDirectory, false,
                            themeName);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "Unable to locate a required class using custom class "
                            + "loader",
                    e);
        }
    }
}
