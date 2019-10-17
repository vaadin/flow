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
package com.vaadin.flow.plugin.common;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.internal.ExportsWebComponent;
import com.vaadin.flow.migration.ClassPathIntrospector;
import com.vaadin.flow.plugin.samplecode.AbstractExporter;
import com.vaadin.flow.plugin.samplecode.BarExporter;
import com.vaadin.flow.plugin.samplecode.FooExporter;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.webcomponent.WebComponentModulesWriter;

import static org.mockito.Mockito.mock;

public class WebComponentModulesGeneratorTest {

    private static final String FOO_EXPORTER_FQN = FooExporter.class.getName();

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private ClassPathIntrospector introspector;

    private WebComponentModulesGenerator generator;

    @Before
    public void init() throws ClassNotFoundException {
        List<URL> urls = new ArrayList<>();
        for (String path : getRawClasspathEntries()) {
            if (path.contains("api")
                    || (path.contains("maven")
                            && !path.contains("flow-maven-plugin"))
                    || path.contains("plexus") || path.contains("eclipse")
                    || path.contains("helger") || path.contains("com/google")) {
                continue;
            }
            File file = new File(path);
            try {
                urls.add(file.toURI().toURL());
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
    }


    @Test
    public void getExporters_exportersAreDiscovered() throws ClassNotFoundException {
        // prepare
        ClassFinder finder =
                getMockFinderWithExporterClasses(FooExporter.class,
                        BarExporter.class, AbstractExporter.class);

        introspector = new ClassPathIntrospector(finder) {};
        generator = new WebComponentModulesGenerator(introspector);

        // act
        Set<File> files =
                generator.generateWebComponentModules(temporaryFolder.getRoot());

        // verify
        Assert.assertEquals(2, files.size());

        Assert.assertTrue(
                "FooExporter class is not discovered as an exporter class",
                files.stream().anyMatch(file -> file.getName().contains("wc-foo")));
        Assert.assertTrue(
                "BarExporter class is not discovered as an exporter class",
                files.stream().anyMatch(file -> file.getName().contains("wc-bar")));
    }


    @Test
    public void generateModuleFile_fileIsGenerated() throws IOException, ClassNotFoundException {
        // prepare
        ClassFinder finder =
                getMockFinderWithExporterClasses(FooExporter.class);
        introspector = new ClassPathIntrospector(finder) {};
        generator = new WebComponentModulesGenerator(introspector);

        // act
        Set<File> files =
                generator.generateWebComponentModules(temporaryFolder.getRoot());

        Assert.assertEquals("One file should have been generated", 1,
                files.size());

        File generateFile = files.stream().findFirst().get();

        String content = FileUtils.readFileToString(generateFile,
                StandardCharsets.UTF_8);

        // verify
        Assert.assertThat(
                "Generated module doesn't contain 'age' property default",
                content, CoreMatchers.containsString("this['_age'] = 1;"));

        Assert.assertThat("Generated module doesn't contain 'age' setter",
                content,
                // split due to windows env
                CoreMatchers.allOf(
                        CoreMatchers.containsString("set ['age'](value) {"),
                        CoreMatchers.containsString("if (this['_age'] === value)"),
                        CoreMatchers.containsString("this['_age'] = value;")));

        Assert.assertThat(
                "Generated module doesn't contain element registration",
                content,
                CoreMatchers.allOf(
                        CoreMatchers.containsString(
                                "customElements.define('wc-foo', WcFoo);"),
                        CoreMatchers.containsString("class WcFoo extends")));

        Assert.assertThat("Generated module contains UI import", content,
                CoreMatchers.not(
                        CoreMatchers.containsString("web-component-ui.html")));
    }

    private static List<String> getRawClasspathEntries() {
        String pathSep = System.getProperty("path.separator");
        String classpath = System.getProperty("java.class.path");

        if (classpath.startsWith("\"")) {
            classpath = classpath.substring(1);
        }
        if (classpath.endsWith("\"")) {
            classpath = classpath.substring(0, classpath.length() - 1);
        }

        String[] split = classpath.split(pathSep);
        return Arrays.asList(split);
    }

    private static ClassFinder getMockFinderWithExporterClasses(Class<?
            extends WebComponentExporter>... exporters) throws ClassNotFoundException {
        ClassFinder finder = mock(ClassFinder.class);
        Mockito.when(finder.loadClass(WebComponentModulesWriter.class.getName()))
                .thenReturn((Class)WebComponentModulesWriter.class);
        Mockito.when(finder.loadClass(ExportsWebComponent.class.getName()))
                .thenReturn((Class) ExportsWebComponent.class);
        Mockito.when(finder.getSubTypesOf(ExportsWebComponent.class))
                .thenReturn(Stream.of(exporters).collect(Collectors.toSet()));
        return finder;
    }
}
