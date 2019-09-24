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
package com.vaadin.flow.server.frontend.scanner;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.function.SerializableBiFunction;
import com.vaadin.flow.server.frontend.NodeTestComponents;
import com.vaadin.flow.server.frontend.NodeTestComponents.ExtraImport;
import com.vaadin.flow.server.frontend.NodeTestComponents.LocalP3Template;
import com.vaadin.flow.server.frontend.NodeTestComponents.LumoTest;
import com.vaadin.flow.server.frontend.NodeTestComponents.VaadinElementMixin;
import com.vaadin.flow.server.frontend.NodeTestComponents.VaadinShrinkWrap;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.NoTheme;
import com.vaadin.flow.theme.Theme;

public class FullDependenciesScannerTest {

    private ClassFinder finder = Mockito.mock(ClassFinder.class);
    private FullDependenciesScanner scanner;

    public static class FakeLumoTheme implements AbstractTheme {

        @Override
        public String getBaseUrl() {
            return "foo";
        }

        @Override
        public String getThemeUrl() {
            return null;
        }

    }

    @NoTheme
    private static class NoThemeComponent extends Component {

    }

    @Before
    public void setUp() throws ClassNotFoundException {
        Mockito.when(finder.loadClass(AbstractTheme.class.getName()))
                .thenReturn((Class) AbstractTheme.class);
    }

    @Test
    public void getTheme_noExplicitTheme_lumoThemeIsDiscovered()
            throws ClassNotFoundException {
        setUpThemeScanner(Collections.emptySet(), Collections.emptySet(),
                (type, annotationType) -> Collections.emptyList());

        Mockito.verify(finder).loadClass(AbstractTheme.class.getName());

        Assert.assertNotNull(scanner.getTheme());
        Assert.assertEquals("foo", scanner.getTheme().getBaseUrl());
        Assert.assertEquals(FakeLumoTheme.class,
                scanner.getThemeDefinition().getTheme());
        Assert.assertEquals("", scanner.getThemeDefinition().getVariant());

        Assert.assertEquals(0, scanner.getClasses().size());
    }

    @Test
    public void getTheme_noTheme_noThemeIsDiscovered()
            throws ClassNotFoundException {
        setUpThemeScanner(Collections.emptySet(),
                Collections.singleton(NoThemeComponent.class),
                (type, annotationType) -> Collections.emptyList());

        Mockito.verify(finder).loadClass(AbstractTheme.class.getName());

        Assert.assertNull(scanner.getTheme());
        Assert.assertNull(scanner.getThemeDefinition());
        Assert.assertEquals(0, scanner.getClasses().size());
    }

    @Test
    public void getTheme_explicitTheme_themeIsDiscovered()
            throws ClassNotFoundException {
        Mockito.when(finder.loadClass(LumoTest.class.getName()))
                .thenReturn((Class) LumoTest.class);

        setUpThemeScanner(getAnnotatedClasses(Theme.class),
                Collections.emptySet(),
                (type, annotationType) -> findAnnotations(type, Theme.class));

        Mockito.verify(finder).loadClass(AbstractTheme.class.getName());

        Assert.assertNotNull(scanner.getTheme());
        Assert.assertEquals("theme/lumo/", scanner.getTheme().getThemeUrl());
        Assert.assertEquals(LumoTest.class,
                scanner.getThemeDefinition().getTheme());
        Assert.assertEquals("dark", scanner.getThemeDefinition().getVariant());
        Assert.assertEquals(0, scanner.getClasses().size());
    }

    @Test
    public void getPackages_returnsAllPackages_getClassesReturnAllPackageAnnotatedComponents()
            throws ClassNotFoundException {
        // use this fake/mock class for the loaded class to check that annotated
        // classes are requested for the loaded class and not for the
        // NpmPackage.class
        Class clazz = Object.class;

        Mockito.when(finder.loadClass(NpmPackage.class.getName()))
                .thenReturn(clazz);

        Mockito.when(finder.getAnnotatedClasses(clazz))
                .thenReturn(getAnnotatedClasses(NpmPackage.class));

        scanner = new FullDependenciesScanner(finder,
                (type, annotation) -> findAnnotations(type, NpmPackage.class));

        Map<String, String> packages = scanner.getPackages();

        Assert.assertEquals(packages.get("@vaadin/vaadin-button"), "1.1.1");
        Assert.assertEquals(packages.get("@vaadin/vaadin-element-mixin"),
                "1.1.2");
        Assert.assertEquals(packages.get("@foo/var-component"), "1.1.0");
        Assert.assertEquals(packages.get("@webcomponents/webcomponentsjs"),
                "2.2.9");
        Assert.assertEquals(packages.get("@vaadin/vaadin-shrinkwrap"), "1.2.3");

        Assert.assertEquals(5, packages.size());

        Set<String> visitedClasses = scanner.getClasses();
        Assert.assertTrue(
                visitedClasses.contains(VaadinShrinkWrap.class.getName()));
        Assert.assertTrue(
                visitedClasses.contains(LocalP3Template.class.getName()));
        Assert.assertTrue(visitedClasses
                .contains(NodeTestComponents.BUTTON_COMPONENT_FQN));
        Assert.assertTrue(
                visitedClasses.contains(VaadinElementMixin.class.getName()));
        Assert.assertTrue(visitedClasses.contains(ExtraImport.class.getName()));
    }

    private List<? extends Annotation> findAnnotations(Class<?> type,
            Class<? extends Annotation> annotationType) {
        return Arrays.asList(type.getAnnotationsByType(annotationType));
    }

    private Set<Class<?>> getAnnotatedClasses(
            Class<? extends Annotation> annotationType) {
        Class<?>[] classes = NodeTestComponents.class.getDeclaredClasses();
        Set<Class<?>> result = new HashSet<>();
        for (Class<?> clazz : classes) {
            if (clazz.getAnnotationsByType(annotationType).length > 0) {
                result.add(clazz);
            }
        }
        return result;
    }

    private void setUpThemeScanner(Set<Class<?>> themedClasses,
            Set<Class<?>> noThemeClasses,
            SerializableBiFunction<Class<?>, Class<? extends Annotation>, List<? extends Annotation>> annotationFinder)
            throws ClassNotFoundException {
        Class fakeThemeClass = Object.class;
        Class fakeNoThemeClass = Throwable.class;

        Mockito.when(finder.loadClass(Theme.class.getName()))
                .thenReturn(fakeThemeClass);
        Mockito.when(finder.loadClass(NoTheme.class.getName()))
                .thenReturn(fakeNoThemeClass);

        Mockito.when(finder.getAnnotatedClasses(fakeThemeClass))
                .thenReturn(themedClasses);
        Mockito.when(finder.getAnnotatedClasses(fakeNoThemeClass))
                .thenReturn(noThemeClasses);

        scanner = new FullDependenciesScanner(finder, annotationFinder) {
            @Override
            protected Class<? extends AbstractTheme> getLumoTheme() {
                return FakeLumoTheme.class;
            }
        };
    }
}
