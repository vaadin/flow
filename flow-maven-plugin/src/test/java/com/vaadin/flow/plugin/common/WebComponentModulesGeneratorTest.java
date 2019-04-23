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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.plugin.samplecode.AbstractExporter;
import com.vaadin.flow.plugin.samplecode.BarExporter;
import com.vaadin.flow.plugin.samplecode.FooExporter;

public class WebComponentModulesGeneratorTest {

    private static final String FOO_EXPORTER_FQN = FooExporter.class.getName();

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private ClassPathIntrospector introspector;

    private WebComponentModulesGenerator generator;

    @Before
    public void init() {
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

        introspector = new ClassPathIntrospector(urls.toArray(new URL[0])) {
        };
        generator = new WebComponentModulesGenerator(introspector);
    }

    @Test
    public void getExporters_exportersAreDiscovered() throws IOException {
        Set<String> exporterFQNs = generator.getExporters()
                .filter(clazz -> clazz.getPackage().getName()
                        .equals(AbstractExporter.class.getPackage().getName()))
                .map(Class::getName).collect(Collectors.toSet());

        Assert.assertEquals(2, exporterFQNs.size());

        Assert.assertTrue(
                "FooExporter class is not discovered as an exporter class",
                exporterFQNs.contains(FOO_EXPORTER_FQN));
        Assert.assertTrue(
                "BarExporter class is not discovered as an exporter class",
                exporterFQNs.contains(BarExporter.class.getName()));
    }

    @Test
    public void generateModuleFile_fileIsGenerated() throws IOException {
        Optional<Class<? extends WebComponentExporter<? extends Component>>> exporter = generator
                .getExporters()
                .filter(clazz -> clazz.getName().equals(FOO_EXPORTER_FQN))
                .findFirst();

        File generateFile = generator.generateModuleFile(exporter.get(),
                temporaryFolder.getRoot());

        String content = FileUtils.readFileToString(generateFile,
                StandardCharsets.UTF_8);

        Assert.assertThat(
                "Generated module doesn't contain 'age' property definition",
                content,
                CoreMatchers.containsString("\"age\":{\"type\":\"Integer\""));

        Assert.assertThat(
                "Generated module doesn't contain polymer-element import",
                content, CoreMatchers.containsString(
                        "bower_components/polymer/polymer-element.html"));

        Assert.assertThat(
                "Generated module doesn't contain element registration",
                content,
                CoreMatchers.allOf(
                        CoreMatchers
                                .containsString("<dom-module id=\"wc-foo\">"),
                        CoreMatchers.containsString(
                                "customElements.define(WcFoo.is, WcFoo);"),
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
}
