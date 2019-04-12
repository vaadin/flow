/*
 * Copyright 2000-2019 Vaadin Ltd.
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
 *
 */

package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.vaadin.flow.server.frontend.ClassPathIntrospector.ClassFinder;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.assertNotNull;

public class NodeUpdateTestBase {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    File nodeModulesPath;

    static AnnotationValuesExtractor getAnnotationValuesExtractor()
            throws MalformedURLException {
        return new AnnotationValuesExtractor(getClassFinder());
    }

    static ClassPathIntrospector.ClassFinder getClassFinder()
            throws MalformedURLException {
        return new ClassPathIntrospector.DefaultClassFinder(
                new URLClassLoader(getClassPath()),
                NodeTestComponents.class.getDeclaredClasses());
    }

    static URL[] getClassPath() throws MalformedURLException {
        // Add folder with test classes
        List<URL> classPaths = new ArrayList<>();

        classPaths.add(new File("target/test-classes").toURI().toURL());

        // Add this test jar which has some frontend resources used in tests
        URL jar = getTestResource("jar-with-frontend-resources.jar");
        classPaths.add(jar);

        // Add other paths already present in the system classpath
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        URL[] urls = ((URLClassLoader) classLoader).getURLs();
        for (URL url : urls) {
            classPaths.add(url);
        }

        return classPaths.toArray(new URL[0]);
    }

    static URL getTestResource(String resourceName) {
        URL resourceUrl = NodeUpdateTestBase.class.getClassLoader()
                .getResource(resourceName);
        assertNotNull(String.format(
                "Expect the test resource to be present in test resource folder with name = '%s'",
                resourceName), resourceUrl);
        return resourceUrl;
    }

    List<String> getExpectedImports() {
        return Arrays.asList("@polymer/iron-icon/iron-icon.js",
                "@vaadin/vaadin-lumo-styles/spacing.js",
                "@vaadin/vaadin-lumo-styles/icons.js",
                "@vaadin/vaadin-lumo-styles/style.js",
                "@vaadin/vaadin-lumo-styles/typography.js",
                "@vaadin/vaadin-lumo-styles/color.js",
                "@vaadin/vaadin-lumo-styles/sizing.js",
                "@vaadin/vaadin-date-picker/theme/lumo/vaadin-date-picker.js",
                "@vaadin/vaadin-date-picker/src/vaadin-month-calendar.js",
                "@vaadin/vaadin-element-mixin/vaadin-element-mixin.js",
                "@vaadin/vaadin-mixed-component/theme/lumo/vaadin-mixed-component.js",
                "@vaadin/vaadin-mixed-component/theme/lumo/vaadin-something-else.js",
                "@vaadin/flow-frontend/ExampleConnector.js",
                "./local-p3-template.js", "./foo.js",
                "./vaadin-mixed-component/theme/lumo/vaadin-mixed-component.js",
                "./local-p2-template.js", "./foo-dir/vaadin-npm-component.js");
    }

    void createExpectedImports(File directoryWithImportsJs,
                                       File nodeModulesPath) throws IOException {
        for (String expectedImport : getExpectedImports()) {
            File newFile = resolveImportFile(directoryWithImportsJs,
                    nodeModulesPath, expectedImport);
            newFile.getParentFile().mkdirs();
            Assert.assertTrue(newFile.createNewFile());
        }
    }

    void deleteExpectedImports(File directoryWithImportsJs,
                                       File nodeModulesPath) {
        for (String expectedImport : getExpectedImports()) {
            Assert.assertTrue(resolveImportFile(directoryWithImportsJs,
                    nodeModulesPath, expectedImport).delete());
        }
    }

    File resolveImportFile(File directoryWithImportsJs,
                                   File nodeModulesPath, String jsImport) {
        File root = jsImport.startsWith("./") ? directoryWithImportsJs
                : nodeModulesPath;
        return new File(root, jsImport);
    }

    File getFlowPackage() {
        return NodeUpdater.getFlowPackage(nodeModulesPath);
    }


    void sleep(int ms) throws InterruptedException {
        Thread.sleep(ms); // NOSONAR
    }

    ClassFinder createProxyClassFinder(ClassFinder classFinder, ClassFinder monitor) throws ClassNotFoundException {
        ClassFinder classFinderMock = Mockito.mock(ClassPathIntrospector.ClassFinder.class);

        Mockito.when(classFinderMock.getAnnotatedClasses(Mockito.any()))
                .thenAnswer(new Answer<Object>() {
                    public Object answer(InvocationOnMock invocation) {
                        Class<? extends Annotation> argument = (Class<? extends Annotation>) invocation
                                .getArguments()[0];

                        if (monitor != null) {
                            monitor.getAnnotatedClasses(argument);
                        }

                        return classFinder.getAnnotatedClasses(argument);
                    }
                });

        Mockito.when(classFinderMock.getResource(Mockito.any()))
                .thenAnswer(new Answer<Object>() {
                    public Object answer(InvocationOnMock invocation)
                            throws ClassNotFoundException {
                        String argument = (String) invocation.getArguments()[0];

                        if (monitor != null) {
                            monitor.getResource(argument);
                        }

                        return classFinder.getResource(argument);
                    }
                });

        Mockito.when(classFinderMock.loadClass(Mockito.any()))
                .thenAnswer(new Answer<Object>() {
                    public Object answer(InvocationOnMock invocation)
                            throws ClassNotFoundException {
                        String argument = (String) invocation.getArguments()[0];

                        if (monitor != null) {
                            monitor.loadClass(argument);
                        }

                        return classFinder.loadClass(argument);
                    }
                });

        Mockito.when(classFinderMock.getSubTypesOf(Mockito.any()))
                .thenAnswer(new Answer<Object>() {
                    public Object answer(InvocationOnMock invocation)
                            throws ClassNotFoundException {
                        Class argument = (Class) invocation.getArguments()[0];

                        if (monitor != null) {
                            monitor.getSubTypesOf(argument);
                        }

                        return classFinder.getSubTypesOf(argument);
                    }
                });

        return classFinderMock;
    }

}
