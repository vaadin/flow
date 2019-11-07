package com.vaadin.flow.server.frontend.scanner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.vaadin.flow.server.frontend.scanner.ClassFinder.DefaultClassFinder;
import com.vaadin.flow.server.frontend.scanner.ScannerTestComponents.Component1;

public class ClassFinderTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

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
}
