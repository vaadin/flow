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

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.webcomponent.WebComponent;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.UIInitListener;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.frontend.scanner.samples.ErrorComponent;
import com.vaadin.flow.server.frontend.scanner.samples.JsModuleOrderComponent;
import com.vaadin.flow.server.frontend.scanner.samples.JsOrderComponent;
import com.vaadin.flow.server.frontend.scanner.samples.MyServiceListener;
import com.vaadin.flow.server.frontend.scanner.samples.MyUIInitListener;
import com.vaadin.flow.server.frontend.scanner.samples.RouteComponent;
import com.vaadin.flow.server.frontend.scanner.samples.RouteComponentWithMethodReference;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.ThemeDefinition;

public class FrontendDependenciesTest {

    private ClassFinder classFinder = Mockito.mock(ClassFinder.class);

    @Before
    public void setUp() throws ClassNotFoundException {
        Mockito.when(classFinder.loadClass(Mockito.anyString()))
                .thenAnswer(q -> {
                    String className = q.getArgument(0);
                    if (className.equals(FrontendDependencies.LUMO)) {
                        return FakeLumo.class;
                    }
                    return Class.forName(className);
                });

        Mockito.doAnswer(invocation -> FrontendDependenciesTest.class
                .getClassLoader().getResource(invocation.getArgument(0)))
                .when(classFinder).getResource(Mockito.anyString());
        Mockito.when(classFinder.shouldInspectClass(Mockito.anyString()))
                .thenReturn(true);

        Mockito.when(classFinder.loadClass(UI.class.getName()))
                .thenReturn((Class) UI.class);
    }

    @Test
    public void routedComponent_entryPointsAreCollected() {
        Mockito.when(classFinder.getAnnotatedClasses(Route.class))
                .thenReturn(Collections.singleton(RouteComponent.class));
        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false, null, true);

        DepsTests.assertImportsExcludingUI(dependencies.getModules(), "foo.js");
        DepsTests.assertImports(dependencies.getScripts(), "bar.js");
    }

    @Test
    public void appShellConfigurator_collectedAsEntryPoint()
            throws ClassNotFoundException {
        Mockito.when(classFinder.getSubTypesOf(AppShellConfigurator.class))
                .thenReturn(Collections.singleton(MyAppShell.class));
        Mockito.when(classFinder.loadClass(FakeLumo.class.getName()))
                .thenReturn((Class) FakeLumo.class);

        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false, null, true);

        Assert.assertEquals("UI, AppShell should be found", 2,
                dependencies.getEntryPoints().size());

        AbstractTheme theme = dependencies.getTheme();
        Assert.assertNotNull("Theme not found in entry point", theme);

        ThemeDefinition themeDefinition = dependencies.getThemeDefinition();
        Assert.assertNotNull("ThemeDefinition is not filled", themeDefinition);
        Assert.assertEquals(FakeLumo.class, themeDefinition.getTheme());
    }

    @Test
    public void themeDefiningClassAndName_throwsException()
            throws ClassNotFoundException {
        Mockito.when(classFinder.getSubTypesOf(AppShellConfigurator.class))
                .thenReturn(Collections.singleton(FaultyThemeAnnotation.class));
        Mockito.when(classFinder.loadClass(FakeLumo.class.getName()))
                .thenReturn((Class) FakeLumo.class);

        IllegalStateException exception = Assert.assertThrows(
                IllegalStateException.class,
                () -> new FrontendDependencies(classFinder, false, null, true));

        Assert.assertEquals("Unexpected message for the thrown exception",
                "Theme name and theme class can not both be specified. "
                        + "Theme name uses Lumo and can not be used in combination with custom theme class.",
                exception.getMessage());
    }

    @Test
    public void noDefaultThemeAvailable_throwsException()
            throws ClassNotFoundException {
        Mockito.when(classFinder.getSubTypesOf(AppShellConfigurator.class))
                .thenReturn(Collections.singleton(MyAppThemeShell.class));
        Mockito.when(classFinder.loadClass(FrontendDependencies.LUMO))
                .thenThrow(ClassNotFoundException.class);

        IllegalStateException exception = Assert.assertThrows(
                IllegalStateException.class,
                () -> new FrontendDependencies(classFinder, false, null, true));

        Assert.assertEquals("Thrown exception didn't contain correct message",
                "Lumo dependency needs to be available on the classpath when using a theme name.",
                exception.getMessage());
    }

    @Test
    public void appThemeDefined_getsLumoAsTheme() {
        Mockito.when(classFinder.getSubTypesOf(AppShellConfigurator.class))
                .thenReturn(Collections.singleton(MyAppThemeShell.class));

        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false, null, true);

        Assert.assertEquals("Faulty default theme received", FakeLumo.class,
                dependencies.getThemeDefinition().getTheme());

    }

    @Test
    public void onlyThemeVariantDefined_getsLumoAsTheme_preserveVariant() {
        Mockito.when(classFinder.getSubTypesOf(AppShellConfigurator.class))
                .thenReturn(Collections.singleton(ThemeVariantOnly.class));

        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false, null, true);

        Assert.assertEquals("Faulty default theme received", FakeLumo.class,
                dependencies.getThemeDefinition().getTheme());
        Assert.assertEquals("Faulty variant received", "dark",
                dependencies.getThemeDefinition().getVariant());

    }

    @Test
    public void hasErrorParameterComponent_entryPointIsCollected() {
        Mockito.when(classFinder.getSubTypesOf(HasErrorParameter.class))
                .thenReturn(Collections.singleton(ErrorComponent.class));
        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false, null, true);
        DepsTests.assertImportsExcludingUI(dependencies.getModules(),
                "./src/bar.js");
        DepsTests.assertImports(dependencies.getScripts(), "./src/baz.js");
    }

    @Test
    public void componentInsideUiInitListener_entryPointsAreCollected() {
        Mockito.when(classFinder.getSubTypesOf(UIInitListener.class))
                .thenReturn(Collections.singleton(MyUIInitListener.class));
        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false, null, true);

        DepsTests.assertImportsExcludingUI(dependencies.getModules(), "baz.js");
        DepsTests.assertImports(dependencies.getScripts(), "foobar.js");

    }

    @Test
    public void componentInsideUiInitListenerInsideServiceInitListener_entryPointsAreCollected() {
        Mockito.when(classFinder.getSubTypesOf(VaadinServiceInitListener.class))
                .thenReturn(Collections.singleton(MyServiceListener.class));
        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false, null, true);
        DepsTests.assertImportsExcludingUI(dependencies.getModules(), "baz.js");
        DepsTests.assertImports(dependencies.getScripts(), "foobar.js");
    }

    @Test
    public void jsScriptOrderIsPreserved() {
        Mockito.when(classFinder.getAnnotatedClasses(Route.class))
                .thenReturn(Collections.singleton(JsOrderComponent.class));
        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false, null, true);

        DepsTests.assertImports(dependencies.getScripts(), "a.js", "b.js",
                "c.js");
    }

    @Test
    public void jsModuleOrderIsPreserved() {
        Mockito.when(classFinder.getAnnotatedClasses(Route.class)).thenReturn(
                Collections.singleton(JsModuleOrderComponent.class));
        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false, null, true);

        DepsTests.assertImportsExcludingUI(dependencies.getModules(), "c.js",
                "b.js", "a.js");
    }

    // flow #6524
    @Test
    public void extractsAndScansClassesFromMethodReferences() {
        Mockito.when(classFinder.getAnnotatedClasses(Route.class)).thenReturn(
                Collections.singleton(RouteComponentWithMethodReference.class));

        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false, null, true);

        DepsTests.assertImportsExcludingUI(dependencies.getModules(), "foo.js",
                "baz.js", "bar.js");
    }

    @Test
    public void defaultThemeIsLoadedForExporters() throws Exception {
        FakeLumo.class.getDeclaredConstructor().newInstance();
        Mockito.when(classFinder.getSubTypesOf(WebComponentExporter.class))
                .thenReturn(Stream.of(MyExporter.class)
                        .collect(Collectors.toSet()));

        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, true, null, true);

        Assert.assertNotNull(dependencies.getTheme());
        Assert.assertNotNull(dependencies.getThemeDefinition());
    }

    @Test // #9861
    public void collectEntryPoints_uiIsAlwaysCollected() {
        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false, null, true);

        Optional<EntryPointData> uiEndpointData = dependencies.getEntryPoints()
                .stream().filter(entryPoint -> entryPoint.getName()
                        .equals(UI.class.getName()))
                .findAny();
        Assert.assertTrue("UI should be visited", uiEndpointData.isPresent());
    }

    @Test // #9861
    public void classInMultipleEntryPoints_collectEntryPointsNotOverrideInitial() {
        // Reference found through first entry point
        Mockito.when(classFinder.getAnnotatedClasses(Route.class))
                .thenReturn(Collections.singleton(TestRoute.class));
        // Reference found through second entry point, should not clear
        Mockito.when(classFinder.getSubTypesOf(HasErrorParameter.class))
                .thenReturn(Collections.singleton(TestRoute.class));

        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false, null, true);
        DepsTests.assertImports(dependencies.getModules(), "reference.js",
                "@vaadin/common-frontend/ConnectionIndicator.js");
    }

    @Test // 20074
    public void layoutClasses_collectedAsEntrypoint() {
        Mockito.when(classFinder.getAnnotatedClasses(Layout.class))
                .thenReturn(Collections.singleton(MainLayout.class));

        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false, null, true);

        Optional<EntryPointData> layoutEndpointData = dependencies
                .getEntryPoints().stream().filter(entryPoint -> entryPoint
                        .getName().equals(MainLayout.class.getName()))
                .findAny();
        Assert.assertTrue("MainLayout should be visited",
                layoutEndpointData.isPresent());
        DepsTests.assertImports(dependencies.getModules(), "reference.js",
                "@vaadin/common-frontend/ConnectionIndicator.js");
    }

    @Test // #9861
    public void visitedExporter_previousEntryPointsNotOverridden()
            throws InstantiationException, IllegalAccessException,
            NoSuchMethodException, InvocationTargetException {

        FakeLumo.class.getDeclaredConstructor().newInstance();
        // Reference found through first entry point
        Mockito.when(classFinder.getAnnotatedClasses(Route.class))
                .thenReturn(Collections.singleton(ReferenceExporter.class));
        // Re-visit through exporter.
        Mockito.when(classFinder.getSubTypesOf(WebComponentExporter.class))
                .thenReturn(Stream.of(ReferenceExporter.class)
                        .collect(Collectors.toSet()));

        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, true, null, true);

        DepsTests.assertImports(dependencies.getModules(), "reference.js",
                "@vaadin/common-frontend/ConnectionIndicator.js");
    }

    @Test // #9861
    public void shouldVisit_shouldNotMatchOnPartOfPackage() {

        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, true, null, true);

        Assert.assertTrue(
                "second package should match fully not as starts with 'spring != springseason'",
                dependencies.shouldVisit("org.springseason.samples"));
        Assert.assertTrue(
                "second package should match fully not as starts with 'spring != springseason'",
                dependencies.shouldVisit("org.springseason"));
        Assert.assertFalse("should not visit with only 2 packages 'org.spring'",
                dependencies.shouldVisit("org.spring"));

        Assert.assertTrue(
                "second package should match fully not as starts with 'sun != sunny'",
                dependencies.shouldVisit("com.sunny.app"));
        Assert.assertTrue(
                "second package should match fully not as starts with 'sun != sunny'",
                dependencies.shouldVisit("com.sunny"));
        Assert.assertFalse("should not visit with only 2 packages 'com.sun'",
                dependencies.shouldVisit("com.sun"));
    }

    @Test
    public void classScanningForChildAndParentEntryPoint_ordered_childrenSeeClassesFromParent() {
        LinkedHashSet<Class<?>> hierarchy = Stream
                .of(ParentRoute.class, AnnotatedChildRoute.class,
                        ChildRoute.class, GrandChildRoute.class)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Mockito.when(classFinder.getAnnotatedClasses(Route.class))
                .thenReturn(hierarchy);

        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false, null, true);

        hierarchy.forEach(entryPointClass -> verifyEntryPointData(dependencies,
                entryPointClass));
    }

    @Test
    public void classScanningForChildAndParentEntryPoint_shuffled_childrenSeeClassesFromParent() {
        LinkedHashSet<Class<?>> hierarchy = Stream
                .of(GrandChildRoute.class, AnnotatedChildRoute.class,
                        ParentRoute.class, ChildRoute.class)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Mockito.when(classFinder.getAnnotatedClasses(Route.class))
                .thenReturn(hierarchy);

        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false, null, true);

        hierarchy.forEach(entryPointClass -> verifyEntryPointData(dependencies,
                entryPointClass));
    }

    @Test
    public void classScanningForChildAndParentEntryPoint_reversed_childrenSeeClassesFromParent() {
        LinkedHashSet<Class<?>> hierarchy = Stream
                .of(GrandChildRoute.class, ChildRoute.class,
                        AnnotatedChildRoute.class, ParentRoute.class)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Mockito.when(classFinder.getAnnotatedClasses(Route.class))
                .thenReturn(hierarchy);

        FrontendDependencies dependencies = new FrontendDependencies(
                classFinder, false, null, true);

        hierarchy.forEach(entryPointClass -> verifyEntryPointData(dependencies,
                entryPointClass));
    }

    private static EntryPointData getEntryPointByClass(
            FrontendDependencies dependencies, Class<?> entryPointClass) {
        Optional<EntryPointData> childEntryPoint = dependencies.getEntryPoints()
                .stream().filter(entryPoint -> entryPoint.getName()
                        .equals(entryPointClass.getName()))
                .findAny();
        Assert.assertTrue(childEntryPoint.isPresent());
        return childEntryPoint.get();
    }

    private static void verifyEntryPointData(FrontendDependencies dependencies,
            Class<?> entryPointClass) {
        EntryPointData entryPointData = getEntryPointByClass(dependencies,
                entryPointClass);

        Assert.assertNotNull(entryPointData.reachableClasses);
        Assert.assertFalse(entryPointData.reachableClasses.isEmpty());

        // Child entrypoint should see classes reachable from parent entrypoint,
        // not only the parent class
        Assert.assertTrue(entryPointData.reachableClasses.size() > 1);

        Assert.assertNotNull(entryPointData.getModules());
        Assert.assertEquals(1, entryPointData.getModules().size());
        Assert.assertEquals("reference.js",
                entryPointData.getModules().iterator().next());
    }

    public static class MyComponent extends Component {
    }

    public static class MyExporter extends WebComponentExporter<MyComponent> {
        public MyExporter() {
            super("tag-tag");
        }

        @Override
        protected void configureInstance(WebComponent<MyComponent> webComponent,
                MyComponent component) {
        }
    }

    public static class FakeLumo implements AbstractTheme {
        public FakeLumo() {
        }

        @Override
        public String getBaseUrl() {
            return null;
        }

        @Override
        public String getThemeUrl() {
            return null;
        }
    }

    @Theme(themeClass = FakeLumo.class)
    public static class MyAppShell implements AppShellConfigurator {
    }

    @Theme("my-theme")
    public static class MyAppThemeShell implements AppShellConfigurator {
    }

    @Theme(value = "my-theme", themeClass = FakeLumo.class)
    public static class FaultyThemeAnnotation implements AppShellConfigurator {
    }

    @Theme(variant = "dark")
    public static class ThemeVariantOnly implements AppShellConfigurator {
    }

    @JsModule("reference.js")
    @Tag("div")
    public static class Referenced extends Component {
    }

    @Route("reference")
    public static class ReferenceExporter
            extends WebComponentExporter<MyComponent> {
        public ReferenceExporter() {
            super("tag-tag");
        }

        @Override
        protected void configureInstance(WebComponent<MyComponent> webComponent,
                MyComponent component) {
            Referenced ref = new Referenced();
        }
    }

    @Route("reference")
    @Tag("div")
    public static class TestRoute extends Component
            implements HasErrorParameter<NotFoundException> {
        Referenced ref;

        public TestRoute() {
            ref = new Referenced();
        }

        @Override
        public int setErrorParameter(BeforeEnterEvent event,
                ErrorParameter<NotFoundException> parameter) {
            return 0;
        }
    }

    @Route("parent")
    public static class ParentRoute extends Component implements HasComponents {
        private Referenced myComponent = new Referenced();

        public ParentRoute() {
            add(myComponent);
        }
    }

    @Route("child")
    public static class AnnotatedChildRoute extends ParentRoute {
    }

    public static class ChildRoute extends ParentRoute {
    }

    public static class GrandChildRoute extends ChildRoute {
    }

    @Tag("div")
    @Layout
    @JsModule("reference.js")
    public static class MainLayout extends Component implements RouterLayout {
    }
}
