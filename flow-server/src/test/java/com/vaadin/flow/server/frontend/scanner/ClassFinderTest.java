package com.vaadin.flow.server.frontend.scanner;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.vaadin.flow.server.frontend.scanner.ClassFinder.DefaultClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesTestComponents.Component1;

public class ClassFinderTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void should_Fail_when_DifferentClasLoader() throws Exception {
        ClassLoader loader = new ClassLoader() {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                throw new ClassNotFoundException();
            }
        };

        exception.expect(ClassNotFoundException.class);
        DefaultClassFinder finder = new DefaultClassFinder(loader, Component1.class);
        finder.loadClass(Component1.class.getName());
    }

    @Test
    public void should_LoadClasses() throws Exception {
        DefaultClassFinder finder = new DefaultClassFinder(new HashSet<>(Arrays.asList(Component1.class)));
        Assert.assertNotNull(finder.loadClass(Component1.class.getName()));
    }

    @Test
    public void should_LoadClasses_when_NoClassListProvided() throws Exception {
        DefaultClassFinder finder = new DefaultClassFinder(Collections.emptySet());
        Assert.assertNotNull(finder.loadClass(Component1.class.getName()));
    }

}
