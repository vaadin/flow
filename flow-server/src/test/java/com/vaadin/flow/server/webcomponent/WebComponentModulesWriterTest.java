/*
 * Copyright 2000-2026 Vaadin Ltd.
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.WebComponentExporterFactory;
import com.vaadin.flow.component.webcomponent.WebComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebComponentModulesWriterTest {

    @TempDir
    Path tempDir;
    private File outputDirectory;

    @BeforeEach
    void init() throws IOException {
        outputDirectory = Files.createTempDirectory(tempDir, "output").toFile();
    }

    @Test
    public void directoryWriter_generateWebComponentsToDirectory_canCallMethodReflectively_js() {
        Set<File> files = WebComponentModulesWriter.DirectoryWriter
                .generateWebComponentsToDirectory(
                        WebComponentModulesWriter.class,
                        Collections.singleton(MyExporter.class),
                        outputDirectory, null);

        assertEquals(1, files.size(), "One file was created");
        assertEquals("real-tag.js",
                files.stream().findFirst().get().getName(),
                "File is js module with correct name");
    }

    @Test
    public void directoryWriter_generateWebComponentsToDirectoryUsingFactory_canCallMethodReflectively_js() {
        Set<File> files = WebComponentModulesWriter.DirectoryWriter
                .generateWebComponentsToDirectory(
                        WebComponentModulesWriter.class,
                        Collections.singleton(ExporterFactory.class),
                        outputDirectory, null);

        assertEquals(1, files.size(), "One file was created");
        assertEquals("foo-bar.js",
                files.stream().findFirst().get().getName(),
                "File is js module with correct name");
    }

    @Test
    public void directoryWriter_generateWebComponentsToDirectory_zeroExportersCreatesZeroFiles() {
        Set<File> files = WebComponentModulesWriter.DirectoryWriter
                .generateWebComponentsToDirectory(
                        WebComponentModulesWriter.class, new HashSet<>(),
                        outputDirectory, null);

        assertEquals(0, files.size(), "No files were created");
    }

    @Test
    public void directoryWriter_generateWebComponentsToDirectory_nonWriterClassThrows() {
        var ex = assertThrows(IllegalArgumentException.class, () -> {
            WebComponentModulesWriter.DirectoryWriter
                    .generateWebComponentsToDirectory(MyComponent.class,
                            new HashSet<>(), outputDirectory, null);
        });
        assertTrue(ex.getMessage()
                .contains("but it is '" + MyComponent.class.getName() + "'"));
    }

    @Test
    public void directoryWriter_generateWebComponentsToDirectory_nullWriterThrows() {
        assertThrows(NullPointerException.class, () -> {
            WebComponentModulesWriter.DirectoryWriter
                    .generateWebComponentsToDirectory(null, new HashSet<>(),
                            outputDirectory, null);
        });
    }

    @Test
    public void directoryWriter_generateWebComponentsToDirectory_nullExporterSetThrows() {
        assertThrows(NullPointerException.class, () -> {
            WebComponentModulesWriter.DirectoryWriter
                    .generateWebComponentsToDirectory(
                            WebComponentModulesWriter.class, null,
                            outputDirectory, null);
        });
    }

    @Test
    public void directoryWriter_generateWebComponentsToDirectory_nullOutputDirectoryThrows() {
        assertThrows(NullPointerException.class, () -> {
            WebComponentModulesWriter.DirectoryWriter
                    .generateWebComponentsToDirectory(
                            WebComponentModulesWriter.class, new HashSet<>(),
                            null, null);
        });
    }

    /*
     * Test assets:
     */

    public static class MyComponent extends Component {
    }

    public static class MyExporter extends WebComponentExporter<MyComponent> {
        public MyExporter() {
            super("real-tag");
        }

        @Override
        public void configureInstance(WebComponent<MyComponent> webComponent,
                MyComponent component) {

        }
    }

    public static class ExporterFactory
            implements WebComponentExporterFactory<MyComponent> {
        @Override
        public WebComponentExporter<MyComponent> create() {
            return new PrivateExporter();
        }
    }

    private static class PrivateExporter
            extends WebComponentExporter<MyComponent> {
        public PrivateExporter() {
            super("foo-bar");
        }

        @Override
        public void configureInstance(WebComponent<MyComponent> webComponent,
                MyComponent component) {

        }
    }

}
