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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.function.SerializableBiFunction;
import com.vaadin.flow.server.frontend.NodeTestComponents;
import com.vaadin.flow.server.frontend.NodeTestComponents.ExtraImport;
import com.vaadin.flow.server.frontend.NodeTestComponents.FlatImport;
import com.vaadin.flow.server.frontend.NodeTestComponents.JavaScriptOrder;
import com.vaadin.flow.server.frontend.NodeTestComponents.LocalP3Template;
import com.vaadin.flow.server.frontend.NodeTestComponents.LocalTemplate;
import com.vaadin.flow.server.frontend.NodeTestComponents.LumoTest;
import com.vaadin.flow.server.frontend.NodeTestComponents.MainLayout;
import com.vaadin.flow.server.frontend.NodeTestComponents.UnresolvedComponent;
import com.vaadin.flow.server.frontend.NodeTestComponents.VaadinBowerComponent;
import com.vaadin.flow.server.frontend.NodeTestComponents.VaadinElementMixin;
import com.vaadin.flow.server.frontend.NodeTestComponents.VaadinMixedComponent;
import com.vaadin.flow.server.frontend.NodeTestComponents.VaadinNpmComponent;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.NoTheme;
import com.vaadin.flow.theme.Theme;

public class FullDependenciesScannerTest {

    private ClassFinder finder;

    @JsModule("./foo-bar-baz.js")
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

    @Theme(themeClass = FakeLumoTheme.class)
    public static class ThemedComponent extends Component {

    }

    @NoTheme
    private static class NoThemeComponent extends Component {

    }

    @NoTheme
    private static class NoThemeComponent1 extends Component {

    }

    @Before
    public void setUp() throws ClassNotFoundException {
        finder = Mockito.mock(ClassFinder.class);
        Mockito.when(finder.loadClass(AbstractTheme.class.getName()))
                .thenReturn((Class) AbstractTheme.class);
    }

    @Test
    public void getTheme_noExplicitTheme_noThemeIsDiscovered()
            throws ClassNotFoundException {
        FrontendDependenciesScanner scanner = setUpThemeScanner(
                Collections.emptySet(), Collections.emptySet(),
                (type, annotationType) -> Collections.emptyList());

        Mockito.verify(finder).loadClass(AbstractTheme.class.getName());

        Assert.assertNull(scanner.getTheme());
        Assert.assertEquals(0, scanner.getClasses().size());
    }

    @Test
    public void getTheme_noTheme_noThemeIsDiscovered()
            throws ClassNotFoundException {
        FrontendDependenciesScanner scanner = setUpThemeScanner(
                Collections.emptySet(),
                new HashSet<>(Arrays.asList(NoThemeComponent.class,
                        NoThemeComponent1.class)),
                (type, annotationType) -> Collections.emptyList());

        Mockito.verify(finder).loadClass(AbstractTheme.class.getName());

        Assert.assertNull(scanner.getTheme());
        Assert.assertNull(scanner.getThemeDefinition());
        Assert.assertEquals(0, scanner.getClasses().size());
        Assert.assertFalse(DepsTests.merge(scanner.getModules())
                .contains("./foo-bar-baz.js"));
    }

    @Test
    public void getTheme_explicitTheme_themeIsDiscovered()
            throws ClassNotFoundException {
        Mockito.when(finder.loadClass(LumoTest.class.getName()))
                .thenReturn((Class) LumoTest.class);

        FrontendDependenciesScanner scanner = setUpThemeScanner(
                getAnnotatedClasses(Theme.class), Collections.emptySet(),
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
    public void themeWithCssDefined_cssLoaded() throws ClassNotFoundException {
        Mockito.when(finder.loadClass(LumoTest.class.getName()))
                .thenReturn((Class) LumoTest.class);
        Mockito.when(finder.loadClass(CssImport.class.getName()))
                .thenReturn((Class) CssImport.class);

        Mockito.when(finder.getAnnotatedClasses(CssImport.class))
                .thenReturn(getAnnotatedClasses(CssImport.class));

        FrontendDependenciesScanner scanner = setUpThemeScanner(
                getAnnotatedClasses(Theme.class), Collections.emptySet(),
                (type, annotationType) -> {
                    if (annotationType.equals(CssImport.class)) {
                        return findAnnotations(type, CssImport.class);
                    }
                    return findAnnotations(type, Theme.class);
                });

        Mockito.verify(finder).loadClass(AbstractTheme.class.getName());

        Assert.assertNotNull(scanner.getTheme());
        Assert.assertTrue(DepsTests.merge(scanner.getCss()).stream()
                .map(CssData::getValue).toList()
                .contains("lumo-css-import.css"));
    }

    @Test
    public void themeWithCssOnClassPath_notDefinedAsTheme_cssNotLoaded()
            throws ClassNotFoundException {
        Mockito.when(finder.loadClass(LumoTest.class.getName()))
                .thenReturn((Class) LumoTest.class);
        Mockito.when(finder.loadClass(CssImport.class.getName()))
                .thenReturn((Class) CssImport.class);

        Set<Class<?>> result = new LinkedHashSet<>();
        for (Class<?> clazz : Arrays.asList(NoThemeComponent.class,
                NoThemeComponent1.class, FakeLumoTheme.class)) {
            if (clazz.getAnnotationsByType(CssImport.class).length > 0) {
                result.add(clazz);
            }
        }

        Mockito.when(finder.getAnnotatedClasses(CssImport.class))
                .thenReturn(result);

        FrontendDependenciesScanner scanner = new FullDependenciesScanner(
                finder,
                (type, annotation) -> findAnnotations(type, CssImport.class),
                null, true);

        Mockito.verify(finder).loadClass(AbstractTheme.class.getName());

        Assert.assertNull(scanner.getTheme());
        Assert.assertFalse(DepsTests.merge(scanner.getCss()).stream()
                .map(CssData::getValue).toList()
                .contains("lumo-css-import.css"));
    }

    @Test(expected = IllegalStateException.class)
    public void getTheme_noThemeAndExplicitTheme_throws()
            throws ClassNotFoundException {
        setUpThemeScanner(getAnnotatedClasses(Theme.class),
                Collections.singleton(NoThemeComponent.class),
                (type, annotationType) -> findAnnotations(type, Theme.class));
    }

    @Test(expected = IllegalStateException.class)
    public void getTheme_severalExplicitThemes_throws()
            throws ClassNotFoundException {
        Set<Class<?>> themeAnnotatedClasses = getAnnotatedClasses(Theme.class);
        themeAnnotatedClasses.add(ThemedComponent.class);
        setUpThemeScanner(themeAnnotatedClasses, Collections.emptySet(),
                (type, annotationType) -> findAnnotations(type, Theme.class));
    }

    @Test
    public void getPackages_returnsAllPackages_getClassesReturnAllPackageAnnotatedComponents()
            throws ClassNotFoundException {
        FrontendDependenciesScanner scanner = setUpAnnotationScanner(
                NpmPackage.class);

        Map<String, String> packages = scanner.getPackages();

        Assert.assertEquals(packages.get("@vaadin/vaadin-button"), "1.1.1");
        Assert.assertEquals(packages.get("@vaadin/vaadin-element-mixin"),
                "1.1.2");
        Assert.assertEquals(packages.get("@foo/var-component"), "1.1.0");
        Assert.assertEquals(packages.get("@webcomponents/webcomponentsjs"),
                "2.2.10");

        Assert.assertEquals(4, packages.size());

        Set<String> visitedClasses = scanner.getClasses();
        Assert.assertTrue(
                visitedClasses.contains(LocalP3Template.class.getName()));
        Assert.assertTrue(visitedClasses
                .contains(NodeTestComponents.BUTTON_COMPONENT_FQN));
        Assert.assertTrue(
                visitedClasses.contains(VaadinElementMixin.class.getName()));
        Assert.assertTrue(visitedClasses.contains(ExtraImport.class.getName()));
    }

    @Test
    public void getAllPackages_returnsAllPackages_packagesAreOrganized_getClassesReturnAllPackageAnnotatedComponents()
            throws ClassNotFoundException {
        FrontendDependenciesScanner scanner = setUpAnnotationScanner(
                NpmPackage.class);

        Map<String, String> packages = scanner.getPackages();
        Map<String, String> devPackages = scanner.getDevPackages();

        Assert.assertEquals(packages.get("@vaadin/vaadin-button"), "1.1.1");
        Assert.assertEquals(packages.get("@vaadin/vaadin-element-mixin"),
                "1.1.2");
        Assert.assertEquals(packages.get("@foo/var-component"), "1.1.0");
        Assert.assertEquals(packages.get("@webcomponents/webcomponentsjs"),
                "2.2.10");

        Assert.assertEquals(devPackages.get("vite-plugin-pwa"), "0.16.5");

        Assert.assertEquals(4, packages.size());
        Assert.assertEquals(1, devPackages.size());

        Set<String> visitedClasses = scanner.getClasses();
        Assert.assertTrue(
                visitedClasses.contains(LocalP3Template.class.getName()));
        Assert.assertTrue(visitedClasses
                .contains(NodeTestComponents.BUTTON_COMPONENT_FQN));
        Assert.assertTrue(
                visitedClasses.contains(VaadinElementMixin.class.getName()));
        Assert.assertTrue(visitedClasses.contains(ExtraImport.class.getName()));
    }

    @Test
    public void getScripts_returnAllScripts_orderPerClassIsPreserved_getClassesReturnAllJSAnnotatedComponents()
            throws ClassNotFoundException {
        FrontendDependenciesScanner scanner = setUpAnnotationScanner(
                JavaScript.class);

        DepsTests.assertImports(scanner.getScripts(), "javascript/a.js",
                "javascript/b.js", "javascript/c.js",
                "experimental-Connector.js", "ExampleConnector.js");

        Set<String> visitedClasses = scanner.getClasses();
        Assert.assertTrue(
                visitedClasses.contains(VaadinBowerComponent.class.getName()));
        Assert.assertTrue(
                visitedClasses.contains(JavaScriptOrder.class.getName()));
    }

    @Test
    public void getScripts_returnAllJsModules_orderPerClassIsPreserved_getClassesReturnAllJSAnnotatedComponents()
            throws ClassNotFoundException {
        List<String> expectedModulesInOrder = new ArrayList<>();
        expectedModulesInOrder.add("jsmodule/h.js");
        expectedModulesInOrder.add("jsmodule/g.js");
        FrontendDependenciesScanner scanner = setUpAnnotationScanner(
                JsModule.class);
        DepsTests.assertImportsWithFilter(scanner.getModules(),
                jsmodule -> expectedModulesInOrder.contains(jsmodule),
                expectedModulesInOrder.toArray(String[]::new));

        Set<String> visitedClasses = scanner.getClasses();
        Assert.assertTrue(
                visitedClasses.contains(VaadinBowerComponent.class.getName()));
        Assert.assertTrue(
                visitedClasses.contains(JavaScriptOrder.class.getName()));
    }

    @Test
    public void getCss_returnAllCss_orderPerClassIsPreserved_getClassesReturnAllCssAnnotatedComponents()
            throws ClassNotFoundException {
        FrontendDependenciesScanner scanner = setUpAnnotationScanner(
                CssImport.class);

        List<CssData> expected = new ArrayList<>();
        expected.add(createCssData("@vaadin/vaadin-mixed-component/bar.css",
                null, null, null));
        expected.add(createCssData("./foo.css", null, null, null));
        expected.add(createCssData("./foo.css", null, "bar", null));
        expected.add(createCssData("./foo.css", "baz", null, null));
        expected.add(createCssData("./foo.css", "baz", "bar", null));
        expected.add(createCssData("./foo.css", null, null, "foo-bar"));
        expected.add(createCssData("./foo.css", null, "bar", "foo-bar"));

        DepsTests.assertCss(scanner.getCss(), expected);
        Set<String> visitedClasses = scanner.getClasses();
        Assert.assertEquals(2, visitedClasses.size());
        Assert.assertTrue(visitedClasses.contains(FlatImport.class.getName()));
        Assert.assertTrue(visitedClasses.contains(LumoTest.class.getName()));
    }

    @Test
    public void getModules_noTheme_returnAllModules_orderPerClassIsPreserved_getClassesReturnAllModuleAnnotatedComponents()
            throws ClassNotFoundException {
        FrontendDependenciesScanner scanner = setUpAnnotationScanner(
                JsModule.class);
        DepsTests.assertImportCount(20, scanner.getModules());
        assertJsModules(DepsTests.merge(scanner.getModules()));

        Set<String> classes = scanner.getClasses();
        Assert.assertEquals(14, classes.size());

        assertJsModulesClasses(classes);
    }

    @Test
    public void getModules_explcitTheme_returnAllModulesExcludingNotUsedTheme_getClassesReturnAllModuleAnnotatedComponents()
            throws ClassNotFoundException {
        // use this fake/mock class for the loaded class to check that annotated
        // classes are requested for the loaded class and not for the
        // annotationType
        Class clazz = Object.class;

        Mockito.when(finder.loadClass(JsModule.class.getName()))
                .thenReturn(clazz);

        Mockito.when(finder.getAnnotatedClasses(clazz))
                .thenReturn(getAnnotatedClasses(JsModule.class));

        Class themeClass = Throwable.class;

        Mockito.when(finder.loadClass(Theme.class.getName()))
                .thenReturn(themeClass);

        Set<Class<?>> themeClasses = getAnnotatedClasses(Theme.class);
        themeClasses.add(FakeLumoTheme.class);
        Mockito.when(finder.getAnnotatedClasses(themeClass))
                .thenReturn(themeClasses);
        Assert.assertTrue(themeClasses.size() >= 2);

        Mockito.when(finder.loadClass(LumoTest.class.getName()))
                .thenReturn((Class) LumoTest.class);
        Mockito.when(finder.loadClass(FakeLumoTheme.class.getName()))
                .thenReturn((Class) FakeLumoTheme.class);

        FrontendDependenciesScanner scanner = new FullDependenciesScanner(
                finder, (type, annotation) -> {
                    if (annotation.equals(clazz)) {
                        return findAnnotations(type, JsModule.class);
                    } else if (annotation.equals(themeClass)) {
                        return findAnnotations(type, Theme.class);
                    }
                    Assert.fail();
                    return null;
                }, null, true);

        DepsTests.assertImportCount(28, scanner.getModules());
        List<String> modules = DepsTests.merge(scanner.getModules());
        assertJsModules(modules);

        // Theme modules should be included now
        Assert.assertTrue(
                modules.contains("@vaadin/vaadin-lumo-styles/color.js"));
        Assert.assertTrue(
                modules.contains("@vaadin/vaadin-lumo-styles/typography.js"));
        Assert.assertTrue(
                modules.contains("@vaadin/vaadin-lumo-styles/sizing.js"));
        Assert.assertTrue(
                modules.contains("@vaadin/vaadin-lumo-styles/spacing.js"));
        Assert.assertTrue(
                modules.contains("@vaadin/vaadin-lumo-styles/style.js"));
        Assert.assertTrue(
                modules.contains("@vaadin/vaadin-lumo-styles/icons.js"));

        // not used theme module is not included
        Assert.assertFalse(modules.contains("./foo-bar-baz.js"));

        Set<String> classes = scanner.getClasses();
        Assert.assertEquals(14, classes.size());

        assertJsModulesClasses(classes);
        Assert.assertTrue(classes.contains(LumoTest.class.getName()));
        Assert.assertFalse(classes.contains(FakeLumoTheme.class.getName()));
    }

    private CssData createCssData(String value, String id, String include,
            String themefor) {
        return new CssData(value, id, include, themefor);
    }

    private List<? extends Annotation> findAnnotations(Class<?> type,
            Class<? extends Annotation> annotationType) {
        return Arrays.asList(type.getAnnotationsByType(annotationType));
    }

    private Set<Class<?>> getAnnotatedClasses(
            Class<? extends Annotation> annotationType) {
        Class<?>[] classes = NodeTestComponents.class.getDeclaredClasses();
        Set<Class<?>> result = new LinkedHashSet<>();
        for (Class<?> clazz : classes) {
            if (clazz.getAnnotationsByType(annotationType).length > 0) {
                result.add(clazz);
            }
        }
        return result;
    }

    private FullDependenciesScanner setUpAnnotationScanner(
            Class<? extends Annotation> annotationType)
            throws ClassNotFoundException {
        // use this fake/mock class for the loaded class to check that annotated
        // classes are requested for the loaded class and not for the
        // annotationType
        Class clazz = Object.class;

        Mockito.when(finder.loadClass(annotationType.getName()))
                .thenReturn(clazz);

        Mockito.when(finder.getAnnotatedClasses(clazz))
                .thenReturn(getAnnotatedClasses(annotationType));

        return new FullDependenciesScanner(finder,
                (type, annotation) -> findAnnotations(type, annotationType),
                null, true);
    }

    private FullDependenciesScanner setUpThemeScanner(
            Set<Class<?>> themedClasses, Set<Class<?>> noThemeClasses,
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

        return new FullDependenciesScanner(finder, annotationFinder, null,
                true) {
        };
    }

    private void assertJsModules(List<String> modules) {
        Assert.assertTrue(modules.contains("@polymer/iron-icon/iron-icon.js"));
        Assert.assertTrue(modules.contains(
                "@vaadin/vaadin-date-picker/src/vaadin-date-picker.js"));
        Assert.assertTrue(modules.contains(
                "@vaadin/vaadin-date-picker/src/vaadin-month-calendar.js"));
        Assert.assertTrue(modules.contains(
                "@vaadin/vaadin-element-mixin/vaadin-element-mixin.js"));
        Assert.assertTrue(
                modules.contains("./foo-dir/vaadin-npm-component.js"));
        Assert.assertTrue(modules.contains(
                "vaadin-mixed-component/src/vaadin-mixed-component.js"));
        Assert.assertTrue(modules.contains("./local-template.js"));
        Assert.assertTrue(modules.contains("3rdparty/component.js"));
        Assert.assertTrue(modules.contains("./local-p3-template.js"));
        Assert.assertTrue(modules.contains("unresolved/component"));
        Assert.assertTrue(modules.contains("./foo.js"));
        Assert.assertTrue(modules.contains(
                "@vaadin/vaadin-mixed-component/src/vaadin-mixed-component.js"));
        Assert.assertTrue(modules.contains(
                "@vaadin/vaadin-mixed-component/src/vaadin-something-else.js"));
        Assert.assertTrue(modules.contains(
                "@vaadin/vaadin-mixed-component/src/vaadin-something-else"));
        Assert.assertTrue(modules.contains(
                "@vaadin/vaadin-mixed-component/src/vaadin-custom-themed-component.js"));
        Assert.assertTrue(modules.contains("./common-js-file.js"));
        Assert.assertTrue(modules.contains("jsmodule/g.js"));

        // Check the order for VaadinBowerComponent class
        List<String> modulesPerClass = modules.stream().filter(
                module -> module.startsWith("@vaadin/vaadin-date-picker"))
                .collect(Collectors.toList());
        Assert.assertEquals(
                "@vaadin/vaadin-date-picker/src/vaadin-date-picker.js",
                modulesPerClass.get(0));
        Assert.assertEquals(
                "@vaadin/vaadin-date-picker/src/vaadin-month-calendar.js",
                modulesPerClass.get(1));

        // Check the order for TranslatedImports class
        modulesPerClass = modules.stream().filter(
                module -> module.startsWith("@vaadin/vaadin-mixed-component"))
                .collect(Collectors.toList());
        Assert.assertEquals(
                "@vaadin/vaadin-mixed-component/src/vaadin-mixed-component.js",
                modulesPerClass.get(0));
        Assert.assertEquals(
                "@vaadin/vaadin-mixed-component/src/vaadin-something-else.js",
                modulesPerClass.get(1));
        Assert.assertEquals(
                "@vaadin/vaadin-mixed-component/src/vaadin-something-else",
                modulesPerClass.get(2));
        Assert.assertEquals(
                "@vaadin/vaadin-mixed-component/src/vaadin-custom-themed-component.js",
                modulesPerClass.get(3));
    }

    private void assertJsModulesClasses(Set<String> classes) {
        Assert.assertTrue(
                classes.contains(NodeTestComponents.ICON_COMPONENT_FQN));
        Assert.assertTrue(
                classes.contains(VaadinBowerComponent.class.getName()));
        Assert.assertTrue(classes.contains(VaadinElementMixin.class.getName()));
        Assert.assertTrue(classes.contains(VaadinNpmComponent.class.getName()));
        Assert.assertTrue(
                classes.contains(VaadinMixedComponent.class.getName()));
        Assert.assertTrue(classes.contains(LocalTemplate.class.getName()));
        Assert.assertTrue(classes.contains(LocalP3Template.class.getName()));
        Assert.assertTrue(
                classes.contains(UnresolvedComponent.class.getName()));
        Assert.assertTrue(classes.contains(FlatImport.class.getName()));
        Assert.assertTrue(classes.contains(MainLayout.class.getName()));
        Assert.assertTrue(classes.contains(JavaScriptOrder.class.getName()));
    }
}
