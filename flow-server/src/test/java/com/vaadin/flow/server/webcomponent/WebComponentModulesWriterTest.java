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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.WebComponentExporterFactory;
import com.vaadin.flow.component.webcomponent.WebComponent;

public class WebComponentModulesWriterTest {

    private ExpectedException exception = ExpectedException.none();

    private TemporaryFolder folder = new TemporaryFolder();
    private File outputDirectory;

    @Before
    public void init() throws IOException {
        folder.create();
        outputDirectory = folder.newFolder();
    }

    @Test
    public void directoryWriter_generateWebComponentsToDirectory_canCallMethodReflectively_js() {
        Set<File> files = WebComponentModulesWriter.DirectoryWriter
                .generateWebComponentsToDirectory(
                        WebComponentModulesWriter.class,
                        Collections.singleton(MyExporter.class),
                        outputDirectory, null);

        Assert.assertEquals("One file was created", 1, files.size());
        Assert.assertEquals("File is js module with correct name",
                "real-tag.js", files.stream().findFirst().get().getName());
    }

    @Test
    public void directoryWriter_generateWebComponentsToDirectoryUsingFactory_canCallMethodReflectively_js() {
        Set<File> files = WebComponentModulesWriter.DirectoryWriter
                .generateWebComponentsToDirectory(
                        WebComponentModulesWriter.class,
                        Collections.singleton(ExporterFactory.class),
                        outputDirectory, null);

        Assert.assertEquals("One file was created", 1, files.size());
        Assert.assertEquals("File is js module with correct name", "foo-bar.js",
                files.stream().findFirst().get().getName());
    }

    @Test
    public void directoryWriter_generateWebComponentsToDirectory_zeroExportersCreatesZeroFiles() {
        Set<File> files = WebComponentModulesWriter.DirectoryWriter
                .generateWebComponentsToDirectory(
                        WebComponentModulesWriter.class, new HashSet<>(),
                        outputDirectory, null);

        Assert.assertEquals("No files were created", 0, files.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void directoryWriter_generateWebComponentsToDirectory_nonWriterClassThrows() {
        exception.expect(IllegalArgumentException.class);
        // end of the exception
        exception.expectMessage(
                "but it is '" + MyComponent.class.getName() + "'");
        WebComponentModulesWriter.DirectoryWriter
                .generateWebComponentsToDirectory(MyComponent.class,
                        new HashSet<>(), outputDirectory, null);
    }

    @Test(expected = NullPointerException.class)
    public void directoryWriter_generateWebComponentsToDirectory_nullWriterThrows() {
        WebComponentModulesWriter.DirectoryWriter
                .generateWebComponentsToDirectory(null, new HashSet<>(),
                        outputDirectory, null);
    }

    @Test(expected = NullPointerException.class)
    public void directoryWriter_generateWebComponentsToDirectory_nullExporterSetThrows() {
        WebComponentModulesWriter.DirectoryWriter
                .generateWebComponentsToDirectory(
                        WebComponentModulesWriter.class, null, outputDirectory,
                        null);
    }

    @Test(expected = NullPointerException.class)
    public void directoryWriter_generateWebComponentsToDirectory_nullOutputDirectoryThrows() {
        WebComponentModulesWriter.DirectoryWriter
                .generateWebComponentsToDirectory(
                        WebComponentModulesWriter.class, new HashSet<>(), null,
                        null);
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
