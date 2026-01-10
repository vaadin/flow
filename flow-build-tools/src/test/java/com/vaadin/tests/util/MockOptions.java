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
