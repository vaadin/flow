/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.tests.util;

import java.io.File;

import org.mockito.Mockito;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.frontend.Options;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;

/**
 * Mocked Options that creates a lookup mock and class finder mock, if it's not
 * given.
 */
public class MockOptions extends Options {

    /**
     * Creates a new instance of mocked Options with a given class finder.
     *
     * @param classFinder
     *            class finder instance to use
     * @param projectFolder
     *            project base folder
     */
    public MockOptions(ClassFinder classFinder, File projectFolder) {
        super(Mockito.mock(Lookup.class), classFinder, projectFolder);

        Mockito.when(getLookup().lookup(ClassFinder.class))
                .thenReturn(classFinder);
    }

    /**
     * Creates a new instance of mocked Options with mocked class finder.
     *
     * @param projectFolder
     *            project base folder
     */
    public MockOptions(File projectFolder) {
        this(Mockito.mock(ClassFinder.class), projectFolder);
    }
}
