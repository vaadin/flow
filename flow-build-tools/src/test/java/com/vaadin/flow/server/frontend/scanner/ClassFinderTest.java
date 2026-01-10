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
package com.vaadin.flow.server.frontend.scanner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.vaadin.flow.server.frontend.NodeTestComponents;
import com.vaadin.flow.server.frontend.scanner.ClassFinder.DefaultClassFinder;
import com.vaadin.flow.server.frontend.scanner.ScannerTestComponents.Component1;

public class ClassFinderTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private final class FakeClassLoader extends ClassLoader {
        private final ClassLoader realClassLoader;

        private FakeClassLoader(ClassLoader realClassLoader) {
            super(null);
            this.realClassLoader = realClassLoader;
        }

        protected Class<?> findClass(String name)
                throws ClassNotFoundException {
            try {
                byte[] bytes = IOUtils
                        .toByteArray(realClassLoader.getResourceAsStream(
                                name.replace(".", "/") + ".class"));
                return defineClass(name, bytes, 0, bytes.length);
            } catch (IOException e) {
                throw new ClassNotFoundException("Failed", e);
            }
        }
    }

    private static class TestList extends ArrayList<String> {

    }

    @Test
    public void should_Fail_when_DifferentClasLoader() throws Exception {
        ClassLoader loader = new ClassLoader() {
            @Override
            public Class<?> loadClass(String name)
                    throws ClassNotFoundException {
                throw new ClassNotFoundException();
            }
        };

        exception.expect(ClassNotFoundException.class);
        DefaultClassFinder finder = new DefaultClassFinder(loader,
                Component1.class);
        finder.loadClass(Component1.class.getName());
    }

    @Test
    public void should_LoadClasses() throws Exception {
        DefaultClassFinder finder = new DefaultClassFinder(
                new HashSet<>(Arrays.asList(Component1.class)));
        Assert.assertNotNull(finder.loadClass(Component1.class.getName()));
    }

    @Test
    public void should_LoadClasses_when_NoClassListProvided() throws Exception {
        DefaultClassFinder finder = new DefaultClassFinder(
                Collections.emptySet());
        Assert.assertNotNull(finder.loadClass(Component1.class.getName()));
    }

    @Test
    public void getSubTypesOf_returnsPlainSubtypes() {
        DefaultClassFinder finder = new DefaultClassFinder(new HashSet<>(
                Arrays.asList(Double.class, Integer.class, String.class)));
        Set<Class<? extends Number>> subTypes = finder
                .getSubTypesOf(Number.class);
        Assert.assertEquals(2, subTypes.size());
        Assert.assertTrue(subTypes.contains(Double.class));
        Assert.assertTrue(subTypes.contains(Integer.class));
    }

    @Test
    public void getSubTypesOf_returnsGenericSubtypes() {
        DefaultClassFinder finder = new DefaultClassFinder(new HashSet<>(
                Arrays.asList(ArrayList.class, TestList.class, String.class)));
        Set<Class<? extends List>> subTypes = finder.getSubTypesOf(List.class);
        Assert.assertEquals(2, subTypes.size());
        Assert.assertTrue(subTypes.contains(ArrayList.class));
        Assert.assertTrue(subTypes.contains(TestList.class));
    }

    @Test
    public void orderIsDeterministic() {
        Set<Class<?>> testClasses = new HashSet<>();
        testClasses.add(NodeTestComponents.ExtraImport.class);
        testClasses.add(NodeTestComponents.VaadinBowerComponent.class);
        testClasses.add(NodeTestComponents.TranslatedImports.class);
        Set<Class<?>> allClasses = new DefaultClassFinder(testClasses)
                .getSubTypesOf(Object.class);
        LinkedHashSet<Class<?>> expected = new LinkedHashSet<>();
        expected.add(NodeTestComponents.ExtraImport.class);
        expected.add(NodeTestComponents.TranslatedImports.class);
        expected.add(NodeTestComponents.VaadinBowerComponent.class);
        Assert.assertEquals(expected, allClasses);
    }

    public static class TestClass1 {

    }

    @Test
    public void defaultsToContextClassLoader() throws Exception {
        ClassLoader contextClassLoader = Thread.currentThread()
                .getContextClassLoader();

        ClassLoader loader1 = new FakeClassLoader(contextClassLoader);
        ClassLoader loader2 = new FakeClassLoader(contextClassLoader);
        Class<?> cls1 = loader1.loadClass(
                "com.vaadin.flow.server.frontend.scanner.ClassFinderTest$TestClass1");
        Class<?> cls2 = loader2.loadClass(
                "com.vaadin.flow.server.frontend.scanner.ClassFinderTest$TestClass1");

        Assert.assertEquals(loader1, cls1.getClassLoader());
        Assert.assertEquals(loader2, cls2.getClassLoader());

        DefaultClassFinder finder = new DefaultClassFinder(Set.of(cls1, cls2));
        Assert.assertEquals(contextClassLoader, finder.getClassLoader());
    }

}
